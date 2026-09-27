package com.drunkencod.spice_road.config;

import java.util.Random;

import com.drunkencod.spice_road.Constants;
import com.drunkencod.spice_road.platform.Services;
import com.drunkencod.spice_road.spice.Tier;

import me.shedaniel.autoconfig.AutoConfig;
import me.shedaniel.autoconfig.ConfigData;
import me.shedaniel.autoconfig.annotation.Config;
import me.shedaniel.autoconfig.annotation.ConfigEntry;
import me.shedaniel.autoconfig.serializer.GsonConfigSerializer;

/**
 * Fabric implementation of {@link IConfigHelper}, backed by Cloth Config's
 * AutoConfig with separate common, server, and client config files.
 */
public class FabricConfigHelper implements IConfigHelper {

    // -------------------------------------------------------------------------
    // Registration - called during mod initialization
    // -------------------------------------------------------------------------

    /**
     * Registers all config files with AutoConfig. Must be called during mod
     * initialization, before any config value is read.
     */
    public void register() {
        AutoConfig.register(CommonConfigData.class, GsonConfigSerializer::new);
        AutoConfig.register(ServerConfigData.class, GsonConfigSerializer::new);
        AutoConfig.register(ClientConfigData.class, GsonConfigSerializer::new);
    }

    // -------------------------------------------------------------------------
    // IConfigHelper implementation
    // -------------------------------------------------------------------------

    @Override
    public double getSpiceRegionCellScale() {
        return AutoConfig.getConfigHolder(ServerConfigData.class).getConfig().spiceRegionCellScale;
    }

    @Override
    public long getSpiceRegionSalt() {
        return AutoConfig.getConfigHolder(ServerConfigData.class).getConfig().spiceRegionSalt;
    }

    @Override
    public double getSpiceRegionClusteringStrength() {
        return AutoConfig.getConfigHolder(ServerConfigData.class).getConfig().spiceRegionClusteringStrength;
    }

    @Override
    public int getSpiceMapSearchRadiusCells() {
        return AutoConfig.getConfigHolder(ServerConfigData.class).getConfig().spiceMapSearchRadiusCells;
    }

    @Override
    public int getSpiceMapVillagerSearchRadiusCells() {
        return AutoConfig.getConfigHolder(ServerConfigData.class).getConfig().spiceMapVillagerSearchRadiusCells;
    }

    @Override
    public boolean isSpiceMapTradesEnabled() {
        return AutoConfig.getConfigHolder(ServerConfigData.class).getConfig().spiceMapTradesEnabled;
    }

    @Override
    public int getSpiceMapBasePrice() {
        return AutoConfig.getConfigHolder(ServerConfigData.class).getConfig().spiceMapBasePrice;
    }

    @Override
    public double getSpiceMapPriceMultiplier(Tier tier) {
        ServerConfigData config = AutoConfig.getConfigHolder(ServerConfigData.class).getConfig();
        double multiplier = switch (tier) {
            case COMMON -> config.spiceMapPriceMultiplierCommon;
            case UNCOMMON -> config.spiceMapPriceMultiplierUncommon;
            case RARE -> config.spiceMapPriceMultiplierRare;
            case EPIC -> config.spiceMapPriceMultiplierEpic;
        };
        return Math.clamp(multiplier, 1.0, 2.0);
    }

    @Override
    public boolean isSpiceMapLootEnabled() {
        return AutoConfig.getConfigHolder(ServerConfigData.class).getConfig().spiceMapLootEnabled;
    }

    @Override
    public boolean isSpiceRegionPlantingRestricted() {
        return AutoConfig.getConfigHolder(ServerConfigData.class).getConfig().spiceRegionPlantingRestricted;
    }

    @Override
    public int getSpiceHardyHarvestDifficulty() {
        return AutoConfig.getConfigHolder(ServerConfigData.class).getConfig().spiceHardyHarvestDifficulty;
    }

    @Override
    public int getSpicePlantGrowthStages() {
        return AutoConfig.getConfigHolder(CommonConfigData.class).getConfig().spicePlantGrowthStages;
    }

    @Override
    public double getSpicePlantHarvestYieldMultiplier() {
        return AutoConfig.getConfigHolder(CommonConfigData.class).getConfig().spicePlantHarvestYieldMultiplier;
    }

    @Override
    public double getSpicePlantGrowthSpeedMultiplier(Tier tier) {
        ServerConfigData config = AutoConfig.getConfigHolder(ServerConfigData.class).getConfig();
        return switch (tier) {
            case COMMON -> config.spiceGrowthSpeedCommon;
            case UNCOMMON -> config.spiceGrowthSpeedUncommon;
            case RARE -> config.spiceGrowthSpeedRare;
            case EPIC -> config.spiceGrowthSpeedEpic;
        };
    }

    @Override
    public double getSpiceTreeHarvestYieldMultiplier() {
        return AutoConfig.getConfigHolder(ServerConfigData.class).getConfig().spiceTreeHarvestYieldMultiplier;
    }

