package com.drunkencod.spice_road.spice;

import java.util.Arrays;
import java.util.function.DoubleUnaryOperator;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.StreamCodec;

/**
 * A score across all {@link FlavorAxis} values. Immutable.
 * <p>
 * Axis values are unbounded (only required to be finite). By convention a
 * single Spice Item's Default Profile stays around {@code [-1, 1]} per axis.
 */
public final class SpiceProfile {

    private static final FlavorAxis[] AXES = FlavorAxis.values();

    /** Profile with every axis at {@code 0}. */
    public static final SpiceProfile ZERO = new SpiceProfile(new double[AXES.length]);

    /**
     * Datapack format: one field per {@link FlavorAxis}, keyed by its
     * lowercase enum name (e.g. {@code "sweet_bitter"}), each a finite double.
     * Field order doesn't matter.
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

    /** Network codec, one double per {@link FlavorAxis} in enum order. */
    public static final StreamCodec<ByteBuf, SpiceProfile> STREAM_CODEC = StreamCodec.of(
            (buf, profile) -> {
                for (double value : profile.values)
                    buf.writeDouble(value);
            },
            buf -> {
                double[] values = new double[AXES.length];
                for (int i = 0; i < values.length; i++)
                    values[i] = buf.readDouble();
                return new SpiceProfile(values);
            });

    private static MapCodec<Double> axisField(FlavorAxis axis) {
        return Codec.DOUBLE.validate(value -> Double.isFinite(value)
                ? DataResult.success(value)
                : DataResult.error(() -> "Flavor axis value for " + axis.getId() + " must be finite, got " + value))
                .fieldOf(axis.getId());
    }

    private final double[] values;

    /**
     * Creates a profile from values given in {@link FlavorAxis} enum order.
     *
     * @param values One finite value per {@link FlavorAxis}, in enum order.
     * @throws IllegalArgumentException If the number of values doesn't match the
     *                                  number of axes, or a value isn't finite.
     */
    public SpiceProfile(double... values) {
        if (values.length != AXES.length) {
            throw new IllegalArgumentException(
                    "Expected " + AXES.length + " flavor axis values, got " + values.length);
        }
        this.values = new double[values.length];
        for (int i = 0; i < values.length; i++) {
            if (!Double.isFinite(values[i]))
                throw new IllegalArgumentException("Flavor axis value for " + AXES[i] + " must be finite, got "
                        + values[i]);
            // Adding 0 turns -0.0 into 0.0, which equals()/hashCode() would otherwise tell apart
            this.values[i] = values[i] + 0D;
        }
    }

    /**
     * Gets this profile's value for the given axis.
     *
     * @param axis The flavor axis to look up.
     * @return The axis value.
     */
    public double get(FlavorAxis axis) {
        return values[axis.ordinal()];
    }

    /**
     * @param other The profile to add.
     * @return The axis-wise sum of this and {@code other}.
     */
    public SpiceProfile add(SpiceProfile other) {
        double[] sum = new double[AXES.length];
        for (int i = 0; i < sum.length; i++)
            sum[i] = values[i] + other.values[i];
        return new SpiceProfile(sum);
    }

    /**
     * @param factor The factor to multiply every axis by.
     * @return This profile with every axis multiplied by {@code factor}.
     */
    public SpiceProfile scale(double factor) {
        return map(value -> value * factor);
    }

    /**
     * @param factors One factor per {@link FlavorAxis}, in enum order.
     * @return This profile with each axis multiplied by its own factor.
     */
    public SpiceProfile scale(double[] factors) {
        double[] scaled = new double[AXES.length];
        for (int i = 0; i < scaled.length; i++)
            scaled[i] = values[i] * factors[i];
        return new SpiceProfile(scaled);
    }

    /**
     * @param operator Applied to every axis value.
     * @return A profile with {@code operator} applied to every axis value.
     */
    public SpiceProfile map(DoubleUnaryOperator operator) {
        double[] mapped = new double[AXES.length];
        for (int i = 0; i < mapped.length; i++)
            mapped[i] = operator.applyAsDouble(values[i]);
        return new SpiceProfile(mapped);
    }

    /** @return Whether every axis is exactly {@code 0}. */
    public boolean isZero() {
        for (double value : values) {
            if (value != 0D)
                return false;
        }
        return true;
    }

    /** @return The largest absolute axis value. */
    public double maxMagnitude() {
        double max = 0D;
        for (double value : values)
            max = Math.max(max, Math.abs(value));
        return max;
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

    @Override
    public String toString() {
        return "SpiceProfile" + Arrays.toString(values);
    }
}
