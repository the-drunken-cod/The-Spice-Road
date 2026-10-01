package com.drunkencod.spice_road.config;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;

import com.drunkencod.spice_road.Constants;
import com.drunkencod.spice_road.spice.Tier;

import me.shedaniel.autoconfig.AutoConfig;
import me.shedaniel.autoconfig.ConfigData;
import me.shedaniel.autoconfig.annotation.Config;
import me.shedaniel.autoconfig.annotation.ConfigEntry;
import me.shedaniel.autoconfig.serializer.ConfigSerializer;
import me.shedaniel.autoconfig.serializer.PartitioningSerializer;

/**
 * Fabric implementation of {@link IConfigHelper}, backed by Cloth Config's
 * AutoConfig with one file per {@link ConfigFile}.
 * <p>
 * The config classes are storage only: every default comes from
 * {@link ConfigSchema}, and {@code validatePostLoad} clamps each field to the
 * schema's range, since AutoConfig enforces no bounds of its own. Comments are
 * written by {@link CommentedJsonConfigSerializer}.
 */
public class FabricConfigHelper implements IConfigHelper {

    // #region Registration

    /**
     * Registers the config with AutoConfig. Must be called during mod
     * initialization, before any config value is read.
     * <p>
     * The two files are registered as one partitioned config so that ModMenu -
     * which allows a mod only a single screen - can reach both of them, as one
     * category each. Each partition still gets its own file, under
     * {@code config/spice_road/}.
     */
    public void register() {
        ConfigSerializer.Factory<ConfigData> partitions = (definition,
                configClass) -> new CommentedJsonConfigSerializer<>(
                        definition, configClass, fileOf(configClass));
        AutoConfig.register(SpiceRoadConfigData.class, PartitioningSerializer.wrap(partitions));
    }

    /**
     * @param configClass One of the partition classes.
     * @return The schema file whose options back it.
     */
    private static ConfigFile fileOf(Class<?> configClass) {
        return configClass == ClientConfigData.class ? ConfigFile.CLIENT : ConfigFile.SERVER;
    }

    /**
     * @return The config screen factory ModMenu opens, covering both partitions.
     */
    public static Class<SpiceRoadConfigData> getConfigClass() {
        return SpiceRoadConfigData.class;
    }

    private static ServerConfigData server() {
        return AutoConfig.getConfigHolder(SpiceRoadConfigData.class).getConfig().server;
    }

    private static ClientConfigData client() {
        return AutoConfig.getConfigHolder(SpiceRoadConfigData.class).getConfig().client;
    }

    /**
     * Resolves a SERVER-file option, preferring a value received via
     * {@link ConfigSync} - see {@link ConfigSyncOverride} - over the local
     * config file.
     *
     * @param <T>    The option's value type.
     * @param option The option being read.
     * @param local  The value currently in the local config file.
     * @return The synced value if one is active, else {@code local}.
     */
    private static <T extends Comparable<T>> T value(ConfigOption<T> option, T local) {
        T synced = ConfigSyncOverride.get(option);
        return synced != null ? synced : local;
    }

    // #region IConfigHelper implementation

    @Override
    public double getSpiceRegionCellScale() {
        return value(ConfigSchema.REGION_CELL_SCALE, server().region.cellScale);
    }

    @Override
    public long getSpiceRegionSalt() {
        return value(ConfigSchema.REGION_SALT, server().region.salt);
    }

    @Override
    public double getSpiceRegionClusteringStrength() {
        return value(ConfigSchema.REGION_CLUSTERING_STRENGTH, server().region.clusteringStrength);
    }

    @Override
    public double getSpiceRegionTierWeight(Tier tier) {
        return value(ConfigSchema.REGION_TIER_WEIGHT.get(tier), server().region.tierWeights.of(tier));
    }

    @Override
    public int getSpiceMapSearchRadiusCells() {
        return value(ConfigSchema.SPICE_MAP_SEARCH_RADIUS_CELLS, server().spiceMap.searchRadiusCells);
    }

    @Override
    public int getSpiceMapVillagerSearchRadiusCells() {
        return value(ConfigSchema.SPICE_MAP_VILLAGER_SEARCH_RADIUS_CELLS, server().spiceMap.villagerSearchRadiusCells);
    }

