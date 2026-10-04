package com.drunkencod.spice_road.spice.board;

/**
 * Bonus saturation for a seasoned food in relation to how many different kinds
 * of spice went into it. It grows linearly from a minimum at one kind to a
 * maximum at a configured number of kinds, regardless of amounts or effects.
 */
public final class DiversityAward {

    private DiversityAward() {
    }

    /**
     * @param kinds         How many different kinds of spice the food holds.
     * @param min           The bonus saturation at a single kind.
     * @param max           The bonus saturation at {@code fullDiversity} kinds.
     * @param fullDiversity The number of kinds that earns {@code max}.
     * @return The bonus saturation: {@code 0} without any kinds, otherwise
     *         interpolated between {@code min} and {@code max} (swapped if given
     *         the wrong way round) and capped at {@code max}.
     */
    public static double saturation(int kinds, double min, double max, int fullDiversity) {
        if (kinds <= 0)
            return 0D;
        double low = Math.min(min, max);
        double high = Math.max(min, max);
        double progress = fullDiversity <= 1 ? 1D : Math.min(1D, (kinds - 1D) / (fullDiversity - 1D));
        return low + (high - low) * progress;
    }
}
