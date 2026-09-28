package com.drunkencod.spice_road.config;

import java.util.Random;

import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.ModContainer;
import net.neoforged.neoforge.common.ModConfigSpec;
import org.apache.commons.lang3.tuple.Pair;

import com.drunkencod.spice_road.Constants;
import com.drunkencod.spice_road.platform.Services;
import com.drunkencod.spice_road.spice.Tier;

/**
 * NeoForge implementation of {@link IConfigHelper}, backed by
 * {@link ModConfigSpec}s for the common, server, and client configs.
 */
public class NeoForgeConfigHelper implements IConfigHelper {

    // -------------------------------------------------------------------------
    // Common (startup) config
    // -------------------------------------------------------------------------

    /** Common config values, loaded on both physical sides. */
    public static final CommonConfig COMMON;
    private static final ModConfigSpec COMMON_SPEC;

    static {
        Pair<CommonConfig, ModConfigSpec> specPair = new ModConfigSpec.Builder()
                .configure(CommonConfig::new);
        COMMON = specPair.getLeft();
        COMMON_SPEC = specPair.getRight();
    }

    // -------------------------------------------------------------------------
    // Server config
    // -------------------------------------------------------------------------

    /** Gameplay config values, synced from the logical server. */
    public static final ServerConfig SERVER;
    private static final ModConfigSpec SERVER_SPEC;

    static {
        Pair<ServerConfig, ModConfigSpec> specPair = new ModConfigSpec.Builder()
                .configure(ServerConfig::new);
        SERVER = specPair.getLeft();
        SERVER_SPEC = specPair.getRight();
    }

    // -------------------------------------------------------------------------
    // Client config
    // -------------------------------------------------------------------------

    /** Client-only display config values. */
    public static final ClientConfig CLIENT;
    private static final ModConfigSpec CLIENT_SPEC;

    static {
        Pair<ClientConfig, ModConfigSpec> specPair = new ModConfigSpec.Builder()
                .configure(ClientConfig::new);
        CLIENT = specPair.getLeft();
        CLIENT_SPEC = specPair.getRight();
    }

    // -------------------------------------------------------------------------
    // Registration - called from SpiceRoadMod constructor
    // -------------------------------------------------------------------------

    /**
     * Must be called in the NeoForge mod constructor with the injected
     * {@link ModContainer} so that configs are registered before the world loads.
     */
    public void register(ModContainer modContainer) {
        modContainer.registerConfig(ModConfig.Type.COMMON, COMMON_SPEC);
        modContainer.registerConfig(ModConfig.Type.SERVER, SERVER_SPEC);
        modContainer.registerConfig(ModConfig.Type.CLIENT, CLIENT_SPEC);
    }

    // -------------------------------------------------------------------------
    // IConfigHelper implementation
    // -------------------------------------------------------------------------

    @Override
    public double getSpiceRegionCellScale() {
        return SERVER.spiceRegionCellScale.get();
    }

    @Override
    public long getSpiceRegionSalt() {
        return SERVER.spiceRegionSalt.get();
    }

    @Override
    public double getSpiceRegionClusteringStrength() {
        return SERVER.spiceRegionClusteringStrength.get();
    }

    @Override
    public int getSpiceMapSearchRadiusCells() {
        return SERVER.spiceMapSearchRadiusCells.get();
    }

    @Override
    public int getSpiceMapVillagerSearchRadiusCells() {
        return SERVER.spiceMapVillagerSearchRadiusCells.get();
    }

    @Override
    public boolean isSpiceMapTradesEnabled() {
        return SERVER.spiceMapTradesEnabled.get();
    }

    @Override
    public int getSpiceMapBasePrice() {
        return SERVER.spiceMapBasePrice.get();
    }

    @Override
    public double getSpiceMapPriceMultiplier(Tier tier) {
        return switch (tier) {
            case COMMON -> SERVER.spiceMapPriceMultiplierCommon.get();
            case UNCOMMON -> SERVER.spiceMapPriceMultiplierUncommon.get();
            case RARE -> SERVER.spiceMapPriceMultiplierRare.get();
            case EPIC -> SERVER.spiceMapPriceMultiplierEpic.get();
        };
    }