    @Override
    public boolean isSpiceMapTradesEnabled() {
        return value(ConfigSchema.SPICE_MAP_TRADES_ENABLED, server().spiceMap.trades.enabled);
    }

    @Override
    public int getSpiceMapBasePrice() {
        return value(ConfigSchema.SPICE_MAP_BASE_PRICE, server().spiceMap.trades.basePrice);
    }

    @Override
    public double getSpiceMapPriceMultiplier(Tier tier) {
        return value(ConfigSchema.SPICE_MAP_PRICE_MULTIPLIER.get(tier),
                server().spiceMap.trades.priceMultiplier.of(tier));
    }

    @Override
    public boolean isSpiceMapLootEnabled() {
        return value(ConfigSchema.SPICE_MAP_LOOT_ENABLED, server().spiceMap.lootEnabled);
    }

    @Override
    public boolean isSpiceRegionPlantingRestricted() {
        return value(ConfigSchema.CULTIVATION_REGION_PLANTING_RESTRICTED,
                server().cultivation.regionPlantingRestricted);
    }

    @Override
    public int getSpiceHardyHarvestDifficulty() {
        return value(ConfigSchema.CULTIVATION_HARDY_HARVEST_DIFFICULTY, server().cultivation.hardyHarvestDifficulty);
    }

    @Override
    public boolean isAquaticSpiceWaterRequired() {
        return value(ConfigSchema.CULTIVATION_AQUATIC_REQUIRES_WATER, server().cultivation.aquaticRequiresWater);
    }

    @Override
    public boolean isAquaticSpiceFlowThroughEnabled() {
        return value(ConfigSchema.CULTIVATION_AQUATIC_FLOW_THROUGH, server().cultivation.aquaticFlowThrough);
    }

    @Override
    public boolean isUnripeSpicePollinationAllowed() {
        return value(ConfigSchema.CULTIVATION_BEES_POLLINATE_UNRIPE, server().cultivation.beesPollinateUnripe);
    }

    @Override
    public double getSpicePlantHarvestYieldMultiplier() {
        return value(ConfigSchema.PLANT_HARVEST_YIELD_MULTIPLIER, server().plant.harvestYieldMultiplier);
    }

    @Override
    public double getSpicePlantGrowthSpeedMultiplier(Tier tier) {
        return value(ConfigSchema.CULTIVATION_GROWTH_SPEED_MULTIPLIER.get(tier),
                server().cultivation.growthSpeed.of(tier));
    }

    @Override
    public double getSpiceTreeHarvestYieldMultiplier() {
        return value(ConfigSchema.TREE_HARVEST_YIELD_MULTIPLIER, server().tree.harvestYieldMultiplier);
    }

    @Override
    public double getSpiceTreeFruitingLeavesChance(Tier tier) {
        return value(ConfigSchema.TREE_FRUITING_LEAVES_CHANCE.get(tier), server().tree.fruitingLeaves.of(tier));
    }

    @Override
    public double getSpiceVineRipeningSegmentsChance(Tier tier) {
        return value(ConfigSchema.VINE_RIPENING_SEGMENTS_CHANCE.get(tier), server().vine.ripeningSegments.of(tier));
    }

    @Override
    public double getCookingVarianceMin() {
        return value(ConfigSchema.COOKING_VARIANCE_MIN, server().cooking.varianceMin);
    }

    @Override
    public double getCookingVarianceMax() {
        return value(ConfigSchema.COOKING_VARIANCE_MAX, server().cooking.varianceMax);
    }

    @Override
    public double getFlavorSoftCap() {
        return value(ConfigSchema.FLAVOR_SOFT_CAP, server().flavor.softCap);
    }

    @Override
    public double getFlavorMinimumAxisValue() {
        return value(ConfigSchema.FLAVOR_MINIMUM_AXIS_VALUE, server().flavor.minimumAxisValue);
    }

    @Override
    public double getSufficientlySeasoned() {
        return value(ConfigSchema.FLAVOR_SUFFICIENTLY_SEASONED, server().flavor.sufficientlySeasoned);
    }

    @Override
    public boolean isTooltipBothAxisLabelsShown() {
        return client().tooltip.showBothAxisLabels;
    }

    @Override
    public boolean isTooltipAxisValueShown() {
        return client().tooltip.showAxisValues;
    }

