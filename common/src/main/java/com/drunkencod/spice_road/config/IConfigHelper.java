package com.drunkencod.spice_road.config;

import com.drunkencod.spice_road.spice.Tier;

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
     * Configured harvest yield multiplier for
     * {@code FLOWER_PATCH}/{@code CROP} Spice Plants. Note: loot table datagen does
     * not call this method directly (a pure {@code runData} pass may run before
     * config is loaded) - it bakes in
     * {@link com.drunkencod.spice_road.Constants#DEFAULT_SPICE_PLANT_HARVEST_YIELD_MULTIPLIER}
     * instead, which is this value's default. Re-run datagen after changing that
     * default to keep the two in sync.
     *
     * @return The configured flat harvest yield.
     */
    double getSpicePlantHarvestYieldMultiplier();

    /**
     * Growth-speed multiplier for {@code FLOWER_PATCH}/{@code CROP} Spice Plants
     * of the given {@link Tier} (see {@code Spice#getTier()}), applied on top of
     * vanilla's farmland/light-based growth odds - {@code 1.0} matches vanilla
     * speed, {@code < 1.0} slows growth down, {@code > 1.0} speeds it up.
     * <p>
     * Unlike {@link #getSpicePlantGrowthStages()}, this is read live at
     * tick-time (see {@code SpicePlantBlock#randomTick}), not during
     * block-state-freeze, so there is no constraint against reading it directly
     * from live config.
     *
     * @param tier The Spice's {@link Tier}.
     * @return The configured growth-speed multiplier for that tier.
     */
    double getSpicePlantGrowthSpeedMultiplier(Tier tier);

    /**
     * Harvest yield multiplier for Spice Trees, applied to
     * {@code Spice#getDropAmount()} whenever bark is stripped or fruiting
     * leaves are picked. Read live at harvest time. A fractional result is
     * rounded up or down at random, weighted by its fractional part.
     *
     * @return The configured Spice Tree harvest yield multiplier.
     */
    double getSpiceTreeHarvestYieldMultiplier();

    /**
     * Fraction ({@code 0.0}-{@code 1.0}) of air-exposed, naturally grown
     * leaves of a fruiting Spice Tree of the given {@link Tier} that are able
     * to bear fruit. Which leaves are fruit-bearing is a deterministic
     * function of world seed and position, so changing this value takes
     * effect live without re-growing trees.
     *
     * @param tier The Spice's {@link Tier}.
     * @return The configured fruiting leaves fraction for that tier.
     */
    double getSpiceTreeFruitingLeavesChance(Tier tier);

    /**
     * Client-side. Whether Flavor Axis tooltips show both pole labels of each
     * axis (e.g. {@code [Spicy / Cooling]}), emphasizing the one matching the
     * value's sign, instead of only the matching one.
     *
     * @return Whether both Flavor Axis labels are shown.
     */
    boolean isTooltipBothAxisLabelsShown();

    /**
     * Client-side. Whether Flavor Axis tooltips show each axis' score,
     * multiplied by 10, after its label (e.g. {@code [Spicy: 5]}).
     *
     * @return Whether Flavor Axis values are shown.
     */
    boolean isTooltipAxisValueShown();

    /**
     * Client-side. Whether tooltip content that normally requires holding
     * Shift is always shown instead.
     *
     * @return Whether Shift-gated tooltips are always shown.
     */
    boolean isTooltipShiftBypassed();
}
