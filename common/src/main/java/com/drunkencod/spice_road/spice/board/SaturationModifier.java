package com.drunkencod.spice_road.spice.board;

import java.util.Map;
import java.util.TreeMap;

import com.drunkencod.spice_road.spice.region.SeededHash;

/**
 * Saturation modifier for a seasoned food that ended up without any effects.
 * It is drawn from a configured range, which may be negative, and scaled down
 * for a food with less spice than a full dose, and seeded by the food item and
 * its contributors, so identical foods give the identical amount and keep
 * stacking.
 */
public final class SaturationModifier {

    private SaturationModifier() {
    }

    /**
     * @param itemId       The registry ID of the food item.
     * @param contributors The food's counted contributors, by registry ID.
     * @return The seed of the modifier, the same for equal foods.
     */
    public static long seedOf(String itemId, Map<String, Double> contributors) {
        long seed = BoardSeed.hashString(itemId);
        for (Map.Entry<String, Double> contributor : new TreeMap<>(contributors).entrySet())
            seed = SeededHash.hash(seed, BoardSeed.hashString(contributor.getKey()),
                    Double.doubleToLongBits(contributor.getValue()));
        return seed;
    }

    /**
     * @param seed        The modifier's seed, see {@link #seedOf}.
     * @param totalAmount The food's total spice amount.
     * @param min         The lower end of the range at a full dose.
     * @param max         The upper end of the range at a full dose.
     * @param fullDose    The spice amount that earns the full modifier.
     * @return The saturation modifier: a seeded value between {@code min} and
     *         {@code max} (swapped if given the wrong way round), scaled by
     *         {@code totalAmount / fullDose} and capped at the full modifier.
     *         Negative if the drawn value is.
     */
    public static double saturation(long seed, double totalAmount, double min, double max, double fullDose) {
        double low = Math.min(min, max);
        double high = Math.max(min, max);
        double drawn = low + (high - low) * SeededHash.toUnitDouble(seed);
        double dose = fullDose <= 0D ? 1D : Math.min(1D, totalAmount / fullDose);
        return drawn * dose;
    }
}
