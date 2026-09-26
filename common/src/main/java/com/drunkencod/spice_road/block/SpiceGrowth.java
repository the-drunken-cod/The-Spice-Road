package com.drunkencod.spice_road.block;

import net.minecraft.util.RandomSource;

/**
 * Shared growth-speed scaling for Spice Plant and Spice Tree blocks whose
 * vanilla parent class owns the actual growth-chance formula.
 */
public final class SpiceGrowth {

    private SpiceGrowth() {
    }

    /**
     * Scales a vanilla growth roll by a speed multiplier (see
     * {@code IConfigHelper#getSpicePlantGrowthSpeedMultiplier}) by performing
     * it once per whole multiplier unit, plus once more with probability equal
     * to the fractional remainder. Each roll is an independent shot at vanilla's
     * own growth check, so this raises/lowers the odds of growing this tick
     * without duplicating vanilla's growth-chance formula.
     *
     * @param multiplier  The growth-speed multiplier. Values {@code <= 0}
     *                    disable growth entirely.
     * @param random      The random source of the ticking level.
     * @param growthRoll  One vanilla growth roll, e.g. a {@code super.randomTick}
     *                    call. Should be passed the originally ticked state rather
     *                    than re-reading it, so extra rolls raise the odds of the
     *                    same transition instead of compounding (and never run
     *                    against a block that has since been replaced).
     */
    public static void rollScaled(double multiplier, RandomSource random, Runnable growthRoll) {
        if (multiplier <= 0)
            return;

        int guaranteedRolls = (int) multiplier;
        double bonusRollChance = multiplier - guaranteedRolls;

        for (int i = 0; i < guaranteedRolls; i++)
            growthRoll.run();

        if (bonusRollChance > 0 && random.nextDouble() < bonusRollChance)
            growthRoll.run();
    }
}
