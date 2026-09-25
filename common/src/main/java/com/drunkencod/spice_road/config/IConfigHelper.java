package com.drunkencod.spice_road.config;

/**
 * Cross-loader config service interface.
 * <p>
 * Add config entries here as interface methods, then implement them in
 * {@code NeoForgeConfigHelper} (using {@code ModConfigSpec}) and
 * {@code FabricConfigHelper} (using Cloth Config / AutoConfig).
 *
 * <p>
 * Config is loaded via
 * {@link com.drunkencod.spice_road.platform.Services#CONFIG}.
 */
public interface IConfigHelper {

    /**
     * The (approximate) edge length of a Spice Region cell, in blocks. See
     * {@code com.drunkencod.spice_road.spice.region.SpiceRegionResolver#resolveCell}.
     *
     * @return The configured cell scale, in blocks.
     */
    double getSpiceRegionCellScale();

    /**
     * How strongly Spice Region generation favors common Spices over rarer
     * ones. Defaults differ between singleplayer (integrated) and dedicated
     * servers - see {@code IPlatformHelper#isDedicatedServer()} - since the
     * clustering is meant to encourage travel/trade, which matters more on
     * shared servers. See
     * {@code com.drunkencod.spice_road.spice.region.SpiceRegionResolver#resolveSpice}.
     *
     * @return The configured clustering strength exponent.
     */
    double getSpiceRegionClusteringStrength();

    /**
     * Whether Spice Region support gates player planting of
     * {@code CROP} Spice Plants (see
     * {@code com.drunkencod.spice_road.block.SpiceCropBlock#canSurvive}).
     * When {@code false}, a Spice's seeds can be planted anywhere the ground
     * itself allows (farmland), regardless of Spice Region/Climate. Spices at
     * or below {@link #getSpiceHardyHarvestDifficulty()} are always exempt
     * from this gate.
     *
     * @return Whether the Spice Region planting restriction is active.
     */
    boolean isSpiceRegionPlantingRestricted();

    /**
     * The harvest/cultivation difficulty (see {@code Spice#getHarvestDifficulty()})
     * at or below which a Spice is considered "hardy" and can be planted
     * anywhere the ground allows, bypassing the Spice Region check described
     * at {@link #isSpiceRegionPlantingRestricted()}.
     *
     * @return The configured hardy harvest-difficulty threshold, 1-5.
     */
    int getSpiceHardyHarvestDifficulty();

    /**
     * Configured growth stage count (highest age value, 1-7) shared by every
     * {@code FLOWER_PATCH}/{@code CROP} Spice Plant block.
     * <p>
     * <b>Not currently applied by the block itself.</b> Vanilla
     * {@code CropBlock#isRandomlyTicking()} calls {@code getMaxAge()} during
     * block-state-freeze, which happens immediately after
     * {@code RegisterEvent} and always before {@code ModConfigEvent.Loading}
     * in every environment. There is no safe point at which a {@code Block} can
     * read live config for this value, so {@code SpicePlantBlock#getMaxAge()}
     * returns
     * {@link com.drunkencod.spice_road.Constants#DEFAULT_SPICE_PLANT_GROWTH_STAGES}
     * directly instead of calling this method. This accessor is kept for any
     * future mechanic that genuinely reads it at true runtime (e.g. a
     * growth-chance multiplier checked during {@code randomTick}), not for
     * the stage count itself.
     *
     * @return The configured growth stage count.
     */
    int getSpicePlantGrowthStages();

    /**
     * Configured flat harvest yield (item count) for
     * {@code FLOWER_PATCH}/{@code CROP} Spice Plants. Note: loot table datagen does
     * not call this method directly (a pure {@code runData} pass may run before
     * config is loaded) - it bakes in
     * {@link com.drunkencod.spice_road.Constants#DEFAULT_SPICE_PLANT_HARVEST_YIELD}
     * instead, which is this value's default. Re-run datagen after changing that
     * default to keep the two in sync.
     *
     * @return The configured flat harvest yield.
     */
    int getSpicePlantHarvestYield();
}
