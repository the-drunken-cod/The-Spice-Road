package com.drunkencod.spice_road.config;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;

import com.drunkencod.spice_road.Constants;
import com.drunkencod.spice_road.compat.CompatRecipeMod;
import com.drunkencod.spice_road.compat.viewer.ViewerCategory;
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
        if (configClass == ClientConfigData.class) {
            return ConfigFile.CLIENT;
        }
        return configClass == CommonConfigData.class ? ConfigFile.COMMON : ConfigFile.SERVER;
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

    private static CommonConfigData common() {
        return AutoConfig.getConfigHolder(SpiceRoadConfigData.class).getConfig().common;
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
    public double getSpiceRegionSpicelessChance() {
        return value(ConfigSchema.REGION_SPICELESS_CHANCE, server().region.spicelessChance);
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
    public int getHarvestLuckMaxLevel() {
        return value(ConfigSchema.HARVEST_LUCK_MAX_LEVEL, server().harvestLuck.maxLevel);
    }

    @Override
    public double getHarvestLuckYieldBonus() {
        return value(ConfigSchema.HARVEST_LUCK_YIELD_BONUS, server().harvestLuck.yieldBonus);
    }

    @Override
    public double getHarvestLuckSeedChanceBonus() {
        return value(ConfigSchema.HARVEST_LUCK_SEED_CHANCE_BONUS, server().harvestLuck.seedChanceBonus);
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
    public int getSeasoningMaxEffects() {
        return value(ConfigSchema.SEASONING_MAX_EFFECTS, server().seasoning.maxEffects);
    }

    @Override
    public double getSeasoningEffectDurationMultiplier() {
        return value(ConfigSchema.SEASONING_EFFECT_DURATION_MULTIPLIER, server().seasoning.effectDurationMultiplier);
    }

    @Override
    public long getSeasoningBoardSalt() {
        return value(ConfigSchema.SEASONING_BOARD_SALT, server().seasoning.boardSalt);
    }

    @Override
    public double getSeasoningStepCost() {
        return value(ConfigSchema.SEASONING_STEP_COST, server().seasoning.stepCost);
    }

    @Override
    public double getSeasoningLockInCost(int ring) {
        ServerConfigData.Seasoning.LockInCost cost = server().seasoning.lockInCost;
        return switch (ring) {
            case 2 -> value(ConfigSchema.SEASONING_LOCK_IN_COST_RING_2, cost.ring2);
            case 3 -> value(ConfigSchema.SEASONING_LOCK_IN_COST_RING_3, cost.ring3);
            default -> value(ConfigSchema.SEASONING_LOCK_IN_COST_RING_4, cost.ring4);
        };
    }

    @Override
    public int getSeasoningMaxSpices() {
        return value(ConfigSchema.SEASONING_MAX_SPICES, server().seasoning.maxSpices);
    }

    @Override
    public int getSeasoningMaxSpicesPerKind() {
        return value(ConfigSchema.SEASONING_MAX_SPICES_PER_KIND, server().seasoning.maxSpicesPerKind);
    }

    @Override
    public int getSeasoningStorageRadius() {
        return value(ConfigSchema.SEASONING_STORAGE_RADIUS, server().seasoning.storageRadius);
    }

    @Override
    public int getSeasoningDiscoveryLimit() {
        return value(ConfigSchema.SEASONING_DISCOVERY_LIMIT, server().seasoning.discoveryLimit);
    }

    @Override
    public boolean getSeasoningBypassSaturationCap() {
        return value(ConfigSchema.SEASONING_BYPASS_SATURATION_CAP, server().seasoning.bypassSaturationCap);
    }

    @Override
    public double getSeasoningSaturationOvercap() {
        return value(ConfigSchema.SEASONING_SATURATION_OVERCAP, server().seasoning.saturationOvercap);
    }

    @Override
    public double getSeasoningParticipationMinSaturation() {
        return value(ConfigSchema.SEASONING_PARTICIPATION_MIN_SATURATION,
                server().seasoning.participation.minSaturation);
    }

    @Override
    public double getSeasoningParticipationMaxSaturation() {
        return value(ConfigSchema.SEASONING_PARTICIPATION_MAX_SATURATION,
                server().seasoning.participation.maxSaturation);
    }

    @Override
    public double getSeasoningParticipationFullDose() {
        return value(ConfigSchema.SEASONING_PARTICIPATION_FULL_DOSE, server().seasoning.participation.fullDose);
    }

    @Override
    public double getSeasoningDiversityMinSaturation() {
        return value(ConfigSchema.SEASONING_DIVERSITY_MIN_SATURATION, server().seasoning.diversity.minSaturation);
    }

    @Override
    public double getSeasoningDiversityMaxSaturation() {
        return value(ConfigSchema.SEASONING_DIVERSITY_MAX_SATURATION, server().seasoning.diversity.maxSaturation);
    }

    @Override
    public int getSeasoningDiversityFullDiversity() {
        return value(ConfigSchema.SEASONING_DIVERSITY_FULL_DIVERSITY, server().seasoning.diversity.fullDiversity);
    }

    @Override
    public boolean isDarkMode() {
        return client().gui.darkMode;
    }

    @Override
    public int getGrinderInputBufferMs() {
        return client().grinder.inputBufferMs;
    }

    @Override
    public boolean isGrinderPadOutlineColored() {
        return client().grinder.coloredPadOutlines;
    }

    @Override
    public int getGrinderPadYellowSteps() {
        return client().grinder.padYellowSteps;
    }

    @Override
    public int getGrinderPadRedSteps() {
        return client().grinder.padRedSteps;
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
    public boolean isTooltipAnimated() {
        return client().tooltip.animateAxes;
    }

    @Override
    public int getTooltipAnimationDurationMs() {
        return client().tooltip.animationDurationMs;
    }

    @Override
    public boolean isSneakRequiredToPlaceFlavoredFood() {
        return value(ConfigSchema.COMPAT_SNEAK_TO_PLACE_FLAVORED_FOOD, server().compat.sneakToPlaceFlavoredFood);
    }

    @Override
    public boolean isCompatRecipeEnabled(CompatRecipeMod mod) {
        CommonConfigData.CompatRecipes recipes = common().compatRecipes;
        return switch (mod) {
            case BOTANY_POTS -> recipes.botanypots;
            case IMMERSIVE_ENGINEERING -> recipes.immersiveengineering;
        };
    }

    @Override
    public boolean isRecipeViewerCategoryEnabled(ViewerCategory category) {
        ClientConfigData.RecipeViewer viewer = client().recipeViewer;
        return switch (category) {
            case SPICE_PROFILE -> viewer.spiceProfile;
            case SPICE_ORIGIN -> viewer.spiceOrigin;
            case DRYING -> viewer.drying;
        };
    }

    @Override
    public int getStatsFoundScanIntervalTicks() {
        return value(ConfigSchema.STATS_FOUND_SCAN_INTERVAL_TICKS, server().stats.foundScanIntervalTicks);
    }

    @Override
    public int getSpiceMixCapacity() {
        return value(ConfigSchema.SPICE_MIX_CAPACITY, server().spiceMix.capacity);
    }

    @Override
    public double getDryingRackSpeedMultiplier() {
        return value(ConfigSchema.DRYING_RACK_SPEED_MULTIPLIER, server().dryingRack.speedMultiplier);
    }

    @Override
    public double getDryingRackHeatedSpeedMultiplier() {
        return value(ConfigSchema.DRYING_RACK_HEATED_SPEED_MULTIPLIER, server().dryingRack.heatedSpeedMultiplier);
    }

    @Override
    public boolean areDryingRackSoundsEnabled() {
        return value(ConfigSchema.DRYING_RACK_SOUNDS, server().dryingRack.sounds);
    }

    @Override
    public boolean areDryingRackParticlesEnabled() {
        return client().dryingRackEffects.particles;
    }

    @Override
    public boolean areSpiceRackItemsRendered() {
        return client().spiceRack.renderItems;
    }

    @Override
    public int getSpiceRackItemRenderDistance() {
        return client().spiceRack.itemRenderDistance;
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
        @ConfigEntry.Category("common")
        @ConfigEntry.Gui.TransitiveObject
        public CommonConfigData common = new CommonConfigData();
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
        public HarvestLuck harvestLuck = new HarvestLuck();
        @ConfigEntry.Gui.CollapsibleObject
        public Flavor flavor = new Flavor();
        @ConfigEntry.Gui.CollapsibleObject
        public Seasoning seasoning = new Seasoning();
        @ConfigEntry.Gui.CollapsibleObject
        public SpiceMix spiceMix = new SpiceMix();
        @ConfigEntry.Gui.CollapsibleObject
        public DryingRack dryingRack = new DryingRack();
        @ConfigEntry.Gui.CollapsibleObject
        public Stats stats = new Stats();
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
            @ConfigEntry.Gui.Tooltip
            public double spicelessChance = ConfigSchema.REGION_SPICELESS_CHANCE.getDefault();
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

        /** How the Luck of a harvesting player scales Spice drops. */
        public static class HarvestLuck {
            @ConfigEntry.Gui.Tooltip
            public int maxLevel = ConfigSchema.HARVEST_LUCK_MAX_LEVEL.getDefault();
            @ConfigEntry.Gui.Tooltip
            public double yieldBonus = ConfigSchema.HARVEST_LUCK_YIELD_BONUS.getDefault();
            @ConfigEntry.Gui.Tooltip
            public double seedChanceBonus = ConfigSchema.HARVEST_LUCK_SEED_CHANCE_BONUS.getDefault();
        }

        /** How stored Spice Profiles turn into the flavor a player gets. */
        public static class Flavor {
            @ConfigEntry.Gui.Tooltip
            public double softCap = ConfigSchema.FLAVOR_SOFT_CAP.getDefault();
            @ConfigEntry.Gui.Tooltip
            public double minimumAxisValue = ConfigSchema.FLAVOR_MINIMUM_AXIS_VALUE.getDefault();
        }

        /** What eating seasoned food does. */
        public static class Seasoning {
            @ConfigEntry.Gui.Tooltip
            public int maxEffects = ConfigSchema.SEASONING_MAX_EFFECTS.getDefault();
            @ConfigEntry.Gui.Tooltip
            public double effectDurationMultiplier = ConfigSchema.SEASONING_EFFECT_DURATION_MULTIPLIER.getDefault();
            @ConfigEntry.Gui.Tooltip
            @ConfigEntry.Gui.RequiresRestart
            public long boardSalt = ConfigSchema.SEASONING_BOARD_SALT.getDefault();
            @ConfigEntry.Gui.Tooltip
            public double stepCost = ConfigSchema.SEASONING_STEP_COST.getDefault();
            @ConfigEntry.Gui.Tooltip
            public int maxSpices = ConfigSchema.SEASONING_MAX_SPICES.getDefault();
            @ConfigEntry.Gui.Tooltip
            public int maxSpicesPerKind = ConfigSchema.SEASONING_MAX_SPICES_PER_KIND.getDefault();
            @ConfigEntry.Gui.Tooltip
            public int storageRadius = ConfigSchema.SEASONING_STORAGE_RADIUS.getDefault();
            @ConfigEntry.Gui.Tooltip
            public int discoveryLimit = ConfigSchema.SEASONING_DISCOVERY_LIMIT.getDefault();
            @ConfigEntry.Gui.Tooltip
            public boolean bypassSaturationCap = ConfigSchema.SEASONING_BYPASS_SATURATION_CAP.getDefault();
            @ConfigEntry.Gui.Tooltip
            public double saturationOvercap = ConfigSchema.SEASONING_SATURATION_OVERCAP.getDefault();
            @ConfigEntry.Gui.CollapsibleObject
            public LockInCost lockInCost = new LockInCost();
            @ConfigEntry.Gui.CollapsibleObject
            public Participation participation = new Participation();
            @ConfigEntry.Gui.CollapsibleObject
            public Diversity diversity = new Diversity();

            /** Lock-in prices on the Seasoning Board, per ring. */
            public static class LockInCost {
                @ConfigEntry.Gui.Tooltip
                public double ring2 = ConfigSchema.SEASONING_LOCK_IN_COST_RING_2.getDefault();
                @ConfigEntry.Gui.Tooltip
                public double ring3 = ConfigSchema.SEASONING_LOCK_IN_COST_RING_3.getDefault();
                @ConfigEntry.Gui.Tooltip
                public double ring4 = ConfigSchema.SEASONING_LOCK_IN_COST_RING_4.getDefault();
            }

            /** Bonus saturation for seasoned food that ended up without effects. */
            public static class Participation {
                @ConfigEntry.Gui.Tooltip
                public double minSaturation = ConfigSchema.SEASONING_PARTICIPATION_MIN_SATURATION.getDefault();
                @ConfigEntry.Gui.Tooltip
                public double maxSaturation = ConfigSchema.SEASONING_PARTICIPATION_MAX_SATURATION.getDefault();
                @ConfigEntry.Gui.Tooltip
                public double fullDose = ConfigSchema.SEASONING_PARTICIPATION_FULL_DOSE.getDefault();
            }

            /** Bonus saturation for seasoned food in relation to its spice variety. */
            public static class Diversity {
                @ConfigEntry.Gui.Tooltip
                public double minSaturation = ConfigSchema.SEASONING_DIVERSITY_MIN_SATURATION.getDefault();
                @ConfigEntry.Gui.Tooltip
                public double maxSaturation = ConfigSchema.SEASONING_DIVERSITY_MAX_SATURATION.getDefault();
                @ConfigEntry.Gui.Tooltip
                public int fullDiversity = ConfigSchema.SEASONING_DIVERSITY_FULL_DIVERSITY.getDefault();
            }
        }

        /** Spice Mixes and their crafting. */
        public static class SpiceMix {
            @ConfigEntry.Gui.Tooltip
            public int capacity = ConfigSchema.SPICE_MIX_CAPACITY.getDefault();
        }

        /** The Drying Rack's speed and sounds. */
        public static class DryingRack {
            @ConfigEntry.Gui.Tooltip
            public double speedMultiplier = ConfigSchema.DRYING_RACK_SPEED_MULTIPLIER.getDefault();
            @ConfigEntry.Gui.Tooltip
            public double heatedSpeedMultiplier = ConfigSchema.DRYING_RACK_HEATED_SPEED_MULTIPLIER.getDefault();
            @ConfigEntry.Gui.Tooltip
            public boolean sounds = ConfigSchema.DRYING_RACK_SOUNDS.getDefault();
        }

        /** Player statistics the mod tracks. */
        public static class Stats {
            @ConfigEntry.Gui.Tooltip
            public int foundScanIntervalTicks = ConfigSchema.STATS_FOUND_SCAN_INTERVAL_TICKS.getDefault();
        }

        /** Behavior around other mods' placeable food items. */
        public static class Compat {
            @ConfigEntry.Gui.Tooltip
            public boolean sneakToPlaceFlavoredFood = ConfigSchema.COMPAT_SNEAK_TO_PLACE_FLAVORED_FOOD.getDefault();
        }
    }

    /**
     * Values read when datapacks load, mirroring {@code spice_road-common.toml}.
     */
    @Config(name = "common")
    public static class CommonConfigData implements ConfigData {

        @ConfigEntry.Gui.CollapsibleObject
        public CompatRecipes compatRecipes = new CompatRecipes();

        @Override
        public void validatePostLoad() {
            clampToSchema(this, ConfigFile.COMMON);
        }

        /** Which optional mods get recipes loaded, one field per mod ID. */
        public static class CompatRecipes {
            @ConfigEntry.Gui.Tooltip
            public boolean botanypots = ConfigSchema.COMPAT_RECIPES_ENABLED.get(CompatRecipeMod.BOTANY_POTS)
                    .getDefault();
            @ConfigEntry.Gui.Tooltip
            public boolean immersiveengineering = ConfigSchema.COMPAT_RECIPES_ENABLED
                    .get(CompatRecipeMod.IMMERSIVE_ENGINEERING).getDefault();
        }
    }

    /** Client-only display values, mirroring {@code spice_road-client.toml}. */
    @Config(name = Constants.MOD_ID + "_client")
    public static class ClientConfigData implements ConfigData {

        @ConfigEntry.Gui.CollapsibleObject
        public Gui gui = new Gui();
        @ConfigEntry.Gui.CollapsibleObject
        public TooltipDisplay tooltip = new TooltipDisplay();
        @ConfigEntry.Gui.CollapsibleObject
        public Grinder grinder = new Grinder();
        @ConfigEntry.Gui.CollapsibleObject
        public DryingRackEffects dryingRackEffects = new DryingRackEffects();
        @ConfigEntry.Gui.CollapsibleObject
        public SpiceRackDisplay spiceRack = new SpiceRackDisplay();
        @ConfigEntry.Gui.CollapsibleObject
        public RecipeViewer recipeViewer = new RecipeViewer();

        @Override
        public void validatePostLoad() {
            clampToSchema(this, ConfigFile.CLIENT);
        }

        /** Drying Rack effects. */
        public static class DryingRackEffects {
            @ConfigEntry.Gui.Tooltip
            public boolean particles = ConfigSchema.DRYING_RACK_PARTICLES.getDefault();
        }

        /** Spice Rack display. */
        public static class SpiceRackDisplay {
            @ConfigEntry.Gui.Tooltip
            public boolean renderItems = ConfigSchema.SPICE_RACK_RENDER_ITEMS.getDefault();
            @ConfigEntry.Gui.Tooltip
            public int itemRenderDistance = ConfigSchema.SPICE_RACK_ITEM_RENDER_DISTANCE.getDefault();
        }

        /** Which of the mod's categories Recipe Viewers show, one field per category. */
        public static class RecipeViewer {
            @ConfigEntry.Gui.Tooltip
            public boolean spiceProfile = ConfigSchema.RECIPE_VIEWER_CATEGORIES.get(ViewerCategory.SPICE_PROFILE)
                    .getDefault();
            @ConfigEntry.Gui.Tooltip
            public boolean spiceOrigin = ConfigSchema.RECIPE_VIEWER_CATEGORIES.get(ViewerCategory.SPICE_ORIGIN)
                    .getDefault();
            @ConfigEntry.Gui.Tooltip
            public boolean drying = ConfigSchema.RECIPE_VIEWER_CATEGORIES.get(ViewerCategory.DRYING).getDefault();
        }

        /** Look shared by all of the mod's GUIs. */
        public static class Gui {
            @ConfigEntry.Gui.Tooltip
            public boolean darkMode = ConfigSchema.DARK_MODE.getDefault();
        }

        /** Spice Grinder GUI behavior. */
        public static class Grinder {
            @ConfigEntry.Gui.Tooltip
            public boolean coloredPadOutlines = ConfigSchema.GRINDER_COLORED_PAD_OUTLINES.getDefault();
            @ConfigEntry.Gui.Tooltip
            public int inputBufferMs = ConfigSchema.GRINDER_INPUT_BUFFER_MS.getDefault();
            @ConfigEntry.Gui.Tooltip
            public int padYellowSteps = ConfigSchema.GRINDER_PAD_YELLOW_STEPS.getDefault();
            @ConfigEntry.Gui.Tooltip
            public int padRedSteps = ConfigSchema.GRINDER_PAD_RED_STEPS.getDefault();
        }

        /** What the mod shows on item tooltips. */
        public static class TooltipDisplay {
            @ConfigEntry.Gui.Tooltip
            public boolean showBothAxisLabels = ConfigSchema.TOOLTIP_SHOW_BOTH_AXIS_LABELS.getDefault();
            @ConfigEntry.Gui.Tooltip
            public boolean showAxisValues = ConfigSchema.TOOLTIP_SHOW_AXIS_VALUES.getDefault();
            @ConfigEntry.Gui.Tooltip
            public boolean alwaysShowShiftContent = ConfigSchema.TOOLTIP_ALWAYS_SHOW_SHIFT_CONTENT.getDefault();
            @ConfigEntry.Gui.Tooltip
            public boolean animateAxes = ConfigSchema.TOOLTIP_ANIMATE_AXES.getDefault();
            @ConfigEntry.Gui.Tooltip
            public int animationDurationMs = ConfigSchema.TOOLTIP_ANIMATION_DURATION_MS.getDefault();
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
