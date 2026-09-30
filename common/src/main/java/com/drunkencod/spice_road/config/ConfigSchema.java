package com.drunkencod.spice_road.config;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import com.drunkencod.spice_road.Constants;
import com.drunkencod.spice_road.platform.Services;
import com.drunkencod.spice_road.spice.Tier;

/**
 * Every config option the mod has, declared once. Both loaders build their own
 * config representation from this: NeoForge walks it into a
 * {@code ModConfigSpec}, Fabric initializes and clamps its backing fields
 * against it.
 * <p>
 * Adding an option means declaring it here and adding its two strings to
 * {@code lang/en_us.json} - the build fails if either is missing.
 */
public final class ConfigSchema {

    private static final List<ConfigOption<?>> OPTIONS = new ArrayList<>();

    // #region Sections

    /** Spice Region layout and identity. */
    public static final ConfigSection REGION = ConfigSection.root(ConfigFile.SERVER, "region");
    /** Finding Spice Regions, and the maps that point at them. */
    public static final ConfigSection SPICE_MAP = ConfigSection.root(ConfigFile.SERVER, "spiceMap");
    /** Cartographer Spice Map offers. */
    public static final ConfigSection SPICE_MAP_TRADES = SPICE_MAP.child("trades");
    /** Per-tier price scaling of Spice Map offers. */
    public static final ConfigSection SPICE_MAP_TRADE_PRICES = SPICE_MAP_TRADES.child("priceMultiplier");
    /** Where and whether players may plant Spices. */
    public static final ConfigSection CULTIVATION = ConfigSection.root(ConfigFile.SERVER, "cultivation");
    /** Per-tier growth pacing of planted Spices. */
    public static final ConfigSection CULTIVATION_GROWTH_SPEED = CULTIVATION.child("growthSpeed");
    /** Harvesting Spice Plants that grow on the ground. */
    public static final ConfigSection PLANT = ConfigSection.root(ConfigFile.SERVER, "plant");
    /** Harvesting Spice Trees. */
    public static final ConfigSection TREE = ConfigSection.root(ConfigFile.SERVER, "tree");
    /** Per-tier fruiting of Spice Tree leaves. */
    public static final ConfigSection TREE_FRUITING_LEAVES = TREE.child("fruitingLeaves");
    /** Harvesting Spice Vines. */
    public static final ConfigSection VINE = ConfigSection.root(ConfigFile.SERVER, "vine");
    /** Per-tier ripening of Spice Vine segments. */
    public static final ConfigSection VINE_RIPENING_SEGMENTS = VINE.child("ripeningSegments");
    /** How stored Spice Profiles turn into the flavor a player actually gets. */
    public static final ConfigSection FLAVOR = ConfigSection.root(ConfigFile.SERVER, "flavor");
    /** How cooking alters inherited flavor. */
    public static final ConfigSection COOKING = ConfigSection.root(ConfigFile.SERVER, "cooking");
    /** Client-side tooltip display. */
    public static final ConfigSection TOOLTIP = ConfigSection.root(ConfigFile.CLIENT, "tooltip");
    /** Behavior around other mods' placeable food items. */
    public static final ConfigSection COMPAT = ConfigSection.root(ConfigFile.SERVER, "compat");

    // #region Region

    /** Approximate edge length of a Spice Region cell, in blocks. */
    public static final ConfigOption<Double> REGION_CELL_SCALE = add(
            ConfigOption.ofDouble(REGION, "cellScale", 256.0, 64.0, 1_000_000.0));

    /** Salt mixed into the world seed when resolving Spice Regions. */
    public static final ConfigOption<Long> REGION_SALT = add(
            ConfigOption.ofLong(REGION, "salt", () -> new Random().nextLong(), Long.MIN_VALUE, Long.MAX_VALUE)
                    .restart(ConfigOption.Restart.WORLD));

    /** How strongly Region generation favors common Spices over rarer ones. */
    public static final ConfigOption<Double> REGION_CLUSTERING_STRENGTH = add(ConfigOption.ofDouble(REGION,
            "clusteringStrength", () -> Services.PLATFORM.isDedicatedServer() ? 1.75 : 1.0, 0.0, 10.0));

    // #region Spice Map

    /** Search radius, in cells, for {@code /locate} and Spice Map loot. */
    public static final ConfigOption<Integer> SPICE_MAP_SEARCH_RADIUS_CELLS = add(
            ConfigOption.ofInt(SPICE_MAP, "searchRadiusCells", 25, 1, 1000));

    /** Search radius, in cells, for cartographer Spice Map offers. */
    public static final ConfigOption<Integer> SPICE_MAP_VILLAGER_SEARCH_RADIUS_CELLS = add(
            ConfigOption.ofInt(SPICE_MAP, "villagerSearchRadiusCells", 12, 1, 1000));

    /** Whether Spice Maps can generate as chest loot. */
    public static final ConfigOption<Boolean> SPICE_MAP_LOOT_ENABLED = add(
            ConfigOption.ofBoolean(SPICE_MAP, "lootEnabled", true));