    @Override
    public boolean isTooltipShiftBypassed() {
        return client().tooltip.alwaysShowShiftContent;
    }

    @Override
    public boolean isSneakRequiredToPlaceFlavoredFood() {
        return value(ConfigSchema.COMPAT_SNEAK_TO_PLACE_FLAVORED_FOOD, server().compat.sneakToPlaceFlavoredFood);
    }

    // #region Clamping

    /**
     * Moves every field of a loaded config back inside the range its schema
     * entry declares. AutoConfig has no notion of bounds, so without this a
     * hand-edited file could feed nonsense (a negative flavor soft cap, say)
     * straight into gameplay code.
     *
     * @param root The freshly loaded config object.
     * @param file The schema file describing it.
     */
    private static void clampToSchema(Object root, ConfigFile file) {
        for (ConfigOption<?> entry : ConfigSchema.options(file)) {
            try {
                clampOne(root, entry);
            } catch (ReflectiveOperationException | RuntimeException e) {
                Constants.LOG.error("Could not range-check config option {}", entry.getDottedPath(), e);
            }
        }
    }

    private static <T extends Comparable<T>> void clampOne(Object root,
            ConfigOption<T> entry) throws ReflectiveOperationException {
        if (entry.getMin() == null || entry.getMax() == null) {
            return;
        }
        Object owner = ownerFor(root, entry);
        Field field = fieldOf(owner, entry.getKey());
        T current = entry.getType().cast(field.get(owner));
        T clamped = entry.clamp(current);
        if (!clamped.equals(current)) {
            Constants.LOG.warn("Config option {} was {}, clamped to {}", entry.getDottedPath(), current, clamped);
            field.set(owner, clamped);
        }
    }

    /**
     * @param root  The config object an option's section path is relative to.
     * @param entry The option to resolve the owning section object for.
     * @return The nested section object that directly declares {@code entry}'s
     *         field.
     */
    private static Object ownerFor(Object root, ConfigOption<?> entry) throws ReflectiveOperationException {
        Object owner = root;
        for (String name : entry.getSection().getPath()) {
            owner = fieldOf(owner, name).get(owner);
        }
        return owner;
    }

    private static Field fieldOf(Object owner, String name) throws NoSuchFieldException {
        Field field = owner.getClass().getDeclaredField(name);
        field.setAccessible(true);
        return field;
    }

    // #region Sync

    /**
     * @return Every SERVER-file option's current local value, keyed by its
     *         dotted path, for {@link ConfigSync} to send on join.
     */
    static Map<String, Object> currentServerValues() {
        Map<String, Object> values = new HashMap<>();
        for (ConfigOption<?> option : ConfigSchema.options(ConfigFile.SERVER)) {
            try {
                Object owner = ownerFor(server(), option);
                values.put(option.getDottedPath(), fieldOf(owner, option.getKey()).get(owner));
            } catch (ReflectiveOperationException e) {
                Constants.LOG.error("Could not read config option {} for sync", option.getDottedPath(), e);
            }
        }
        return values;
    }

    // #region Config data classes

    /**
     * The whole config, as the single unit ModMenu opens. Each partition below
     * is written to its own file; see {@link #register()}.
     */
    @Config(name = Constants.MOD_ID)
    public static class SpiceRoadConfigData extends PartitioningSerializer.GlobalData {
        @ConfigEntry.Category("server")
        @ConfigEntry.Gui.TransitiveObject
        public ServerConfigData server = new ServerConfigData();
        @ConfigEntry.Category("client")
        @ConfigEntry.Gui.TransitiveObject
        public ClientConfigData client = new ClientConfigData();
    }

    /** Gameplay values, mirroring {@code spice_road-server.toml} on NeoForge. */
    @Config(name = "server")
    public static class ServerConfigData implements ConfigData {

        @ConfigEntry.Gui.CollapsibleObject
        public Region region = new Region();
        @ConfigEntry.Gui.CollapsibleObject
        public SpiceMap spiceMap = new SpiceMap();
        @ConfigEntry.Gui.CollapsibleObject
        public Cultivation cultivation = new Cultivation();
        @ConfigEntry.Gui.CollapsibleObject
        public Plant plant = new Plant();
        @ConfigEntry.Gui.CollapsibleObject
        public Tree tree = new Tree();
        @ConfigEntry.Gui.CollapsibleObject
        public Vine vine = new Vine();
        @ConfigEntry.Gui.CollapsibleObject
        public Flavor flavor = new Flavor();
        @ConfigEntry.Gui.CollapsibleObject
        public Cooking cooking = new Cooking();
        @ConfigEntry.Gui.CollapsibleObject
        public Compat compat = new Compat();