    @Override
    public double getSpiceTreeFruitingLeavesChance(Tier tier) {
        ServerConfigData config = AutoConfig.getConfigHolder(ServerConfigData.class).getConfig();
        return switch (tier) {
            case COMMON -> config.spiceTreeFruitingLeavesCommon;
            case UNCOMMON -> config.spiceTreeFruitingLeavesUncommon;
            case RARE -> config.spiceTreeFruitingLeavesRare;
            case EPIC -> config.spiceTreeFruitingLeavesEpic;
        };
    }

    @Override
    public double getCookingVarianceMin() {
        return AutoConfig.getConfigHolder(ServerConfigData.class).getConfig().cookingVarianceMin;
    }

    @Override
    public double getCookingVarianceMax() {
        return AutoConfig.getConfigHolder(ServerConfigData.class).getConfig().cookingVarianceMax;
    }

    @Override
    public double getFlavorSoftCap() {
        return AutoConfig.getConfigHolder(CommonConfigData.class).getConfig().flavorSoftCap;
    }

    @Override
    public double getFlavorMinimumAxisValue() {
        return AutoConfig.getConfigHolder(CommonConfigData.class).getConfig().flavorMinimumAxisValue;
    }

    @Override
    public boolean isTooltipBothAxisLabelsShown() {
        return AutoConfig.getConfigHolder(ClientConfigData.class).getConfig().tooltipShowBothAxisLabels;
    }

    @Override
    public boolean isTooltipAxisValueShown() {
        return AutoConfig.getConfigHolder(ClientConfigData.class).getConfig().tooltipShowAxisValues;
    }

    @Override
    public boolean isTooltipShiftBypassed() {
        return AutoConfig.getConfigHolder(ClientConfigData.class).getConfig().tooltipAlwaysShowShiftContent;
    }

    // -------------------------------------------------------------------------
    // Config data classes
    // -------------------------------------------------------------------------

    /** Common config, loaded on both physical sides. */
    @Config(name = Constants.MOD_ID + "_common")
    public static class CommonConfigData implements ConfigData {
        /**
         * Growth stage count (highest age value, 1-7) shared by every
         * FLOWER_PATCH/CROP Spice Plant block. Read once per block at
         * registration time; requires a restart to take effect.
         */
        @ConfigEntry.Gui.Tooltip
        @ConfigEntry.BoundedDiscrete(min = 1, max = 7)
        public int spicePlantGrowthStages = Constants.DEFAULT_SPICE_PLANT_GROWTH_STAGES;

        /**
         * Flat harvest yield (item count) for FLOWER_PATCH/CROP Spice
         * Plants. TODO: add tier-based scaling.
         * Loot tables bake in the default value of this option at datagen time, not
         * this live value; re-run datagen after changing
         * {@link Constants#DEFAULT_SPICE_PLANT_HARVEST_YIELD_MULTIPLIER} to regenerate
         * them.
         */
        @ConfigEntry.Gui.Tooltip
        @ConfigEntry.BoundedDiscrete(min = 1, max = 64)
        public double spicePlantHarvestYieldMultiplier = Constants.DEFAULT_SPICE_PLANT_HARVEST_YIELD_MULTIPLIER;

        /**
         * Magnitude each flavor axis saturates towards when a profile is
         * read for effects and tooltips, giving diminishing returns when
         * stacking many spices. Stored values are never capped.
         */
        @ConfigEntry.Gui.Tooltip
        public double flavorSoftCap = 10.0;

        /**
         * Magnitude any non-zero flavor axis counts as at least when a
         * profile is read for effects and tooltips.
         */
        @ConfigEntry.Gui.Tooltip
        public double flavorMinimumAxisValue = 0.05;
    }

    /** Gameplay config read by the logical server. */
    @Config(name = Constants.MOD_ID + "_server")
    public static class ServerConfigData implements ConfigData {
        @ConfigEntry.Gui.Tooltip
        public double spiceRegionCellScale = 256.0;

        /**
         * Salt mixed into the world seed when resolving Spice Regions. Defaults
         * to a random value when this config is first created. WARNING: affects
         * world generation - changing it reshuffles every Spice Region,
         * including in already generated chunks.
         */
        @ConfigEntry.Gui.Tooltip
        public long spiceRegionSalt = new Random().nextLong();

        @ConfigEntry.Gui.Tooltip
        public double spiceRegionClusteringStrength = Services.PLATFORM.isDedicatedServer() ? 1.75 : 1.0;

        /**
         * Maximum distance, in Spice Region cells, searched for a Spice
         * Region's heart by /locate spice and by Spice Map chest loot.
         */
        @ConfigEntry.Gui.Tooltip
        @ConfigEntry.BoundedDiscrete(min = 1, max = 1000)
        public int spiceMapSearchRadiusCells = 25;

