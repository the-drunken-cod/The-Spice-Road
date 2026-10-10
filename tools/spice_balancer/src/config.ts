// #region sliders

/** Lowest value an axis slider can be dragged to. The number input may go beyond it. */
export const SLIDER_MIN = -2;
/** Highest value an axis slider can be dragged to. The number input may go beyond it. */
export const SLIDER_MAX = 2;
/** Step of the axis sliders and number inputs, also used to round scaled values. */
export const SLIDER_STEP = 0.05;
/** Decimals axis values are rounded to on export. */
export const EXPORT_DECIMALS = 2;

// #region potency

/** Target Potency (sum of absolute axis values) per Tier, used by "scale to target". */
export const TIER_TARGETS: Readonly<Record<string, number>> = {
  common: 2.0,
  uncommon: 2.6,
  rare: 3.5,
  epic: 4.8,
};

// #region compare

/** Most profiles that can be compared side by side. */
export const MAX_COMPARE = 4;

// #region display

/** Short labels of the seasons, in display order. */
export const SEASONS: ReadonlyArray<{ id: string; label: string }> = [
  { id: "spring", label: "SP" },
  { id: "summer", label: "SU" },
  { id: "autumn", label: "AU" },
  { id: "winter", label: "WI" },
];

/** Badge colors of the climates. */
export const CLIMATE_COLORS: Readonly<Record<string, string>> = {
  tropical: "#3fae5a",
  temperate: "#7aa83c",
  arid: "#d0a050",
  cold: "#6aa8e0",
};

/** Border color of profiles that don't belong to a known Tier. */
export const UNKNOWN_TIER_COLOR = "#777777";

// #region storage

/** Prefix of every localStorage key this tool writes. */
export const STORAGE_PREFIX = "spice_balancer.";
