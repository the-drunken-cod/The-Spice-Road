import { UNKNOWN_TIER_COLOR } from "./config";
import type { AxisMeta, ItemMeta, ProfileFile, SpiceMeta } from "./data";

/** A profile file together with everything needed to display it. */
export interface Entry {
  path: string;
  name: string;
  /** The targeted items, joined for display. */
  target: string;
  /** Metadata of the single targeted item, or {@code null} for tags, lists and unknown items. */
  meta: ItemMeta | null;
  tier: string | null;
  tierColor: string;
  /** Sorts by Spice ID, keeping a Processed Spice right after its raw Spice. */
  sortKey: string;
}

/** A rarity group of entries. */
export interface Group {
  /** Tier ID, or {@code null} for entries without a Tier. */
  tier: string | null;
  label: string;
  color: string;
  entries: Entry[];
}

/** Looks up English display strings from the lang file. */
export class Labels {
  private readonly lang: Record<string, string>;

  /** @param lang The English lang file. */
  constructor(lang: Record<string, string>) {
    this.lang = lang;
  }

  /**
   * @param axis An axis ID.
   * @param positive Whether to name the positive pole.
   * @return The pole's display name.
   */
  pole(axis: string, positive: boolean): string {
    const fallback = axis.split("_")[positive ? 0 : 1] ?? axis;
    return this.lang[`spice_road.spice_axis.name.${axis}.${positive ? "positive" : "negative"}`] ?? titleCase(fallback);
  }

  /**
   * @param tier A Tier ID.
   * @return Its display name.
   */
  tier(tier: string): string {
    return this.lang[`spice_road.tooltip.spice_tier.${tier}`] ?? titleCase(tier);
  }

  /**
   * @param itemId An item ID like {@code spice_road:dried_clove}.
   * @param spice  The Spice ID the item belongs to, if known.
   * @return The item's display name.
   */
  item(itemId: string, spice?: string): string {
    const [namespace, path] = itemId.includes(":") ? itemId.split(":") : ["minecraft", itemId];
    return this.lang[`item.${namespace}.${path}`]
      ?? (spice ? this.lang[`spice.spice_road.${spice}`] : undefined)
      ?? titleCase(path!);
  }

  /**
   * @param id A processing method ID, e.g. {@code drying}.
   * @return The short badge text for items made with it.
   */
  processing(id: string): string {
    return id === "drying" ? "Dried" : titleCase(id);
  }
}

/**
 * @param meta The spice meta, if datagen has run.
 * @param files Profile files from the baseline.
 * @return The axes to show: the datagenned ones, or every axis found in the files with neutral colors.
 */
export function resolveAxes(meta: SpiceMeta | null, files: ProfileFile[]): AxisMeta[] {
  if (meta)
    return meta.axes;
  const ids = new Set(files.flatMap(file => Object.keys(file.profile)));
  return [...ids].map(id => ({ id, positive_color: "#bbbbbb", negative_color: "#888888" }));
}

/**
 * @param files  Profile files from the baseline.
 * @param meta   The spice meta, if datagen has run.
 * @param labels Display strings.
 * @return The files grouped by Tier, in Tier order, with entries without a Tier last.
 */
export function buildGroups(files: ProfileFile[], meta: SpiceMeta | null, labels: Labels): Group[] {
  const itemMeta = new Map(meta?.items.map(item => [item.item, item]));
  const tierColors = new Map(meta?.tiers.map(tier => [tier.id, tier.color]));

  const entries: Entry[] = files.map(file => {
    const single = file.items.length === 1 && !file.items[0]!.startsWith("#") ? file.items[0]! : null;
    const itemInfo = single ? itemMeta.get(single) ?? null : null;
    const fileName = file.path.slice(file.path.lastIndexOf("/") + 1, -".json".length);
    const name = single ? labels.item(single, itemInfo?.spice) : titleCase(fileName);
    return {
      path: file.path,
      name,
      target: file.items.join(", ") || "(no items)",
      meta: itemInfo,
      tier: itemInfo?.tier ?? null,
      tierColor: (itemInfo && tierColors.get(itemInfo.tier)) ?? UNKNOWN_TIER_COLOR,
      sortKey: `${itemInfo?.spice ?? fileName}\u0000${itemInfo?.processing ? 1 : 0}\u0000${name}`,
    };
  });

  const tierOrder = meta?.tiers.map(tier => tier.id) ?? [];
  const groups: Group[] = tierOrder.map(tier => ({
    tier,
    label: labels.tier(tier),
    color: tierColors.get(tier) ?? UNKNOWN_TIER_COLOR,
    entries: [],
  }));
  const other: Group = { tier: null, label: "Other", color: UNKNOWN_TIER_COLOR, entries: [] };

  for (const entry of entries)
    (groups.find(group => group.tier === entry.tier) ?? other).entries.push(entry);
  for (const group of [...groups, other])
    group.entries.sort((a, b) => a.sortKey.localeCompare(b.sortKey));

  return [...groups, other].filter(group => group.entries.length > 0);
}

/**
 * @param value A snake_case ID.
 * @return It in Title Case, e.g. {@code star_anise} to {@code Star Anise}.
 */
export function titleCase(value: string): string {
  return value.split("_").map(word => word.charAt(0).toUpperCase() + word.slice(1)).join(" ");
}
