package com.drunkencod.spice_road.config;

import com.drunkencod.spice_road.Constants;
import com.drunkencod.spice_road.platform.Services;

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
    public int getSpicePlantHarvestYield() {
        return AutoConfig.getConfigHolder(CommonConfigData.class).getConfig().spicePlantHarvestYield;
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
        public int spicePlantHarvestYield = Constants.DEFAULT_SPICE_PLANT_HARVEST_YIELD;
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
    }

    @Config(name = Constants.MOD_ID + "_client")
    public static class ClientConfigData implements ConfigData {
    }
}