        @Override
        public void validatePostLoad() {
            clampToSchema(this, ConfigFile.SERVER);
        }

        /** Spice Region layout and identity. */
        public static class Region {
            @ConfigEntry.Gui.Tooltip
            public double cellScale = ConfigSchema.REGION_CELL_SCALE.getDefault();
            @ConfigEntry.Gui.Tooltip
            @ConfigEntry.Gui.RequiresRestart
            public long salt = ConfigSchema.REGION_SALT.getDefault();
            @ConfigEntry.Gui.Tooltip
            public double clusteringStrength = ConfigSchema.REGION_CLUSTERING_STRENGTH.getDefault();
            @ConfigEntry.Gui.CollapsibleObject
            public PerTier tierWeights = PerTier.of(ConfigSchema.REGION_TIER_WEIGHT);
        }

        /** Finding Spice Regions, and the maps that point to them. */
        public static class SpiceMap {
            @ConfigEntry.Gui.Tooltip
            public int searchRadiusCells = ConfigSchema.SPICE_MAP_SEARCH_RADIUS_CELLS.getDefault();
            @ConfigEntry.Gui.Tooltip
            public int villagerSearchRadiusCells = ConfigSchema.SPICE_MAP_VILLAGER_SEARCH_RADIUS_CELLS.getDefault();
            @ConfigEntry.Gui.Tooltip
            public boolean lootEnabled = ConfigSchema.SPICE_MAP_LOOT_ENABLED.getDefault();
            @ConfigEntry.Gui.CollapsibleObject
            public Trades trades = new Trades();
        }

        /** Cartographer Spice Map offers. */
        public static class Trades {
            @ConfigEntry.Gui.Tooltip
            public boolean enabled = ConfigSchema.SPICE_MAP_TRADES_ENABLED.getDefault();
            @ConfigEntry.Gui.Tooltip
            public int basePrice = ConfigSchema.SPICE_MAP_BASE_PRICE.getDefault();
            @ConfigEntry.Gui.CollapsibleObject
            public PerTier priceMultiplier = PerTier.of(ConfigSchema.SPICE_MAP_PRICE_MULTIPLIER);
        }

        /** Where Spices may be planted, and how quickly they grow. */
        public static class Cultivation {
            @ConfigEntry.Gui.Tooltip
            public boolean regionPlantingRestricted = ConfigSchema.CULTIVATION_REGION_PLANTING_RESTRICTED.getDefault();
            @ConfigEntry.Gui.Tooltip
            public int hardyHarvestDifficulty = ConfigSchema.CULTIVATION_HARDY_HARVEST_DIFFICULTY.getDefault();
            @ConfigEntry.Gui.Tooltip
            public boolean aquaticRequiresWater = ConfigSchema.CULTIVATION_AQUATIC_REQUIRES_WATER.getDefault();
            @ConfigEntry.Gui.Tooltip
            public boolean aquaticFlowThrough = ConfigSchema.CULTIVATION_AQUATIC_FLOW_THROUGH.getDefault();
            @ConfigEntry.Gui.Tooltip
            public boolean beesPollinateUnripe = ConfigSchema.CULTIVATION_BEES_POLLINATE_UNRIPE.getDefault();
            @ConfigEntry.Gui.CollapsibleObject
            public PerTier growthSpeed = PerTier.of(ConfigSchema.CULTIVATION_GROWTH_SPEED_MULTIPLIER);
        }

        /** Harvesting Spices that grow on the ground. */
        public static class Plant {
            @ConfigEntry.Gui.Tooltip
            public double harvestYieldMultiplier = ConfigSchema.PLANT_HARVEST_YIELD_MULTIPLIER.getDefault();
        }

