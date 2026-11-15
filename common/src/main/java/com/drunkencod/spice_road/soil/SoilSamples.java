package com.drunkencod.spice_road.soil;

import java.util.ArrayList;
import java.util.List;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;

/**
 * The Soil Samples a Soil Analyzer holds so far: where each was taken, as
 * positions in the world the samples' dimension shows (a sample on a Sable
 * sub-level is stored where the sub-level currently is), and the one
 * dimension they all belong to.
 *
 * @param dimension The dimension every sample was taken in.
 * @param positions The sample positions, oldest first.
 */
public record SoilSamples(ResourceKey<Level> dimension, List<BlockPos> positions) {

    /** Saved form. */
    public static final Codec<SoilSamples> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Level.RESOURCE_KEY_CODEC.fieldOf("dimension").forGetter(SoilSamples::dimension),
            BlockPos.CODEC.listOf().fieldOf("positions").forGetter(SoilSamples::positions))
            .apply(instance, SoilSamples::new));

    /** Network form. */
    public static final StreamCodec<RegistryFriendlyByteBuf, SoilSamples> STREAM_CODEC = StreamCodec.composite(
            ResourceKey.streamCodec(Registries.DIMENSION), SoilSamples::dimension,
            BlockPos.STREAM_CODEC.apply(ByteBufCodecs.list()), SoilSamples::positions,
            SoilSamples::new);

    /** Why a new sample can't join a set. */
    public enum Spacing {
        /** The sample is fine. */
        OK,
        /** The set was taken in another dimension. */
        OTHER_DIMENSION,
        /** The sample is too near an earlier one. */
        TOO_CLOSE,
        /** The sample is too far from the first one. */
        TOO_FAR
    }

    /**
     * @param dimension The dimension of the first sample.
     * @param first     Where the first sample was taken.
     * @return A set holding just that sample.
     */
    public static SoilSamples startedWith(ResourceKey<Level> dimension, BlockPos first) {
        return new SoilSamples(dimension, List.of(first.immutable()));
    }

    /** @return How many samples there are. */
    public int count() {
        return positions.size();
    }

    /**
     * @param pos The position of one more sample.
     * @return This set with that sample appended.
     */
    public SoilSamples with(BlockPos pos) {
        List<BlockPos> grown = new ArrayList<>(positions);
        grown.add(pos.immutable());
        return new SoilSamples(dimension, List.copyOf(grown));
    }

    /**
     * Checks whether a sample may join a set. Distances are horizontal, as the
     * analysis only looks at a position's X and Z.
     *
     * @param samples     The samples so far, or {@code null} if there are none.
     * @param dimension   The dimension the new sample is taken in.
     * @param pos         Where the new sample is taken.
     * @param minDistance Blocks the sample must keep from every earlier one.
     * @param maxSpan     Blocks the sample may be from the first one.
     * @return {@link Spacing#OK} if it may join, otherwise the rule it breaks.
     */
    public static Spacing check(SoilSamples samples, ResourceKey<Level> dimension, BlockPos pos, int minDistance,
            int maxSpan) {
        if (samples == null || samples.positions.isEmpty())
            return Spacing.OK;
        if (!samples.dimension.equals(dimension))
            return Spacing.OTHER_DIMENSION;
        if (horizontalDistanceSqr(samples.positions.get(0), pos) > (double) maxSpan * maxSpan)
            return Spacing.TOO_FAR;
        for (BlockPos earlier : samples.positions) {
            if (horizontalDistanceSqr(earlier, pos) < (double) minDistance * minDistance)
                return Spacing.TOO_CLOSE;
        }
        return Spacing.OK;
    }

    /**
     * @return The average of the sample positions, rounded to a block. Empty
     *         sets have no centroid, but never reach this.
     */
    public BlockPos centroid() {
        double x = 0;
        double y = 0;
        double z = 0;
        for (BlockPos pos : positions) {
            x += pos.getX();
            y += pos.getY();
            z += pos.getZ();
        }
        int size = Math.max(1, positions.size());
        return new BlockPos((int) Math.round(x / size), (int) Math.round(y / size), (int) Math.round(z / size));
    }

    private static double horizontalDistanceSqr(BlockPos a, BlockPos b) {
        double dx = a.getX() - b.getX();
        double dz = a.getZ() - b.getZ();
        return (dx * dx) + (dz * dz);
    }
}
