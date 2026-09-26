package com.drunkencod.spice_road.config;

import com.drunkencod.spice_road.Constants;
import com.drunkencod.spice_road.platform.Services;
import com.drunkencod.spice_road.spice.Tier;

import me.shedaniel.autoconfig.AutoConfig;
import me.shedaniel.autoconfig.ConfigData;
import me.shedaniel.autoconfig.annotation.Config;
import me.shedaniel.autoconfig.annotation.ConfigEntry;
import me.shedaniel.autoconfig.serializer.GsonConfigSerializer;

public class FabricConfigHelper implements IConfigHelper {

    // -------------------------------------------------------------------------
    // Registration - called during mod initialization
    // -------------------------------------------------------------------------

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
    public double getSpiceRegionClusteringStrength() {
        return AutoConfig.getConfigHolder(ServerConfigData.class).getConfig().spiceRegionClusteringStrength;
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

    // -------------------------------------------------------------------------
    // Config data classes
    // -------------------------------------------------------------------------

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
         * {@link Constants#DEFAULT_SPICE_PLANT_HARVEST_YIELD} to regenerate them.
         */
        @ConfigEntry.Gui.Tooltip
        @ConfigEntry.BoundedDiscrete(min = 1, max = 64)
        public double spicePlantHarvestYieldMultiplier = Constants.DEFAULT_SPICE_PLANT_HARVEST_YIELD_MULTIPLIER;
    }

    @Config(name = Constants.MOD_ID + "_server")
    public static class ServerConfigData implements ConfigData {
        @ConfigEntry.Gui.Tooltip
        public double spiceRegionCellScale = 1024.0;

        @ConfigEntry.Gui.Tooltip
        public double spiceRegionClusteringStrength = Services.PLATFORM.isDedicatedServer() ? 1.75 : 1.0;

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
    }

    @Config(name = Constants.MOD_ID + "_client")
    public static class ClientConfigData implements ConfigData {
    }
}
