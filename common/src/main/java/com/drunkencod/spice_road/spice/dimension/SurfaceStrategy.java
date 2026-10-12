package com.drunkencod.spice_road.spice.dimension;

import java.util.Optional;
import java.util.function.IntFunction;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.util.StringRepresentable;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

/**
 * How ground is found in a dimension, for Region Hearts, Heart Groves and the
 * plants around them.
 * <ul>
 * <li>{@link Type#TOP}: the topmost block, i.e. the heightmap. Right for
 * everything with open sky.</li>
 * <li>{@link Type#SCAN_DOWN}: the first floor below {@link #startY()}, with
 * open space above it. For dimensions with a ceiling (the Nether) or void
 * between islands (the End), where the heightmap finds the roof or nothing.</li>
 * </ul>
 *
 * @param type   How ground is found.
 * @param startY Where {@link Type#SCAN_DOWN} starts looking, in blocks. Ignored
 *               by {@link Type#TOP}.
 */
public record SurfaceStrategy(Type type, int startY) {

    /** Default {@link #startY()}. */
    public static final int DEFAULT_START_Y = 120;

    /** The topmost-block strategy, which is the default. */
    public static final SurfaceStrategy TOP = new SurfaceStrategy(Type.TOP, DEFAULT_START_Y);

    /** Codec of the strategy's JSON fields. */
    public static final Codec<SurfaceStrategy> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Type.CODEC.optionalFieldOf("type", Type.TOP).forGetter(SurfaceStrategy::type),
            Codec.INT.optionalFieldOf("start_y", DEFAULT_START_Y).forGetter(SurfaceStrategy::startY))
            .apply(instance, SurfaceStrategy::new));

    /** Furthest a fluid column above a floor is followed, in blocks. */
    private static final int MAX_FLUID_DEPTH = 64;

    /** How a dimension's ground is found. */
    public enum Type implements StringRepresentable {
        /** The topmost block. */
        TOP("top"),
        /** The first floor below a start height. */
        SCAN_DOWN("scan_down");

        /** Codec of the type's name. */
        public static final Codec<Type> CODEC = StringRepresentable.fromEnum(Type::values);

        private final String id;

        Type(String id) {
            this.id = id;
        }

        @Override
        public String getSerializedName() {
            return id;
        }
    }

    /**
     * Ground found by {@link #scanDown}.
     *
     * @param floorY   Height of the first open block above the floor, i.e.
     *                 where fluid or air begins.
     * @param surfaceY Height of the first block above that which isn't fluid.
     *                 Equal to {@code floorY} if the floor is dry.
     */
    public record Ground(int floorY, int surfaceY) {

        /** @return Whether no fluid sits on the floor. */
        public boolean isDry() {
            return floorY >= surfaceY;
        }
    }

    /** @return Whether ground is found by scanning down rather than by heightmap. */
    public boolean isScanDown() {
        return type == Type.SCAN_DOWN;
    }

    /**
     * Finds the first floor at or below {@code fromY}: a block that blocks
     * motion, isn't fluid and isn't bedrock, with a block that doesn't block
     * motion directly above it. Looks no further down than {@code minY}.
     *
     * @param blocks Block at a height of the column being scanned.
     * @param fromY  Height to start at, inclusive.
     * @param minY   Lowest height the floor may be at, exclusive.
     * @return The ground, or empty if the column has no floor in range.
     */
    public static Optional<Ground> scanDown(IntFunction<BlockState> blocks, int fromY, int minY) {
        for (int y = fromY; y > minY; y--) {
            if (!isOpen(blocks.apply(y)) || !isFloor(blocks.apply(y - 1)))
                continue;

            int surface = y;
            while (surface < fromY + MAX_FLUID_DEPTH && !blocks.apply(surface).getFluidState().isEmpty())
                surface++;
            return Optional.of(new Ground(y, surface));
        }
        return Optional.empty();
    }

    @SuppressWarnings("deprecation") // blocksMotion() is what vanilla's heightmaps still use
    private static boolean isOpen(BlockState state) {
        return !state.blocksMotion();
    }

    @SuppressWarnings("deprecation") // blocksMotion() is what vanilla's heightmaps still use
    private static boolean isFloor(BlockState state) {
        return state.blocksMotion() && state.getFluidState().isEmpty() && !state.is(Blocks.BEDROCK);
    }
}
