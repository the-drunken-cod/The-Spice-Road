package com.drunkencod.spice_road.worldgen;

import java.util.Optional;
import java.util.function.Function;
import java.util.function.IntFunction;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.WorldGenRegion;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;

import com.drunkencod.spice_road.spice.dimension.SpiceDimension;
import com.drunkencod.spice_road.spice.dimension.SpiceDimensions;
import com.drunkencod.spice_road.spice.dimension.SurfaceStrategy;

/**
 * Finds the ground a feature stands on, depending on where it runs. The
 * {@code _WG} heightmaps only exist while a chunk generates, so a feature
 * placed into a finished chunk (e.g. with {@code /place feature}) would find
 * no ground with them; it reads their live equivalents instead.
 * <p>
 * Dimensions whose {@link SurfaceStrategy} scans down (the Nether's ceiling,
 * the End's void) don't use heightmaps at all: the topmost block there is the
 * roof or nothing, so the first floor below a start height is used instead,
 * read from the real blocks. {@code nearY} arguments say where that floor is
 * expected, so a cave dimension's several levels aren't mixed up.
 */
public final class SurfaceHeightmaps {

    /** How far above {@code nearY} a floor still counts as the one expected there, in blocks. */
    private static final int NEAR_RISE = 8;

    private SurfaceHeightmaps() {
    }

    /**
     * @param level The level the feature is placed in.
     * @return The topmost non-air block's heightmap, fluids included:
     *         {@link Heightmap.Types#WORLD_SURFACE_WG} during worldgen,
     *         {@link Heightmap.Types#WORLD_SURFACE} otherwise.
     */
    public static Heightmap.Types surface(LevelAccessor level) {
        return level instanceof WorldGenRegion ? Heightmap.Types.WORLD_SURFACE_WG : Heightmap.Types.WORLD_SURFACE;
    }

    /**
     * @param level The level the feature is placed in.
     * @return The topmost solid ground's heightmap, fluids excluded:
     *         {@link Heightmap.Types#OCEAN_FLOOR_WG} during worldgen,
     *         {@link Heightmap.Types#OCEAN_FLOOR} otherwise.
     */
    public static Heightmap.Types floor(LevelAccessor level) {
        return level instanceof WorldGenRegion ? Heightmap.Types.OCEAN_FLOOR_WG : Heightmap.Types.OCEAN_FLOOR;
    }

    /**
     * @param level The level the feature is placed in.
     * @return How ground is found in its dimension.
     */
    public static SurfaceStrategy strategyOf(LevelAccessor level) {
        ResourceLocation dimension = null;
        if (level instanceof ServerLevelAccessor accessor)
            dimension = accessor.getLevel().dimension().location();
        else if (level instanceof Level real)
            dimension = real.dimension().location();
        return dimension == null ? SurfaceStrategy.TOP
                : SpiceDimensions.get(dimension).map(SpiceDimension::surface).orElse(SurfaceStrategy.TOP);
    }

    /**
     * Finds the surface of a column: the block position where something would
     * stand, fluids included.
     *
     * @param level  The level the feature is placed in.
     * @param column Any position in the column. Its height only matters in
     *               dimensions that scan down, where it says where the ground
     *               is expected.
     * @return The surface position. In a dimension that scans down and finds no
     *         ground, the lowest position of the world, see
     *         {@link #isGround}.
     */
    public static BlockPos surfacePos(LevelAccessor level, BlockPos column) {
        SurfaceStrategy strategy = strategyOf(level);
        if (!strategy.isScanDown())
            return level.getHeightmapPos(surface(level), column);

        int y = scan(level, strategy, column.getX(), column.getZ(), column.getY())
                .map(SurfaceStrategy.Ground::surfaceY).orElse(level.getMinBuildHeight());
        return new BlockPos(column.getX(), y, column.getZ());
    }

    /**
     * Finds the height of the solid ground of a column, fluids excluded, like
     * the {@linkplain #floor floor heightmap}.
     *
     * @param level The level the feature is placed in.
     * @param x     X coordinate of the column.
     * @param z     Z coordinate of the column.
     * @param nearY Where the ground is expected, for dimensions that scan down.
     * @return The height of the first block above the ground. In a dimension
     *         that scans down and finds no ground, the lowest height of the
     *         world.
     */
    public static int floorHeight(LevelAccessor level, int x, int z, int nearY) {
        SurfaceStrategy strategy = strategyOf(level);
        if (!strategy.isScanDown())
            return level.getHeight(floor(level), x, z);

        return scan(level, strategy, x, z, nearY).map(SurfaceStrategy.Ground::floorY)
                .orElse(level.getMinBuildHeight());
    }

    /**
     * @param level   The level the feature is placed in.
     * @param surface A position found by {@link #surfacePos}.
     * @return Whether there is ground below it. Always true unless the
     *         dimension scans down and the column was empty, like End void.
     */
    public static boolean isGround(LevelAccessor level, BlockPos surface) {
        return !strategyOf(level).isScanDown() || surface.getY() > level.getMinBuildHeight();
    }

    /**
     * Scans a column of real blocks for its floor. Looks near {@code nearY}
     * first, and from the strategy's start height if nothing is found there.
     */
    private static Optional<SurfaceStrategy.Ground> scan(LevelAccessor level, SurfaceStrategy strategy, int x, int z,
            int nearY) {
        return scan(strategy, level::getBlockState, x, z, nearY, level.getMinBuildHeight());
    }

    /**
     * Scans a column of real blocks for its floor, for callers that read blocks
     * through something other than a {@link LevelAccessor}, like a placement
     * modifier's context. Looks near {@code nearY} first, and from the
     * strategy's start height if nothing is found there.
     *
     * @param strategy A strategy that {@linkplain SurfaceStrategy#isScanDown scans down}.
     * @param blocks   Reads a block of the world.
     * @param x        X coordinate of the column.
     * @param z        Z coordinate of the column.
     * @param nearY    Where the ground is expected.
     * @param minY     The lowest height of the world.
     * @return The ground, or empty if the column has none.
     */
    public static Optional<SurfaceStrategy.Ground> scan(SurfaceStrategy strategy,
            Function<BlockPos, BlockState> blocks, int x, int z, int nearY, int minY) {
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        IntFunction<BlockState> column = y -> blocks.apply(pos.set(x, y, z));

        int nearStart = Math.min(strategy.startY(), nearY + NEAR_RISE);
        Optional<SurfaceStrategy.Ground> ground = SurfaceStrategy.scanDown(column, nearStart, minY);
        if (ground.isEmpty() && nearStart < strategy.startY())
            ground = SurfaceStrategy.scanDown(column, strategy.startY(), minY);
        return ground;
    }
}