    @Override
    public boolean isSpiceMapLootEnabled() {
        return SERVER.spiceMapLootEnabled.get();
    }

    @Override
    public boolean isSpiceRegionPlantingRestricted() {
        return SERVER.spiceRegionPlantingRestricted.get();
    }

    @Override
    public int getSpiceHardyHarvestDifficulty() {
        return SERVER.spiceHardyHarvestDifficulty.get();
    }

    @Override
    public int getSpicePlantGrowthStages() {
        return COMMON.spicePlantGrowthStages.get();
    }

    @Override
    public double getSpicePlantHarvestYieldMultiplier() {
        return COMMON.spicePlantHarvestYieldMultiplier.get();
    }

    @Override
    public double getSpicePlantGrowthSpeedMultiplier(Tier tier) {
        return switch (tier) {
            case COMMON -> SERVER.spiceGrowthSpeedCommon.get();
            case UNCOMMON -> SERVER.spiceGrowthSpeedUncommon.get();
            case RARE -> SERVER.spiceGrowthSpeedRare.get();
            case EPIC -> SERVER.spiceGrowthSpeedEpic.get();
        };
    }

    @Override
    public double getSpiceTreeHarvestYieldMultiplier() {
        return SERVER.spiceTreeHarvestYieldMultiplier.get();
    }

    @Override
    public double getSpiceTreeFruitingLeavesChance(Tier tier) {
        return switch (tier) {
            case COMMON -> SERVER.spiceTreeFruitingLeavesCommon.get();
            case UNCOMMON -> SERVER.spiceTreeFruitingLeavesUncommon.get();
            case RARE -> SERVER.spiceTreeFruitingLeavesRare.get();
            case EPIC -> SERVER.spiceTreeFruitingLeavesEpic.get();
        };
    }

    @Override
    public double getCookingVarianceMin() {
        return SERVER.cookingVarianceMin.get();
    }

    @Override
    public double getCookingVarianceMax() {
        return SERVER.cookingVarianceMax.get();
    }

    @Override
    public double getFlavorSoftCap() {
        return COMMON.flavorSoftCap.get();
    }

    @Override
    public double getFlavorMinimumAxisValue() {
        return COMMON.flavorMinimumAxisValue.get();
    }

    @Override
    public boolean isTooltipBothAxisLabelsShown() {
        return CLIENT.tooltipShowBothAxisLabels.get();
    }

    @Override
    public boolean isTooltipAxisValueShown() {
        return CLIENT.tooltipShowAxisValues.get();
    }

    @Override
    public boolean isTooltipShiftBypassed() {
        return CLIENT.tooltipAlwaysShowShiftContent.get();
    }

    // -------------------------------------------------------------------------
    // Inner config classes
    // -------------------------------------------------------------------------

    /** Spec entries of the common config. */
    public static class CommonConfig {
        public final ModConfigSpec.IntValue spicePlantGrowthStages;
        public final ModConfigSpec.DoubleValue spicePlantHarvestYieldMultiplier;
        public final ModConfigSpec.DoubleValue flavorSoftCap;
        public final ModConfigSpec.DoubleValue flavorMinimumAxisValue;

