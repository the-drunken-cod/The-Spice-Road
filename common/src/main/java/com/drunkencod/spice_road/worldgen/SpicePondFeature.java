package com.drunkencod.spice_road.worldgen;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashSet;
import java.util.Set;

import com.mojang.serialization.Codec;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.SectionPos;
import net.minecraft.server.level.WorldGenRegion;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.SnowyDirtBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.material.Fluids;

/**
 * Carves a Spice Pond, shaped by a {@link SpicePondConfiguration}: a roughly
 * round pond exactly one block of water deep, level with the ground at its
 * origin. Higher ground within it is cut away, lower ground is filled up, and
 * wherever the water would spill out a bank is raised to hold it in. Then its
 * shore is painted, its decorations are generated around it and on its water
 * (biome permitting, see {@link #placeWaterDecorations}), and its surface
 * is frozen and snowed on wherever the biome would (see
 * {@link #freezeTopLayer}).
 * <p>
 * Only generates on dry, solid ground, so it never cuts into existing water.
 * Shrinks to fit the area worldgen may write to, so a pond near a chunk's edge
 * may come out smaller than configured.
 */
public class SpicePondFeature extends Feature<SpicePondConfiguration> {

    /**
     * Highest ground above the water surface a pond still cuts through, in blocks.
     */
    private static final int MAX_CUT_HEIGHT = 3;

    /**
     * Deepest ground below the water surface a pond still fills up from, in blocks.
     */
    private static final int MAX_FILL_DEPTH = 3;

    /**
     * Most logs of a single tree a pond removes, so a giant tree or log pile can't
     * stall worldgen.
     */
    private static final int MAX_TREE_LOGS = 512;

    /** How far the pond's edge is pushed in or out at most, in blocks. */
    private static final float EDGE_ROUGHNESS = 1.0F;

    /** How far past the water's edge banks are raised, in blocks. */
    private static final double BANK_WIDTH = 1.5D;

    /**
     * Minimum distance between a decoration and the edge of the area worldgen
     * may write to, in blocks, leaving room for e.g. a tree's canopy.
     * Decorations closer than that are skipped.
     */
    private static final int DECORATION_MARGIN = 3;

    public SpicePondFeature(Codec<SpicePondConfiguration> codec) {
        super(codec);
    }

    @Override
    public boolean place(FeaturePlaceContext<SpicePondConfiguration> context) {
        WorldGenLevel level = context.level();
        RandomSource random = context.random();
        SpicePondConfiguration config = context.config();

        BlockPos origin = level.getHeightmapPos(SurfaceHeightmaps.surface(level), context.origin());
        BlockPos ground = origin.below();
        if (!level.getFluidState(ground).isEmpty() || needsBank(level.getBlockState(ground)))
            return false;

        // Shrunk to fit, so a pond near the edge of what worldgen may write to
        // stays whole instead of spilling through a missing bank.
        WritableArea area = WritableArea.of(level);
        int margin = Mth.ceil(EDGE_ROUGHNESS + BANK_WIDTH) + config.shoreWidth();
        int radius = Math.min(config.radius().sample(random), area.reachFrom(origin) - margin);
        if (radius < 1)
            return false;

        int waterY = ground.getY();
        if (!isFlatEnough(level, origin, radius + (int) Math.ceil(EDGE_ROUGHNESS + BANK_WIDTH), waterY,
                config.maxSlope()))
            return false;

        float phaseA = random.nextFloat() * Mth.TWO_PI;
        float phaseB = random.nextFloat() * Mth.TWO_PI;
        int reach = radius + margin;

        for (int dx = -reach; dx <= reach; dx++) {
            for (int dz = -reach; dz <= reach; dz++) {
                double distance = Math.sqrt((dx * dx) + (dz * dz));
                float angle = (float) Mth.atan2(dz, dx);
                double edge = radius + EDGE_ROUGHNESS
                        * ((0.6F * Mth.sin((2.0F * angle) + phaseA)) + (0.4F * Mth.sin((3.0F * angle) + phaseB)));
                int x = origin.getX() + dx;
                int z = origin.getZ() + dz;

                if (distance <= edge) {
                    carveWater(level, area, random, config, x, z, waterY);
                    continue;
                }
                if (distance <= edge + BANK_WIDTH)
                    raiseBank(level, random, config, x, z, waterY);
                if (distance <= edge + BANK_WIDTH + config.shoreWidth())
                    paintShore(level, random, config, x, z, waterY);
            }
        }

        placeDecorations(context, area, origin, radius);
        placeWaterDecorations(context, area, origin, radius, waterY);
        // After decorations, since trees checked against a sapling won't grow on snow.
        freezeTopLayer(level, area, origin, reach, waterY);
        return true;
    }