        /** Harvesting Spices that grow on trees. */
        public static class Tree {
            @ConfigEntry.Gui.Tooltip
            public double harvestYieldMultiplier = ConfigSchema.TREE_HARVEST_YIELD_MULTIPLIER.getDefault();
            @ConfigEntry.Gui.CollapsibleObject
            public PerTier fruitingLeaves = PerTier.of(ConfigSchema.TREE_FRUITING_LEAVES_CHANCE);
        }

        /** Harvesting Spice Vines. */
        public static class Vine {
            @ConfigEntry.Gui.CollapsibleObject
            public PerTier ripeningSegments = PerTier.of(ConfigSchema.VINE_RIPENING_SEGMENTS_CHANCE);
        }

        /** How stored Spice Profiles turn into the flavor a player gets. */
        public static class Flavor {
            @ConfigEntry.Gui.Tooltip
            public double softCap = ConfigSchema.FLAVOR_SOFT_CAP.getDefault();
            @ConfigEntry.Gui.Tooltip
            public double minimumAxisValue = ConfigSchema.FLAVOR_MINIMUM_AXIS_VALUE.getDefault();
            @ConfigEntry.Gui.Tooltip
            public double sufficientlySeasoned = ConfigSchema.FLAVOR_SUFFICIENTLY_SEASONED.getDefault();
        }

        /** How cooking alters inherited flavor. */
        public static class Cooking {
            @ConfigEntry.Gui.Tooltip
            public double varianceMin = ConfigSchema.COOKING_VARIANCE_MIN.getDefault();
            @ConfigEntry.Gui.Tooltip
            public double varianceMax = ConfigSchema.COOKING_VARIANCE_MAX.getDefault();
        }

        /** Behavior around other mods' placeable food items. */
        public static class Compat {
            @ConfigEntry.Gui.Tooltip
            public boolean sneakToPlaceFlavoredFood = ConfigSchema.COMPAT_SNEAK_TO_PLACE_FLAVORED_FOOD.getDefault();
        }
    }

    /** Client-only display values, mirroring {@code spice_road-client.toml}. */
    @Config(name = Constants.MOD_ID + "_client")
    public static class ClientConfigData implements ConfigData {

        @ConfigEntry.Gui.CollapsibleObject
        public TooltipDisplay tooltip = new TooltipDisplay();

        @Override
        public void validatePostLoad() {
            clampToSchema(this, ConfigFile.CLIENT);
        }

        /** What the mod shows on item tooltips. */
        public static class TooltipDisplay {
            @ConfigEntry.Gui.Tooltip
            public boolean showBothAxisLabels = ConfigSchema.TOOLTIP_SHOW_BOTH_AXIS_LABELS.getDefault();
            @ConfigEntry.Gui.Tooltip
            public boolean showAxisValues = ConfigSchema.TOOLTIP_SHOW_AXIS_VALUES.getDefault();
            @ConfigEntry.Gui.Tooltip
            public boolean alwaysShowShiftContent = ConfigSchema.TOOLTIP_ALWAYS_SHOW_SHIFT_CONTENT.getDefault();
        }
    }

    /**
     * The four per-{@link Tier} values a schema section declares, as a nested
     * object so the config file reads {@code common}/{@code uncommon}/… inside
     * its section rather than repeating the tier in every key.
     */
    public static class PerTier {
        @ConfigEntry.Gui.Tooltip
        public double common;
        @ConfigEntry.Gui.Tooltip
        public double uncommon;
        @ConfigEntry.Gui.Tooltip
        public double rare;
        @ConfigEntry.Gui.Tooltip
        public double epic;

        /**
         * @param entries The schema's per-tier options for one section.
         * @return A value object holding each tier's default.
         */
        static PerTier of(java.util.Map<Tier, ConfigOption<Double>> entries) {
            PerTier values = new PerTier();
            values.common = entries.get(Tier.COMMON).getDefault();
            values.uncommon = entries.get(Tier.UNCOMMON).getDefault();
            values.rare = entries.get(Tier.RARE).getDefault();
            values.epic = entries.get(Tier.EPIC).getDefault();
            return values;
        }

        /**
         * @param tier The tier to read.
         * @return That tier's configured value.
         */
        public double of(Tier tier) {
            return switch (tier) {
                case COMMON -> common;
                case UNCOMMON -> uncommon;
                case RARE -> rare;
                case EPIC -> epic;
            };
        }
    }
}