        CommonConfig(ModConfigSpec.Builder builder) {
            spicePlantGrowthStages = builder
                    .comment("Growth stage count (highest age value, 1-7) shared by every "
                            + "FLOWER_PATCH/CROP Spice Plant block. Read once per block at registration "
                            + "time; requires a restart to take effect.")
                    .defineInRange("spicePlantGrowthStages",
                            Constants.DEFAULT_SPICE_PLANT_GROWTH_STAGES, 1, 7);
            spicePlantHarvestYieldMultiplier = builder
                    .comment("Harvest yield multiplier for all FLOWER_PATCH/CROP Spice Plants. "
                            + "Loot tables bake in the default value of this option at datagen "
                            + "time, not this live value, so re-run datagen after changing "
                            + "Constants.DEFAULT_SPICE_PLANT_HARVEST_YIELD_MULTIPLIER.")
                    .defineInRange("spicePlantHarvestYieldMultiplier",
                            Constants.DEFAULT_SPICE_PLANT_HARVEST_YIELD_MULTIPLIER, 0.0D,
                            Double.MAX_VALUE);
            flavorSoftCap = builder
                    .comment("Magnitude each flavor axis saturates towards when a profile is read for effects and "
                            + "tooltips, giving diminishing returns when stacking many spices. Stored values are "
                            + "never capped.")
                    .defineInRange("flavorSoftCap", 10.0, 0.1, 1000.0);
            flavorMinimumAxisValue = builder
                    .comment("Magnitude any non-zero flavor axis counts as at least when a profile is read for "
                            + "effects and tooltips.")
                    .defineInRange("flavorMinimumAxisValue", 0.05, 0.0, 1.0);
        }
    }

    /** Spec entries of the server config. */
    public static class ServerConfig {
        public final ModConfigSpec.DoubleValue spiceRegionCellScale;
        public final ModConfigSpec.LongValue spiceRegionSalt;
        public final ModConfigSpec.DoubleValue spiceRegionClusteringStrength;
        public final ModConfigSpec.IntValue spiceMapSearchRadiusCells;
        public final ModConfigSpec.IntValue spiceMapVillagerSearchRadiusCells;
        public final ModConfigSpec.BooleanValue spiceMapTradesEnabled;
        public final ModConfigSpec.IntValue spiceMapBasePrice;
        public final ModConfigSpec.DoubleValue spiceMapPriceMultiplierCommon;
        public final ModConfigSpec.DoubleValue spiceMapPriceMultiplierUncommon;
        public final ModConfigSpec.DoubleValue spiceMapPriceMultiplierRare;
        public final ModConfigSpec.DoubleValue spiceMapPriceMultiplierEpic;
        public final ModConfigSpec.BooleanValue spiceMapLootEnabled;
        public final ModConfigSpec.BooleanValue spiceRegionPlantingRestricted;
        public final ModConfigSpec.IntValue spiceHardyHarvestDifficulty;
        public final ModConfigSpec.DoubleValue spiceGrowthSpeedCommon;
        public final ModConfigSpec.DoubleValue spiceGrowthSpeedUncommon;
        public final ModConfigSpec.DoubleValue spiceGrowthSpeedRare;
        public final ModConfigSpec.DoubleValue spiceGrowthSpeedEpic;
        public final ModConfigSpec.DoubleValue spiceTreeHarvestYieldMultiplier;
        public final ModConfigSpec.DoubleValue spiceTreeFruitingLeavesCommon;
        public final ModConfigSpec.DoubleValue spiceTreeFruitingLeavesUncommon;
        public final ModConfigSpec.DoubleValue spiceTreeFruitingLeavesRare;
        public final ModConfigSpec.DoubleValue spiceTreeFruitingLeavesEpic;
        public final ModConfigSpec.DoubleValue cookingVarianceMin;
        public final ModConfigSpec.DoubleValue cookingVarianceMax;