    // #region pond

    /**
     * @return Whether the ground within {@code reach} of {@code origin} stays
     *         within {@code maxSlope} blocks of {@code waterY}, so the pond
     *         sits in the terrain instead of hanging off a hillside.
     */
    private static boolean isFlatEnough(WorldGenLevel level, BlockPos origin, int reach, int waterY, int maxSlope) {
        for (int dx = -reach; dx <= reach; dx++) {
            for (int dz = -reach; dz <= reach; dz++) {
                if ((dx * dx) + (dz * dz) > reach * reach)
                    continue;
                int groundY = level.getHeight(SurfaceHeightmaps.floor(level), origin.getX() + dx, origin.getZ() + dz)
                        - 1;
                if (Math.abs(groundY - waterY) > maxSlope)
                    return false;
            }
        }
        return true;
    }

    /**
     * Turns the column at {@code (x, z)} into pond: water at {@code waterY}
     * with a floor below it, cutting away higher ground and filling up lower
     * ground. Ground too far off the water level is treated as a bank
     * instead.
     */
    @SuppressWarnings("deprecation") // blocksMotion() is what vanilla's heightmaps still use
    private static void carveWater(WorldGenLevel level, WritableArea area, RandomSource random,
            SpicePondConfiguration config, int x, int z, int waterY) {
        int groundY = level.getHeight(SurfaceHeightmaps.floor(level), x, z) - 1;
        if (groundY > waterY + MAX_CUT_HEIGHT || groundY < waterY - MAX_FILL_DEPTH) {
            raiseBank(level, random, config, x, z, waterY);
            return;
        }

        // Clears cut ground plus any vegetation on or above it, which would
        // otherwise float over the water.
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos(x, waterY, z);
        for (int y = waterY + 1; y <= waterY + MAX_CUT_HEIGHT + 2; y++) {
            pos.setY(y);
            BlockState state = level.getBlockState(pos);
            if (state.isAir() || !level.getFluidState(pos).isEmpty())
                continue;
            if (state.is(BlockTags.LOGS)) {
                removeTree(level, area, pos);
                continue;
            }
            if (y <= groundY || !state.blocksMotion())
                level.setBlock(pos, Blocks.AIR.defaultBlockState(), Block.UPDATE_CLIENTS);
        }

        pos.setY(waterY);
        level.setBlock(pos, Blocks.WATER.defaultBlockState(), Block.UPDATE_CLIENTS);
        for (int y = Math.min(groundY + 1, waterY - 1); y < waterY; y++) {
            pos.setY(y);
            level.setBlock(pos, config.floor().getState(random, pos), Block.UPDATE_CLIENTS);
        }
    }

    /**
     * Removes the whole tree that the log at {@code start} belongs to, so its
     * trunk or canopy isn't left floating once the ground under it is cut away.
     * Follows connected logs, then the leaves they support by their
     * {@link LeavesBlock#DISTANCE} (leaves also held up by another tree stay).
     * Blocks outside {@code area} are left alone.
     */
    private static void removeTree(WorldGenLevel level, WritableArea area, BlockPos start) {
        Set<BlockPos> logs = new HashSet<>();
        Deque<BlockPos> queue = new ArrayDeque<>();
        logs.add(start.immutable());
        queue.add(start.immutable());
        while (!queue.isEmpty() && logs.size() < MAX_TREE_LOGS) {
            BlockPos current = queue.poll();
            for (BlockPos next : BlockPos.betweenClosed(current.offset(-1, -1, -1), current.offset(1, 1, 1))) {
                if (area.reachFrom(next) < 0 || !level.getBlockState(next).is(BlockTags.LOGS))
                    continue;
                BlockPos key = next.immutable();
                if (logs.add(key))
                    queue.add(key);
            }
        }

        Set<BlockPos> leaves = new HashSet<>();
        Deque<BlockPos> frontier = new ArrayDeque<>(logs);
        for (int distance = 1; distance <= LeavesBlock.DECAY_DISTANCE && !frontier.isEmpty(); distance++) {
            Deque<BlockPos> nextFrontier = new ArrayDeque<>();
            for (BlockPos current : frontier) {
                for (Direction direction : Direction.values()) {
                    BlockPos next = current.relative(direction);
                    BlockState state = level.getBlockState(next);
                    if (area.reachFrom(next) >= 0 && state.getBlock() instanceof LeavesBlock
                            && state.getValue(LeavesBlock.DISTANCE) == distance && leaves.add(next))
                        nextFrontier.add(next);
                }
            }
            frontier = nextFrontier;
        }

        for (BlockPos pos : logs)
            level.setBlock(pos, Blocks.AIR.defaultBlockState(), Block.UPDATE_CLIENTS);
        for (BlockPos pos : leaves)
            level.setBlock(pos, Blocks.AIR.defaultBlockState(), Block.UPDATE_CLIENTS);
    }

