import { strToU8, zipSync } from "fflate";

import { EXPORT_DECIMALS } from "./config";
import type { Profile, ProfileFile } from "./data";
import { roundToDecimals } from "./potency";
import type { Store } from "./store";

/**
 * Serializes a changed profile in its original file's shape: same fields, key
 * order, indentation and trailing newline. Axes the original lacked are only
 * written if they are non-zero, and unchanged values keep their original spelling.
 * @param file    The baseline file.
 * @param profile The values to write.
 * @param axes    Known axis IDs, in display order.
 * @return The new file text.
 */
export function serializeProfile(file: ProfileFile, profile: Profile, axes: readonly string[]): string {
  const order = [...Object.keys(file.profile), ...axes.filter(axis => !(axis in file.profile))];
  const written: Profile = {};
  for (const axis of order) {
    const value = roundToDecimals(profile[axis] ?? 0, EXPORT_DECIMALS);
    if (axis in file.profile || value !== 0)
      written[axis] = value;
  }

  // numbers are swapped for marker strings, so integers can keep a decimal like the hand-written files
  const markedProfile = Object.fromEntries(Object.entries(written).map(([axis, value]) => {
    const unchanged = file.profile[axis] === value && file.literals[axis];
    return [axis, NUMBER_MARKER + (unchanged || formatNumber(value))];
  }));
  const text = JSON.stringify({ ...file.json, profile: markedProfile }, null, file.indent)
    .replace(new RegExp(`"${NUMBER_MARKER}([^"]+)"`, "g"), "$1");
  return text + (file.trailingNewline ? "\n" : "");
}

/** Prefix of the placeholder strings standing in for profile numbers during serialization. */
const NUMBER_MARKER = "~spice_balancer_number~";

/**
 * @param value An axis value.
 * @return It as JSON, with non-zero integers written as e.g. {@code 1.0}.
 */
function formatNumber(value: number): string {
  return Number.isInteger(value) && value !== 0 ? value.toFixed(1) : String(value);
}

/**
 * Packs every changed profile into a ZIP, at its datapack-relative path, and
 * downloads it.
 * @param store The store holding the profiles.
 * @return How many files were exported.
 */
export function downloadChangedZip(store: Store): number {
  const entries: Record<string, Uint8Array> = {};
  for (const path of store.changedPaths()) {
    const file = store.file(path);
    if (file)
      entries[path] = strToU8(serializeProfile(file, store.get(path), store.axes));
  }

  const count = Object.keys(entries).length;
  if (count === 0)
    return 0;

  const blob = new Blob([zipSync(entries)], { type: "application/zip" });
  const url = URL.createObjectURL(blob);
  const link = document.createElement("a");
  link.href = url;
  link.download = `spice_profiles_${new Date().toISOString().slice(0, 19).replace(/[:T]/g, "-")}.zip`;
  link.click();
  setTimeout(() => URL.revokeObjectURL(url), 1000);
  return count;
}
