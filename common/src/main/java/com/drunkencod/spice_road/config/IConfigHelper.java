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
     * Salt mixed into the world seed when resolving Spice Regions, so region
     * layout can differ between worlds sharing the same seed. Defaults to a
     * random value when the config is first created.
     * <p>
     * <b>Affects world generation:</b> changing it reshuffles every Spice
     * Region, including in already generated chunks.
     *
     * @return The configured Spice Region salt.
     */
    long getSpiceRegionSalt();

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
     * Chance of a Spice Region being a Spiceless Region, containing no Spices
     * at all. See
     * {@code com.drunkencod.spice_road.spice.region.SpiceRegionResolver#isSpiceless}.
     * <p>
     * <b>Affects world generation:</b> changing it turns more regions
     * spiceless or restores some, including in already generated chunks.
     *
     * @return The configured chance, from {@code 0.0} to {@code 1.0}.
     */
    double getSpiceRegionSpicelessChance();

    /**
     * Base pick weight of a Spice of {@code tier} when a Spice Region resolves
     * its Spice, before {@link #getSpiceRegionClusteringStrength()} is applied
     * as an exponent. Higher weights make that tier's Spices more common.
     * <p>
     * <b>Affects world generation:</b> changing it reshuffles which Spice
     * grows where, including in already generated chunks.
     *
     * @param tier The tier of the Spice being weighed.
     * @return The configured weight.
     */
    double getSpiceRegionTierWeight(Tier tier);

    /**
     * Maximum distance, in Spice Region cells, searched for a Region Heart by
     * {@code /locate spice}, {@code /locate spice_climate}, and by Spice Map
     * loot. Measured in cells so changing {@link #getSpiceRegionCellScale()}
     * doesn't change how many hearts are within reach.
     *
     * @return The configured search radius, in cells.
     */
    int getSpiceMapSearchRadiusCells();

    /**
     * Maximum distance, in Spice Region cells, a cartographer searches for a
     * Region Heart when offering a Spice Map. Kept separate from
     * {@link #getSpiceMapSearchRadiusCells()} so trading halls can be balanced
     * on their own.
     *
     * @return The configured villager search radius, in cells.
     */
    int getSpiceMapVillagerSearchRadiusCells();

    /**
     * Whether cartographers offer Spice Map trades. Only affects offers
     * generated from now on.
     *
     * @return Whether Spice Map trades are enabled.
     */
    boolean isSpiceMapTradesEnabled();

    /**
     * Base emerald price of a Spice Map trade, before the per-{@link Tier}
     * multiplier (see {@link #getSpiceMapPriceMultiplier(Tier)}).
     *
     * @return The configured base price, in emeralds.
     */
    int getSpiceMapBasePrice();

    /**
     * Multiplier applied to {@link #getSpiceMapBasePrice()} for Spice Maps of
     * the given {@link Tier}, between {@code 1.0} and {@code 2.0}.
     *
     * @param tier The mapped Spice's {@link Tier}.
     * @return The configured price multiplier for that tier.
     */
    double getSpiceMapPriceMultiplier(Tier tier);

    /**
     * Whether Spice Maps can generate as chest loot. Read whenever loot is
     * rolled, so toggling it needs no {@code /reload}.
     *
     * @return Whether Spice Map loot is enabled.
     */
    boolean isSpiceMapLootEnabled();

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
     * Whether an Aquatic Spice's plant is stunted while not waterlogged. When
     * {@code false}, it grows the same wet or dry. Read live at tick-time.
     *
     * @return Whether Aquatic Spices need water to grow.
     */
    boolean isAquaticSpiceWaterRequired();

    /**
     * Whether flowing water passes through an Aquatic Spice's plant as if it
     * weren't there, via a mixin on vanilla fluid ticking. When {@code false},
     * that mixin does nothing, flowing water can't enter the plant, and
     * planting it into flowing water waterlogs it with a source instead - an
     * escape hatch for mods that change fluid ticking themselves. A plant
     * still holding flowing water when this is turned off is washed away by
     * its water's next update.
     *
     * @return Whether flowing water passes through Aquatic Spice plants.
     */
    boolean isAquaticSpiceFlowThroughEnabled();

    /**
     * Whether bees treat Spice plants in {@code #minecraft:flowers} as flowers
     * at any growth stage, instead of only once ripe. Read live whenever a bee
     * checks a flower.
     *
     * @return Whether bees may pollinate unripe Spice plants.
     */
    boolean isUnripeSpicePollinationAllowed();

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
     * speed, {@code < 1.0} slows growth down, {@code > 1.0} speeds it up. Read
     * live at tick-time (see {@code SpicePlantBlock#randomTick}).
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
     * Fraction ({@code 0.0}-{@code 1.0}) of a Spice Vine's segments of the
     * given {@link Tier} that are able to ripen. Which segments can ripen is a
     * deterministic function of world seed and position, so changing this
     * value takes effect live without regrowing vines.
     *
     * @param tier The Spice's {@link Tier}.
     * @return The configured ripening segments fraction for that tier.
     */
    double getSpiceVineRipeningSegmentsChance(Tier tier);

    /**
     * Magnitude each axis of an Effective Profile saturates towards, giving
     * diminishing returns when stacking many spices. Stored profiles are never
     * capped.
     *
     * @return The configured flavor soft cap.
     */
    double getFlavorSoftCap();

    /**
     * Magnitude any non-zero axis of an Effective Profile counts as at least,
     * so a tiny share of a spice is still noticeable.
     *
     * @return The configured minimum axis magnitude.
     */
    double getFlavorMinimumAxisValue();

    /**
     * Most distinct Seasoning Effects a single food can hold; duplicates merge
     * into one effect of a higher level instead.
     *
     * @return The configured maximum number of effects per food.
     */
    int getSeasoningMaxEffects();

    /**
     * Multiplier applied to the duration of every Seasoning Effect when its
     * food is eaten, on top of each effect's own base duration.
     *
     * @return The configured Seasoning Effect duration multiplier.
     */
    double getSeasoningEffectDurationMultiplier();

    /**
     * Salt mixed into the world seed when building Seasoning Boards, so
     * different packs and servers can get different boards from the same seed.
     *
     * @return The configured board salt.
     */
    long getSeasoningBoardSalt();

    /**
     * @return The points one step on the Seasoning Board costs, paid from the
     *         axis its direction is bound to.
     */
    double getSeasoningStepCost();

    /**
     * @param ring The ring of the effect cell, {@code 2} to {@code 4}.
     * @return The points locking in an effect cell of that ring costs, paid
     *         from the axis of its zone.
     */
    double getSeasoningLockInCost(int ring);

    /**
     * @return The most spices of all kinds that count towards one food's
     *         points; a food keeps its true amounts, only the excess is
     *         ignored.
     */
    int getSeasoningMaxSpices();

    /**
     * @return The most spices of one kind that count towards one food's points.
     */
    int getSeasoningMaxSpicesPerKind();

    /**
     * @return The least bonus saturation a seasoned food without effects gives
     *         at a full dose of spice.
     */
    double getSeasoningParticipationMinSaturation();

    /**
     * @return The most bonus saturation a seasoned food without effects gives
     *         at a full dose of spice.
     */
    double getSeasoningParticipationMaxSaturation();

    /**
     * @return How many spices per food earn the full participation bonus;
     *         fewer earn a proportional share.
     */
    double getSeasoningParticipationFullDose();

    /**
     * Client-side. How long the Spice Grinder GUI waits after a movement key
     * for a second one, so two keys pressed almost together make a diagonal step.
     *
     * @return The configured input buffer in milliseconds.
     */
    int getGrinderInputBufferMs();

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

    /**
     * Client-side. Whether Flavor Axis tooltips count their values and bars up
     * from zero whenever they appear, instead of showing them immediately.
     *
     * @return Whether Flavor Axis tooltips are animated.
     */
    boolean isTooltipAnimated();

    /**
     * Client-side.
     *
     * @return The duration, in milliseconds, of the Flavor Axis tooltip
     *         count-up.
     */
    int getTooltipAnimationDurationMs();

    /**
     * Whether placing an {@link net.minecraft.world.item.ItemStack} carrying a
     * {@code spice_road:spice_profile} override as a block that isn't a
     * {@code BlockEntity} requires sneaking, so the override isn't silently
     * discarded by an accidental right-click (e.g. a Farmer's Delight-style
     * placeable food). When {@code false}, such stacks place as normal without
     * sneaking, same as vanilla.
     *
     * @return Whether sneaking is required to place a flavored food stack.
     */
    boolean isSneakRequiredToPlaceFlavoredFood();
}