    /**
     * Fills the column at {@code (x, z)} with floor blocks wherever pond water
     * could spill through.
     */
    private static void raiseBank(WorldGenLevel level, RandomSource random, SpicePondConfiguration config, int x,
            int z, int waterY) {
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos(x, waterY - 1, z);
        for (int y = waterY - 1; y <= waterY; y++) {
            pos.setY(y);
            if (needsBank(level.getBlockState(pos)))
                level.setBlock(pos, config.floor().getState(random, pos), Block.UPDATE_CLIENTS);
        }
    }

    /**
     * Replaces the column's {@link SpicePondConfiguration#replaceable()
     * replaceable} ground near the water level with shore.
     */
    private static void paintShore(WorldGenLevel level, RandomSource random, SpicePondConfiguration config, int x,
            int z, int waterY) {
        if (config.shore().isEmpty())
            return;

        BlockPos pos = new BlockPos(x, level.getHeight(SurfaceHeightmaps.floor(level), x, z) - 1, z);
        if (Math.abs(pos.getY() - waterY) > 1 || !level.getBlockState(pos).is(config.replaceable()))
            return;
        level.setBlock(pos, config.shore().get().getState(random, pos), Block.UPDATE_CLIENTS);
    }

    /**
     * @return Whether water could flow through {@code state}, e.g. air, a fluid, or
     *         a plant.
     */
    @SuppressWarnings("deprecation")
    private static boolean needsBank(BlockState state) {
        return !state.blocksMotion() || !state.getFluidState().isEmpty();
    }

    // #region top layer

    /**
     * Does vanilla's {@code minecraft:freeze_top_layer} work for the columns
     * within {@code reach} of {@code origin}: freezes exposed water and lays
     * snow wherever the biome would. Vanilla only does that for the chunk
     * being decorated, so without this a pond spilling into a neighboring
     * chunk that was already decorated would stay open water past the chunk
     * border. Waterlogged blocks, like Aquatic Spice plants, never freeze.
     * <p>
     * Runs on the pond's own columns only, from just below its water up to
     * just above the highest ground it cut through.
     */
    private static void freezeTopLayer(WorldGenLevel level, WritableArea area, BlockPos origin, int reach,
            int waterY) {
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        BlockPos.MutableBlockPos below = new BlockPos.MutableBlockPos();
        for (int dx = -reach; dx <= reach; dx++) {
            for (int dz = -reach; dz <= reach; dz++) {
                pos.set(origin.getX() + dx, waterY, origin.getZ() + dz);
                if (area.reachFrom(pos) < 0)
                    continue;

                int topY = findTopLayer(level, pos, waterY);
                if (topY == Integer.MIN_VALUE)
                    continue;

                pos.setY(topY + 1);
                below.setWithOffset(pos, Direction.DOWN);
                Biome biome = level.getBiome(pos).value();
                if (biome.shouldFreeze(level, below, false))
                    level.setBlock(below, Blocks.ICE.defaultBlockState(), Block.UPDATE_CLIENTS);

                if (biome.shouldSnow(level, pos)) {
                    level.setBlock(pos, Blocks.SNOW.defaultBlockState(), Block.UPDATE_CLIENTS);
                    BlockState belowState = level.getBlockState(below);
                    if (belowState.hasProperty(SnowyDirtBlock.SNOWY))
                        level.setBlock(below, belowState.setValue(SnowyDirtBlock.SNOWY, true), Block.UPDATE_CLIENTS);
                }
            }
        }
    }

    /**
     * Mirrors the {@code MOTION_BLOCKING} heightmap's rule within the range a
     * pond changes, since heightmaps of neighboring chunks aren't reliably up
     * to date during worldgen.
     *
     * @return The Y of the topmost block at {@code column} that blocks motion
     *         or holds fluid, or {@link Integer#MIN_VALUE} if there is none
     *         in range.
     */
    @SuppressWarnings("deprecation")
    private static int findTopLayer(WorldGenLevel level, BlockPos column, int waterY) {
        BlockPos.MutableBlockPos pos = column.mutable();
        for (int y = waterY + MAX_CUT_HEIGHT + 2; y >= waterY - MAX_FILL_DEPTH - 1; y--) {
            pos.setY(y);
            BlockState state = level.getBlockState(pos);
            if (state.blocksMotion() || !state.getFluidState().isEmpty())
                return y;
        }
        return Integer.MIN_VALUE;
    }

    // #region decorations

