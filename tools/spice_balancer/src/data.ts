// #region types

/** One Flavor Axis with its pole colors. */
export interface AxisMeta {
  id: string;
  positive_color: string;
  negative_color: string;
}

/** One Tier with its display color. */
export interface TierMeta {
  id: string;
  color: string;
}

/** Growth metadata of one Spice Item, inherited from its Spice for Processed Spices. */
export interface ItemMeta {
  item: string;
  spice: string;
  processing?: string;
  tier: string;
  harvest_difficulty: number;
  climate: string;
  seasons: string[];
  source_type: string;
  harvest_action: string;
  aquatic: boolean;
  hand_pick: boolean;
}

/** The datagenned spice meta document. */
export interface SpiceMeta {
  axes: AxisMeta[];
  tiers: TierMeta[];
  items: ItemMeta[];
}

/** Axis ID to value. */
export type Profile = Record<string, number>;

/** One parsed spice profile JSON. */
export interface ProfileFile {
  /** Datapack-relative path, e.g. {@code data/spice_road/spice_profile/cinnamon.json}. */
  path: string;
  /** The whole parsed document, kept so unknown fields survive the export. */
  json: Record<string, unknown>;
  /** Item IDs or tags the profile targets. */
  items: string[];
  profile: Profile;
  /** How each axis value was spelled in the file, e.g. {@code 1.0}, so unchanged values export as-is. */
  literals: Record<string, string>;
  /** The file's original text. */
  raw: string;
  /** Indentation the file was written with. */
  indent: string;
  trailingNewline: boolean;
}

// #region bundled data

const profileModules = import.meta.glob<string>(
  "../../../common/src/main/resources/data/*/spice_profile/**/*.json",
  { eager: true, query: "?raw", import: "default" },
);
const metaModules = import.meta.glob<SpiceMeta>("../../../dev/spice_meta.json", { eager: true, import: "default" });
const langModules = import.meta.glob<Record<string, string>>(
  "../../../common/src/main/resources/assets/spice_road/lang/en_us.json",
  { eager: true, import: "default" },
);

/** @return The datagenned spice meta, or {@code null} if datagen hasn't run yet. */
export function loadMeta(): SpiceMeta | null {
  return Object.values(metaModules)[0] ?? null;
}

/** @return The English lang file, or an empty object if it's missing. */
export function loadLang(): Record<string, string> {
  return Object.values(langModules)[0] ?? {};
}

/** @return The profile files from the repository's resources. */
export function loadRepoProfiles(): ProfileFile[] {
  const files: ProfileFile[] = [];
  for (const [key, raw] of Object.entries(profileModules)) {
    const path = key.slice(key.indexOf("/data/") + 1);
    const file = parseProfile(path, raw);
    if (file)
      files.push(file);
  }
  return files;
}

// #region parsing

/**
 * @param path Datapack-relative path of the file.
 * @param raw  The file's text.
 * @return The parsed file, or {@code null} if it isn't a spice profile.
 */
export function parseProfile(path: string, raw: string): ProfileFile | null {
  let json: unknown;
  try {
    json = JSON.parse(raw);
  }
  catch {
    return null;
  }
  if (!isObject(json) || !isObject(json.profile))
    return null;

  const profile: Profile = {};
  for (const [axis, value] of Object.entries(json.profile)) {
    if (typeof value === "number")
      profile[axis] = value;
  }

  const items = typeof json.items === "string"
    ? [json.items]
    : Array.isArray(json.items) ? json.items.filter((item): item is string => typeof item === "string") : [];

  const literals: Record<string, string> = {};
  for (const axis of Object.keys(profile)) {
    const literal = raw.match(new RegExp(`"${axis}"\\s*:\\s*(-?[0-9][0-9.eE+-]*)`))?.[1];
    if (literal)
      literals[axis] = literal;
  }

  return {
    path,
    json,
    items,
    profile,
    literals,
    raw,
    indent: raw.match(/^[ \t]+(?=")/m)?.[0] ?? "  ",
    trailingNewline: raw.endsWith("\n"),
  };
}

/**
 * Reads the spice profiles out of a folder picked through a directory input.
 * Paths are rebuilt from the {@code data/} folder if there is one, otherwise
 * the files are assumed to be in {@code data/spice_road/spice_profile/}.
 * @param fileList The picked files.
 * @return The parsed profiles and how many JSON files were skipped as invalid.
 */
export async function loadProfilesFromFolder(fileList: FileList): Promise<{ files: ProfileFile[]; skipped: number }> {
  const files: ProfileFile[] = [];
  let skipped = 0;
  for (const file of Array.from(fileList)) {
    const relative = file.webkitRelativePath || file.name;
    if (!relative.endsWith(".json"))
      continue;

    const parsed = parseProfile(toDatapackPath(relative), await file.text());
    if (parsed)
      files.push(parsed);
    else
      skipped++;
  }
  return { files, skipped };
}

/**
 * @param relative A path relative to the picked folder.
 * @return The matching datapack-relative path.
 */
function toDatapackPath(relative: string): string {
  const parts = relative.split("/");
  const dataIndex = parts.lastIndexOf("data");
  if (dataIndex >= 0 && parts[dataIndex + 2] === "spice_profile")
    return parts.slice(dataIndex).join("/");

  const profileIndex = parts.lastIndexOf("spice_profile");
  const rest = profileIndex >= 0 ? parts.slice(profileIndex + 1) : parts.slice(1);
  return ["data", "spice_road", "spice_profile", ...rest].join("/");
}

/**
 * @param value Any value.
 * @return Whether {@code value} is a plain JSON object.
 */
function isObject(value: unknown): value is Record<string, unknown> {
  return typeof value === "object" && value !== null && !Array.isArray(value);
}