    /** Whether cartographers offer Spice Map trades. */
    public static final ConfigOption<Boolean> SPICE_MAP_TRADES_ENABLED = add(
            ConfigOption.ofBoolean(SPICE_MAP_TRADES, "enabled", true));

    /** Base emerald price of a Spice Map trade, before the tier multiplier. */
    public static final ConfigOption<Integer> SPICE_MAP_BASE_PRICE = add(
            ConfigOption.ofInt(SPICE_MAP_TRADES, "basePrice", 16, 1, 128));

    /** Spice Map price multiplier per {@link Tier}. */
    public static final Map<Tier, ConfigOption<Double>> SPICE_MAP_PRICE_MULTIPLIER = perTier(SPICE_MAP_TRADE_PRICES,
            1.0, 1.33, 1.67, 2.0, 1.0, 2.0);

    // #region Cultivation

    /** Whether planting a Spice requires a Region that supports it. */
    public static final ConfigOption<Boolean> CULTIVATION_REGION_PLANTING_RESTRICTED = add(
            ConfigOption.ofBoolean(CULTIVATION, "regionPlantingRestricted", true));

    /** Harvest difficulty at or below which a Spice may be planted anywhere. */
    public static final ConfigOption<Integer> CULTIVATION_HARDY_HARVEST_DIFFICULTY = add(
            ConfigOption.ofInt(CULTIVATION, "hardyHarvestDifficulty", 2, 1, 5));

    /** Whether Aquatic Spices only grow while waterlogged. */
    public static final ConfigOption<Boolean> CULTIVATION_AQUATIC_REQUIRES_WATER = add(
            ConfigOption.ofBoolean(CULTIVATION, "aquaticRequiresWater", true));

    /** Whether flowing water passes through Aquatic Spice plants instead of being blocked by them. */
    public static final ConfigOption<Boolean> CULTIVATION_AQUATIC_FLOW_THROUGH = add(
            ConfigOption.ofBoolean(CULTIVATION, "aquaticFlowThrough", true));

    /** Whether bees may pollinate Spice plants that aren't ripe yet. */
    public static final ConfigOption<Boolean> CULTIVATION_BEES_POLLINATE_UNRIPE = add(
            ConfigOption.ofBoolean(CULTIVATION, "beesPollinateUnripe", false));

    /** Growth-speed multiplier of planted Spices per {@link Tier}. */
    public static final Map<Tier, ConfigOption<Double>> CULTIVATION_GROWTH_SPEED_MULTIPLIER = perTier(
            CULTIVATION_GROWTH_SPEED, 1.0, 1.0, 1.0, 1.0, 0.1, 5.0);

    // #region Harvesting

    /** Harvest yield multiplier for FLOWER_PATCH and CROP Spice Plants. */
    public static final ConfigOption<Double> PLANT_HARVEST_YIELD_MULTIPLIER = add(ConfigOption.ofDouble(PLANT,
            "harvestYieldMultiplier", Constants.DEFAULT_SPICE_PLANT_HARVEST_YIELD_MULTIPLIER, 0.0, 64.0));

    /** Harvest yield multiplier for Spice Trees. */
    public static final ConfigOption<Double> TREE_HARVEST_YIELD_MULTIPLIER = add(ConfigOption.ofDouble(TREE,
            "harvestYieldMultiplier", Constants.DEFAULT_SPICE_TREE_HARVEST_YIELD_MULTIPLIER, 0.0, 64.0));

    /** Fraction of a fruiting Spice Tree's leaves that bear fruit, per {@link Tier}. */
    public static final Map<Tier, ConfigOption<Double>> TREE_FRUITING_LEAVES_CHANCE = perTier(TREE_FRUITING_LEAVES,
            0.3, 0.2, 0.15, 0.1, 0.0, 1.0);

    /** Fraction of a Spice Vine's segments that can ripen, per {@link Tier}. */
    public static final Map<Tier, ConfigOption<Double>> VINE_RIPENING_SEGMENTS_CHANCE = perTier(
            VINE_RIPENING_SEGMENTS, 0.3, 0.2, 0.15, 0.1, 0.0, 1.0);

    // #region Flavor

    /** Magnitude each flavor axis saturates towards when read. */
    public static final ConfigOption<Double> FLAVOR_SOFT_CAP = add(
            ConfigOption.ofDouble(FLAVOR, "softCap", 10.0, 0.1, 1000.0));

    /** Magnitude any non-zero flavor axis counts as at least. */
    public static final ConfigOption<Double> FLAVOR_MINIMUM_AXIS_VALUE = add(
            ConfigOption.ofDouble(FLAVOR, "minimumAxisValue", 0.05, 0.0, 1.0));

    /** Magnitude a raw flavor axis must reach to count as Sufficiently Seasoned. */
    public static final ConfigOption<Double> FLAVOR_SUFFICIENTLY_SEASONED = add(
            ConfigOption.ofDouble(FLAVOR, "sufficientlySeasoned", 7.0, 0.1, 1000.0));