        ServerConfig(ModConfigSpec.Builder builder) {
            spiceRegionCellScale = builder
                    .comment("Approximate edge length of a Spice Region cell, in blocks. Should only be increased "
                            + "on Multiplayer servers, to encourage lots of travel and specialization. ")
                    .defineInRange("spiceRegionCellScale", 256.0, 64.0, 1_000_000.0);
            spiceRegionSalt = builder
                    .comment("Salt mixed into the world seed when resolving Spice Regions. Change this to "
                            + "any random value to shuffle spice regions.",
                            "WARNING: This affects existing worlds! Changing this will regenerate all Spice Regions, "
                                    + "including in already generated chunks.")
                    .worldRestart()
                    .defineInRange("spiceRegionSalt", new Random().nextLong(), Long.MIN_VALUE,
                            Long.MAX_VALUE);
            spiceRegionClusteringStrength = builder
                    .comment("How strongly Spice Region generation favors common Spices over rarer ones")
                    .defineInRange("spiceRegionClusteringStrength",
                            Services.PLATFORM.isDedicatedServer() ? 1.75 : 1.0, 0.0, 10.0);
            spiceMapSearchRadiusCells = builder
                    .comment("Maximum distance, in Spice Region cells, searched for a Spice Region's heart by "
                            + "/locate spice, /locate spice_climate, and by Spice Map chest loot. Measured in "
                            + "cells, so changing the cell scale doesn't change how many regions are within reach.")
                    .defineInRange("spiceMapSearchRadiusCells", 25, 1, 1000);
            spiceMapVillagerSearchRadiusCells = builder
                    .comment("Maximum distance, in Spice Region cells, a cartographer searches for a Spice Region's "
                            + "heart when offering a Spice Map. Lower values keep trading halls from reaching every "
                            + "Spice.")
                    .defineInRange("spiceMapVillagerSearchRadiusCells", 12, 1, 1000);
            spiceMapTradesEnabled = builder
                    .comment("Whether cartographers offer Spice Map trades. Only affects newly generated offers.")
                    .define("spiceMapTradesEnabled", true);
            spiceMapBasePrice = builder
                    .comment("Base emerald price of a Spice Map trade, multiplied by the Spice's tier multiplier. "
                            + "The final price is capped at 128; above 64, the compass is replaced by a second "
                            + "stack of emeralds.")
                    .defineInRange("spiceMapBasePrice", 16, 1, 128);
            spiceMapPriceMultiplierCommon = builder
                    .comment("Spice Map price multiplier for COMMON-tier Spices.")
                    .defineInRange("spiceMapPriceMultiplierCommon", 1.0, 1.0, 2.0);
            spiceMapPriceMultiplierUncommon = builder
                    .comment("Spice Map price multiplier for UNCOMMON-tier Spices.")
                    .defineInRange("spiceMapPriceMultiplierUncommon", 1.33, 1.0, 2.0);
            spiceMapPriceMultiplierRare = builder
                    .comment("Spice Map price multiplier for RARE-tier Spices.")
                    .defineInRange("spiceMapPriceMultiplierRare", 1.67, 1.0, 2.0);
            spiceMapPriceMultiplierEpic = builder
                    .comment("Spice Map price multiplier for EPIC-tier Spices. Cartographers don't sell these by "
                            + "default, but datapacks and other mods may.")
                    .defineInRange("spiceMapPriceMultiplierEpic", 2.0, 1.0, 2.0);
            spiceMapLootEnabled = builder
                    .comment("Whether Spice Maps can generate as chest loot.")
                    .define("spiceMapLootEnabled", true);
            spiceRegionPlantingRestricted = builder
                    .comment("Whether planting a CROP Spice's seeds requires the Spice Region at that "
                            + "position to actually support that Spice. Spices at or below "
                            + "spiceHardyHarvestDifficulty are always exempt.")
                    .define("spiceRegionPlantingRestricted", true);
            spiceHardyHarvestDifficulty = builder
                    .comment("Harvest/cultivation difficulty (1-5) at or below which a Spice is 'hardy' and "
                            + "can be planted anywhere the ground allows, bypassing the Spice Region check")
                    .defineInRange("spiceHardyHarvestDifficulty", 2, 1, 5);
            spiceGrowthSpeedCommon = builder
                    .comment("Growth-speed multiplier for COMMON-tier Spice Plants, applied on top of vanilla's "
                            + "farmland/light-based growth odds. 1.0 matches vanilla speed, < 1.0 slows growth "
                            + "down, > 1.0 speeds it up.")
                    .defineInRange("spiceGrowthSpeedCommon", 1.0, 0.1, 5.0);
            spiceGrowthSpeedUncommon = builder
                    .comment("Growth-speed multiplier for UNCOMMON-tier Spice Plants. See spiceGrowthSpeedCommon.")
                    .defineInRange("spiceGrowthSpeedUncommon", 1.0, 0.1, 5.0);
            spiceGrowthSpeedRare = builder
                    .comment("Growth-speed multiplier for RARE-tier Spice Plants. See spiceGrowthSpeedCommon.")
                    .defineInRange("spiceGrowthSpeedRare", 1.0, 0.1, 5.0);
            spiceGrowthSpeedEpic = builder
                    .comment("Growth-speed multiplier for EPIC-tier Spice Plants. See spiceGrowthSpeedCommon.")
                    .defineInRange("spiceGrowthSpeedEpic", 1.0, 0.1, 5.0);
            spiceTreeHarvestYieldMultiplier = builder
                    .comment("Harvest yield multiplier for Spice Trees, applied when stripping bark or picking "
                            + "fruiting leaves. Fractional results are rounded up or down at random.")
                    .defineInRange("spiceTreeHarvestYieldMultiplier",
                            Constants.DEFAULT_SPICE_TREE_HARVEST_YIELD_MULTIPLIER, 0.0,
                            64.0);
            spiceTreeFruitingLeavesCommon = builder
                    .comment("Fraction (0.0-1.0) of air-exposed, naturally grown leaves of COMMON-tier fruiting "
                            + "Spice Trees that can bear fruit.")
                    .defineInRange("spiceTreeFruitingLeavesCommon", 0.3, 0.0, 1.0);
            spiceTreeFruitingLeavesUncommon = builder
                    .comment(
                            "Fruiting leaves fraction for UNCOMMON-tier Spice Trees. See spiceTreeFruitingLeavesCommon.")
                    .defineInRange("spiceTreeFruitingLeavesUncommon", 0.2, 0.0, 1.0);
            spiceTreeFruitingLeavesRare = builder
                    .comment("Fruiting leaves fraction for RARE-tier Spice Trees. See spiceTreeFruitingLeavesCommon.")
                    .defineInRange("spiceTreeFruitingLeavesRare", 0.15, 0.0, 1.0);
            spiceTreeFruitingLeavesEpic = builder
                    .comment("Fruiting leaves fraction for EPIC-tier Spice Trees. See spiceTreeFruitingLeavesCommon.")
                    .defineInRange("spiceTreeFruitingLeavesEpic", 0.1, 0.0, 1.0);
            cookingVarianceMin = builder
                    .comment("Lower bound of the per-flavor-axis multipliers applied to inherited flavor when "
                            + "cooking. Results are reproducible: the same ingredients always cook into the same "
                            + "flavor.")
                    .defineInRange("cookingVarianceMin", 0.85, 0.0, 10.0);
            cookingVarianceMax = builder
                    .comment("Upper bound of the per-flavor-axis multipliers applied to inherited flavor when "
                            + "cooking.")
                    .defineInRange("cookingVarianceMax", 1.15, 0.0, 10.0);
        }
    }

    /** Spec entries of the client config. */
    public static class ClientConfig {
        public final ModConfigSpec.BooleanValue tooltipShowBothAxisLabels;
        public final ModConfigSpec.BooleanValue tooltipShowAxisValues;
        public final ModConfigSpec.BooleanValue tooltipAlwaysShowShiftContent;

        ClientConfig(ModConfigSpec.Builder builder) {
            tooltipShowBothAxisLabels = builder
                    .comment("Whether Flavor Axis tooltips show both labels of each axis (e.g. [Spicy / Cooling]), "
                            + "emphasizing the one matching the value, instead of only the matching one.")
                    .define("tooltipShowBothAxisLabels", false);
            tooltipShowAxisValues = builder
                    .comment("Whether Flavor Axis tooltips show each axis' value, multiplied by 10, after its label "
                            + "(e.g. [Spicy: 5]).")
                    .define("tooltipShowAxisValues", false);
            tooltipAlwaysShowShiftContent = builder
                    .comment("Whether all of this mod's tooltip content that normally requires holding Shift is "
                            + "always shown instead.")
                    .define("tooltipAlwaysShowShiftContent", false);
        }
    }
}