        /**
         * Maximum distance, in Spice Region cells, a cartographer searches for
         * a Spice Region's heart when offering a Spice Map.
         */
        @ConfigEntry.Gui.Tooltip
        @ConfigEntry.BoundedDiscrete(min = 1, max = 1000)
        public int spiceMapVillagerSearchRadiusCells = 12;

        /** Whether cartographers offer Spice Map trades. */
        @ConfigEntry.Gui.Tooltip
        public boolean spiceMapTradesEnabled = true;

        /**
         * Base emerald price of a Spice Map trade, multiplied by the Spice's
         * tier multiplier. Capped at 128; above 64, the compass is replaced by
         * a second stack of emeralds.
         */
        @ConfigEntry.Gui.Tooltip
        @ConfigEntry.BoundedDiscrete(min = 1, max = 128)
        public int spiceMapBasePrice = 16;

        /** Spice Map price multiplier (1.0-2.0) per {@link Tier}. */
        @ConfigEntry.Gui.Tooltip
        public double spiceMapPriceMultiplierCommon = 1.0;

        @ConfigEntry.Gui.Tooltip
        public double spiceMapPriceMultiplierUncommon = 1.33;

        @ConfigEntry.Gui.Tooltip
        public double spiceMapPriceMultiplierRare = 1.67;

        @ConfigEntry.Gui.Tooltip
        public double spiceMapPriceMultiplierEpic = 2.0;

        /** Whether Spice Maps can generate as chest loot. */
        @ConfigEntry.Gui.Tooltip
        public boolean spiceMapLootEnabled = true;

        @ConfigEntry.Gui.Tooltip
        public boolean spiceRegionPlantingRestricted = true;

        @ConfigEntry.Gui.Tooltip
        @ConfigEntry.BoundedDiscrete(min = 1, max = 5)
        public int spiceHardyHarvestDifficulty = 2;

        /**
         * Growth-speed multiplier for Spice Plants, applied on top of vanilla's
         * farmland/light-based growth odds, per {@link Tier}. 1.0 matches vanilla
         * speed, less than 1.0 slows growth down, greater than 1.0 speeds it up.
         */
        @ConfigEntry.Gui.Tooltip
        public double spiceGrowthSpeedCommon = 1.0;

        @ConfigEntry.Gui.Tooltip
        public double spiceGrowthSpeedUncommon = 1.0;

        @ConfigEntry.Gui.Tooltip
        public double spiceGrowthSpeedRare = 1.0;

        @ConfigEntry.Gui.Tooltip
        public double spiceGrowthSpeedEpic = 1.0;

        /**
         * Harvest yield multiplier for Spice Trees, applied when stripping bark
         * or picking fruiting leaves.
         */
        @ConfigEntry.Gui.Tooltip
        public double spiceTreeHarvestYieldMultiplier = Constants.DEFAULT_SPICE_TREE_HARVEST_YIELD_MULTIPLIER;

        /**
         * Fraction (0.0-1.0) of air-exposed, naturally grown leaves of
         * fruiting Spice Trees that can bear fruit, per {@link Tier}.
         */
        @ConfigEntry.Gui.Tooltip
        public double spiceTreeFruitingLeavesCommon = 0.3;

        @ConfigEntry.Gui.Tooltip
        public double spiceTreeFruitingLeavesUncommon = 0.2;

        @ConfigEntry.Gui.Tooltip
        public double spiceTreeFruitingLeavesRare = 0.15;

        @ConfigEntry.Gui.Tooltip
        public double spiceTreeFruitingLeavesEpic = 0.1;

        /**
         * Lower bound of the per-flavor-axis multipliers applied to inherited
         * flavor when cooking. Results are reproducible: the same ingredients
         * always cook into the same flavor.
         */
        @ConfigEntry.Gui.Tooltip
        public double cookingVarianceMin = 0.85;

        /**
         * Upper bound of the per-flavor-axis multipliers applied to inherited
         * flavor when cooking.
         */
        @ConfigEntry.Gui.Tooltip
        public double cookingVarianceMax = 1.15;
    }

    /** Client-only display config. */
    @Config(name = Constants.MOD_ID + "_client")
    public static class ClientConfigData implements ConfigData {
        /**
         * Whether Flavor Axis tooltips show both labels of each axis (e.g.
         * [Spicy / Cooling]), emphasizing the one matching the value, instead
         * of only the matching one.
         */
        @ConfigEntry.Gui.Tooltip
        public boolean tooltipShowBothAxisLabels = false;

        /**
         * Whether Flavor Axis tooltips show each axis' value, multiplied by
         * 10, after its label (e.g. [Spicy: 5]).
         */
        @ConfigEntry.Gui.Tooltip
        public boolean tooltipShowAxisValues = false;

        /**
         * Whether all of this mod's tooltip content that normally requires
         * holding Shift is always shown instead. Can help with finding specific
         * items in JEI/EMI, since their search can then match this content.
         */
        @ConfigEntry.Gui.Tooltip
        public boolean tooltipAlwaysShowShiftContent = false;
    }
}
