package com.drunkencod.spice_road.spice;

import java.util.Arrays;

/**
 * A Spice's score across all {@link FlavorAxis} values, for a given item
 * state (raw/dried). Immutable.
 */
public final class SpiceProfile {

    private static final FlavorAxis[] AXES = FlavorAxis.values();

    private final double[] values;

    /**
     * Creates a profile from values given in {@link FlavorAxis} enum order.
     *
     * @param values One value per {@link FlavorAxis}, in enum order, each in range
     *               [-1, 1].
     * @throws IllegalArgumentException If the number of values doesn't match the
     *                                  number of axes, or is outside [-1, 1].
     */
    public SpiceProfile(double... values) {

        if (values.length != AXES.length) {
            throw new IllegalArgumentException(
                    "Expected " + AXES.length + " flavor axis values, got " + values.length);
        }

        for (int i = 0; i < values.length; i++) {
            double value = values[i];
            if (value < -1.0 || value > 1.0) {
                throw new IllegalArgumentException(
                        "Flavor axis value for " + AXES[i] + " must be in range [-1, 1], got " + value);
            }
        }

        this.values = Arrays.copyOf(values, values.length);
    }

    /**
     * Gets this profile's value for the given axis.
     *
     * @param axis The flavor axis to look up.
     * @return The axis value, in range [-1, 1].
     */
    public double get(FlavorAxis axis) {

        return values[axis.ordinal()];
    }
}
