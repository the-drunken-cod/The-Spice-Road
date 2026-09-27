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
        }
    }

    /** Spec entries of the server config. */
    public static class ServerConfig {
        public final ModConfigSpec.DoubleValue spiceRegionCellScale;
        public final ModConfigSpec.LongValue spiceRegionSalt;
        public final ModConfigSpec.DoubleValue spiceRegionClusteringStrength;
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

        ServerConfig(ModConfigSpec.Builder builder) {
            spiceRegionCellScale = builder
                    .comment("Approximate edge length of a Spice Region cell, in blocks")
                    .defineInRange("spiceRegionCellScale", 1024.0, 64.0, 1_000_000.0);
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
