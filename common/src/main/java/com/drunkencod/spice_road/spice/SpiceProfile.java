package com.drunkencod.spice_road.spice;

import java.util.Arrays;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

/**
 * A Spice's score across all {@link FlavorAxis} values, for a given item
 * state (raw/dried). Immutable.
 */
public final class SpiceProfile {

    private static final double AXIS_RANGE = 10.0;

    private static final FlavorAxis[] AXES = FlavorAxis.values();

    /**
     * Datapack format: one field per {@link FlavorAxis}, keyed by its
     * lowercase enum name (e.g. {@code "sweet_bitter"}), each a double in
     * range [-1, 1]. Field order doesn't matter.
     */
    public static final Codec<SpiceProfile> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            axisField(FlavorAxis.HEAT_COOLING).forGetter(p -> p.get(FlavorAxis.HEAT_COOLING)),
            axisField(FlavorAxis.SWEET_BITTER).forGetter(p -> p.get(FlavorAxis.SWEET_BITTER)),
            axisField(FlavorAxis.SOUR_MELLOW).forGetter(p -> p.get(FlavorAxis.SOUR_MELLOW)),
            axisField(FlavorAxis.EARTHY_FLORAL).forGetter(p -> p.get(FlavorAxis.EARTHY_FLORAL)),
            axisField(FlavorAxis.WOODY_GREEN).forGetter(p -> p.get(FlavorAxis.WOODY_GREEN)),
            axisField(FlavorAxis.PUNGENT_SOFT).forGetter(p -> p.get(FlavorAxis.PUNGENT_SOFT)),
            axisField(FlavorAxis.RESINOUS_CLEAN).forGetter(p -> p.get(FlavorAxis.RESINOUS_CLEAN)),
            axisField(FlavorAxis.SAVORY_DELICATE).forGetter(p -> p.get(FlavorAxis.SAVORY_DELICATE)))
            .apply(instance, SpiceProfile::new));

    /** Network sync for the {@code spice_road:spice_profile} data component. */
    public static final StreamCodec<ByteBuf, SpiceProfile> STREAM_CODEC = ByteBufCodecs.fromCodec(CODEC);

    private static MapCodec<Double> axisField(FlavorAxis axis) {
        return Codec.doubleRange(-AXIS_RANGE, AXIS_RANGE).fieldOf(axis.getId());
    }

    private final double[] values;

    /**
     * Creates a profile from values given in {@link FlavorAxis} enum order.
     *
     * @param values One value per {@link FlavorAxis}, in enum order, each in range
     *               [-AXIS_RANGE, +AXIS_RANGE].
     * @throws IllegalArgumentException If the number of values doesn't match the
     *                                  number of axes, or is outside
     *                                  [-AXIS_RANGE, +AXIS_RANGE].
     */
    public SpiceProfile(double... values) {
        if (values.length != AXES.length) {
            throw new IllegalArgumentException(
                    "Expected " + AXES.length + " flavor axis values, got " + values.length);
        }

        for (int i = 0; i < values.length; i++) {
            double value = values[i];
            if (value < -AXIS_RANGE || value > AXIS_RANGE) {
                throw new IllegalArgumentException(
                        "Flavor axis value for " + AXES[i] + " must be in range [" + AXIS_RANGE + ", " + (-AXIS_RANGE)
                                + "], got " + value);
            }
        }

        this.values = Arrays.copyOf(values, values.length);
    }

    /**
     * Gets this profile's value for the given axis.
     *
     * @param axis The flavor axis to look up.
     * @return The axis value, in range [-AXIS_RANGE, +AXIS_RANGE].
     */
    public double get(FlavorAxis axis) {
        return values[axis.ordinal()];
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (!(obj instanceof SpiceProfile other))
            return false;
        return Arrays.equals(values, other.values);
    }

    @Override
    public int hashCode() {
        return Arrays.hashCode(values);
    }
}