    // #region Cooking

    /** Lower bound of the per-axis multipliers applied when cooking. */
    public static final ConfigOption<Double> COOKING_VARIANCE_MIN = add(
            ConfigOption.ofDouble(COOKING, "varianceMin", 0.85, 0.0, 10.0));

    /** Upper bound of the per-axis multipliers applied when cooking. */
    public static final ConfigOption<Double> COOKING_VARIANCE_MAX = add(
            ConfigOption.ofDouble(COOKING, "varianceMax", 1.15, 0.0, 10.0));

    // #region Tooltip

    /** Whether Flavor Axis tooltips show both pole labels of each axis. */
    public static final ConfigOption<Boolean> TOOLTIP_SHOW_BOTH_AXIS_LABELS = add(
            ConfigOption.ofBoolean(TOOLTIP, "showBothAxisLabels", false));

    /** Whether Flavor Axis tooltips show each axis' score. */
    public static final ConfigOption<Boolean> TOOLTIP_SHOW_AXIS_VALUES = add(
            ConfigOption.ofBoolean(TOOLTIP, "showAxisValues", false));

    /** Whether Shift-gated tooltip content is always shown. */
    public static final ConfigOption<Boolean> TOOLTIP_ALWAYS_SHOW_SHIFT_CONTENT = add(
            ConfigOption.ofBoolean(TOOLTIP, "alwaysShowShiftContent", false));

    // #region Compat

    /** Whether placing a Spice Profile override as a non-block-entity block requires sneaking. */
    public static final ConfigOption<Boolean> COMPAT_SNEAK_TO_PLACE_FLAVORED_FOOD = add(
            ConfigOption.ofBoolean(COMPAT, "sneakToPlaceFlavoredFood", true));

    private ConfigSchema() {
    }

    // #region Lookup

    /**
     * @param file The config file to list.
     * @return That file's options, in the order they should appear in it.
     */
    public static List<ConfigOption<?>> options(ConfigFile file) {
        return OPTIONS.stream().filter(option -> option.getSection().getFile() == file).toList();
    }

    /** @return Every option of every file, in declaration order. */
    public static List<ConfigOption<?>> options() {
        return Collections.unmodifiableList(OPTIONS);
    }

    /**
     * @param file The config file to list.
     * @return Every section of that file, including nested ones, in the order
     *         they are first used.
     */
    public static Set<ConfigSection> sections(ConfigFile file) {
        Set<ConfigSection> found = new LinkedHashSet<>();
        for (ConfigOption<?> option : options(file)) {
            addWithAncestors(found, option.getSection());
        }
        return found;
    }

    /**
     * @param file        The config file the path belongs to.
     * @param dottedPath  A dotted option path, e.g. {@code "cooking.varianceMin"}.
     * @return The matching option, or {@code null} if the file has none.
     */
    public static ConfigOption<?> option(ConfigFile file, String dottedPath) {
        return options(file).stream()
                .filter(option -> option.getDottedPath().equals(dottedPath))
                .findFirst()
                .orElse(null);
    }

    /**
     * @param file       The config file the path belongs to.
     * @param dottedPath A dotted section path, e.g. {@code "spiceMap.trades"}.
     * @return The matching section, or {@code null} if the file has none.
     */
    public static ConfigSection section(ConfigFile file, String dottedPath) {
        return sections(file).stream()
                .filter(section -> section.getDottedPath().equals(dottedPath))
                .findFirst()
                .orElse(null);
    }

    private static void addWithAncestors(Set<ConfigSection> found, ConfigSection section) {
        if (section.getParent() != null) {
            addWithAncestors(found, section.getParent());
        }
        found.add(section);
    }

    private static <T extends Comparable<T>> ConfigOption<T> add(ConfigOption<T> option) {
        OPTIONS.add(option);
        return option;
    }

    /**
     * Declares one option per {@link Tier} in the given section, keyed by the
     * tier's lowercase name.
     *
     * @param section  The section holding the four options.
     * @param common   The COMMON tier's default.
     * @param uncommon The UNCOMMON tier's default.
     * @param rare     The RARE tier's default.
     * @param epic     The EPIC tier's default.
     * @param min      The lowest accepted value, inclusive.
     * @param max      The highest accepted value, inclusive.
     * @return The declared options, by tier.
     */
    private static Map<Tier, ConfigOption<Double>> perTier(ConfigSection section, double common, double uncommon,
            double rare, double epic, double min, double max) {
        Map<Tier, ConfigOption<Double>> byTier = new EnumMap<>(Tier.class);
        double[] defaults = { common, uncommon, rare, epic };
        for (Tier tier : Tier.values()) {
            byTier.put(tier,
                    add(ConfigOption.ofDouble(section, tier.getSerializedName(), defaults[tier.ordinal()], min, max)));
        }
        return Collections.unmodifiableMap(byTier);
    }
}