    /**
     * Generates every {@link SpicePondConfiguration#decorations() decoration}
     * at random surface positions on the shore, just past the water's edge.
     * Positions within {@link #DECORATION_MARGIN} of {@code area}'s edge are
     * skipped.
     */
    private static void placeDecorations(FeaturePlaceContext<SpicePondConfiguration> context, WritableArea area,
            BlockPos origin, int radius) {
        WorldGenLevel level = context.level();
        RandomSource random = context.random();
        SpicePondConfiguration config = context.config();

        for (SpicePondConfiguration.Decoration decoration : config.decorations()) {
            int count = decoration.count().sample(random);
            for (int i = 0; i < count; i++) {
                float angle = random.nextFloat() * Mth.TWO_PI;
                double distance = radius + EDGE_ROUGHNESS + BANK_WIDTH + (random.nextFloat() * config.shoreWidth());
                BlockPos column = origin.offset(Mth.floor(Mth.cos(angle) * distance), 0,
                        Mth.floor(Mth.sin(angle) * distance));
                if (area.reachFrom(column) < DECORATION_MARGIN)
                    continue;

                BlockPos surface = level.getHeightmapPos(SurfaceHeightmaps.surface(level), column);
                decoration.feature().value().place(level, context.chunkGenerator(), random, surface);
            }
        }
    }

    /**
     * Generates every {@link SpicePondConfiguration#waterDecorations() water
     * decoration} one block above the water, at random columns within
     * {@code radius} of {@code origin}, unless the origin's biome is in
     * {@link SpicePondConfiguration#noWaterDecorationBiomes()}. Columns that
     * aren't water, e.g. past the roughened edge, and columns within
     * {@link #DECORATION_MARGIN} of {@code area}'s edge are skipped.
     */
    private static void placeWaterDecorations(FeaturePlaceContext<SpicePondConfiguration> context,
            WritableArea area, BlockPos origin, int radius, int waterY) {
        WorldGenLevel level = context.level();
        RandomSource random = context.random();
        SpicePondConfiguration config = context.config();
        if (config.waterDecorations().isEmpty() || level.getBiome(origin).is(config.noWaterDecorationBiomes()))
            return;

        for (SpicePondConfiguration.Decoration decoration : config.waterDecorations()) {
            int count = decoration.count().sample(random);
            for (int i = 0; i < count; i++) {
                float angle = random.nextFloat() * Mth.TWO_PI;
                double distance = Math.sqrt(random.nextFloat()) * radius;
                BlockPos surface = new BlockPos(origin.getX() + Mth.floor(Mth.cos(angle) * distance), waterY + 1,
                        origin.getZ() + Mth.floor(Mth.sin(angle) * distance));
                if (area.reachFrom(surface) < DECORATION_MARGIN || !level.getFluidState(surface.below()).is(Fluids.WATER))
                    continue;

                decoration.feature().value().place(level, context.chunkGenerator(), random, surface);
            }
        }
    }

    // #region writable area

    /**
     * The block columns worldgen may write to while decorating a chunk: that
     * chunk plus {@link #WRITE_RADIUS} chunks around it. Unbounded outside of
     * chunk generation, e.g. for {@code /place feature}.
     */
    private record WritableArea(int minX, int maxX, int minZ, int maxZ) {

        /** Chunk radius around the decorated chunk vanilla lets features write to. */
        private static final int WRITE_RADIUS = 1;

        private static final WritableArea UNBOUNDED = new WritableArea(Integer.MIN_VALUE, Integer.MAX_VALUE,
                Integer.MIN_VALUE, Integer.MAX_VALUE);

        /** @return The area {@code level} may be written to in. */
        static WritableArea of(WorldGenLevel level) {
            if (!(level instanceof WorldGenRegion region))
                return UNBOUNDED;

            ChunkPos center = region.getCenter();
            return new WritableArea(
                    SectionPos.sectionToBlockCoord(center.x - WRITE_RADIUS),
                    SectionPos.sectionToBlockCoord(center.x + WRITE_RADIUS, 15),
                    SectionPos.sectionToBlockCoord(center.z - WRITE_RADIUS),
                    SectionPos.sectionToBlockCoord(center.z + WRITE_RADIUS, 15));
        }

        /**
         * @return Horizontal distance from {@code pos} to this area's nearest edge, in
         *         blocks.
         */
        int reachFrom(BlockPos pos) {
            long reach = Math.min(Math.min((long) pos.getX() - minX, (long) maxX - pos.getX()),
                    Math.min((long) pos.getZ() - minZ, (long) maxZ - pos.getZ()));
            return (int) Mth.clamp(reach, Integer.MIN_VALUE, Integer.MAX_VALUE);
        }
    }
}
