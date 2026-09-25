package com.drunkencod.spice_road.config;

import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.ModContainer;
import net.neoforged.neoforge.common.ModConfigSpec;
import org.apache.commons.lang3.tuple.Pair;

import com.drunkencod.spice_road.Constants;
import com.drunkencod.spice_road.platform.Services;
import com.drunkencod.spice_road.spice.Tier;

public class NeoForgeConfigHelper implements IConfigHelper {

    // -------------------------------------------------------------------------
    // Common (startup) config
    // -------------------------------------------------------------------------

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
    public int getSpicePlantHarvestYield() {
        return COMMON.spicePlantHarvestYield.get();
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

    // -------------------------------------------------------------------------
    // Inner config classes
    // -------------------------------------------------------------------------

    public static class CommonConfig {
        public final ModConfigSpec.IntValue spicePlantGrowthStages;
        public final ModConfigSpec.IntValue spicePlantHarvestYield;

        CommonConfig(ModConfigSpec.Builder builder) {
            spicePlantGrowthStages = builder
                    .comment("Growth stage count (highest age value, 1-7) shared by every "
                            + "FLOWER_PATCH/CROP Spice Plant block. Read once per block at registration "
                            + "time; requires a restart to take effect.")
                    .defineInRange("spicePlantGrowthStages", Constants.DEFAULT_SPICE_PLANT_GROWTH_STAGES, 1, 7);
            spicePlantHarvestYield = builder
                    .comment("Flat harvest yield (item count) for FLOWER_PATCH/CROP Spice Plants. "
                            + "Phase 1: no tier-based scaling yet. Loot tables bake in the default value "
                            + "of this option at datagen time, not this live value; re-run datagen after "
                            + "changing Constants.DEFAULT_SPICE_PLANT_HARVEST_YIELD to regenerate them.")
                    .defineInRange("spicePlantHarvestYield", Constants.DEFAULT_SPICE_PLANT_HARVEST_YIELD, 1, 64);
        }
    }

    public static class ServerConfig {
        public final ModConfigSpec.DoubleValue spiceRegionCellScale;
        public final ModConfigSpec.DoubleValue spiceRegionClusteringStrength;
        public final ModConfigSpec.BooleanValue spiceRegionPlantingRestricted;
        public final ModConfigSpec.IntValue spiceHardyHarvestDifficulty;
        public final ModConfigSpec.DoubleValue spiceGrowthSpeedCommon;
        public final ModConfigSpec.DoubleValue spiceGrowthSpeedUncommon;
        public final ModConfigSpec.DoubleValue spiceGrowthSpeedRare;
        public final ModConfigSpec.DoubleValue spiceGrowthSpeedEpic;

        ServerConfig(ModConfigSpec.Builder builder) {
            spiceRegionCellScale = builder
                    .comment("Approximate edge length of a Spice Region cell, in blocks")
                    .defineInRange("spiceRegionCellScale", 1024.0, 64.0, 1_000_000.0);
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
        }
    }

    public static class ClientConfig {

        ClientConfig(ModConfigSpec.Builder builder) {
        }
    }
}
