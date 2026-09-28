package com.drunkencod.spice_road.worldgen;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import com.mojang.serialization.Codec;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;

import com.drunkencod.spice_road.block.SpicePlantBlock;
import com.drunkencod.spice_road.block.SpicePlants;
import com.drunkencod.spice_road.block.SpiceTree;
import com.drunkencod.spice_road.block.SpiceTrees;
import com.drunkencod.spice_road.platform.Services;
import com.drunkencod.spice_road.spice.Climate;
import com.drunkencod.spice_road.spice.Spice;
import com.drunkencod.spice_road.spice.region.RegionHeart;
import com.drunkencod.spice_road.spice.region.RegionHeartSearch;
import com.drunkencod.spice_road.spice.region.SpiceRegionResolver;

/**
 * Places a patch of Spice Plants, shaped by a {@link SpicePlantConfiguration}.
 * Which Spice appears is chosen by {@link SpiceRegionResolver} rather than a
 * fixed per-biome roster.
 * <p>
 * By default each attempt resolves the Spice at its own position, and
 * frequency comes from the placement modifiers, not from this class. With
 * {@link SpicePlantConfiguration#heartSpiceOnly()}, every plant is the Heart
 * Spice of the origin's Spice Region instead, which is how Heart Groves are
 * generated.
 * <p>
 * If the patch's Spice grows as a tree, trees are placed instead, as many as
 * {@link SpicePlantConfiguration#trees()} rolls for the origin's biome, using
 * that tree's datapack-defined configured feature.
 */
public class SpicePlantFeature extends Feature<SpicePlantConfiguration> {

    public SpicePlantFeature(Codec<SpicePlantConfiguration> codec) {
        super(codec);
    }

    @Override
    public boolean place(FeaturePlaceContext<SpicePlantConfiguration> context) {
        WorldGenLevel level = context.level();
        RandomSource random = context.random();
        SpicePlantConfiguration config = context.config();
        BlockPos originSurface = level.getHeightmapPos(Heightmap.Types.WORLD_SURFACE_WG, context.origin());

        Optional<Spice> heartSpice = Optional.empty();
        if (config.heartSpiceOnly()) {
            heartSpice = RegionHeartSearch.heartAt(level.getLevel(), originSurface).map(RegionHeart::spice);
            if (heartSpice.isEmpty())
                return false;
        }

        Optional<Spice> originSpice = heartSpice.isPresent() ? heartSpice : resolveSpiceAt(level, originSurface);
        SpiceTree tree = originSpice.map(SpiceTrees.getRegistered()::get).orElse(null);
        if (tree != null)
            return placeTrees(context, tree, originSurface);

        boolean placedAny = false;
        for (int i = 0; i < config.tries(); i++) {
            BlockPos surfacePos = randomSurfacePos(level, random, context.origin(), config.xzSpread());
            Optional<Spice> spice = heartSpice.isPresent() ? heartSpice : resolveSpiceAt(level, surfacePos);
            if (spice.isPresent() && tryPlaceOne(level, random, surfacePos, spice.get()))
                placedAny = true;
        }
        return placedAny;
    }

    /**
     * Places as many trees as {@link SpicePlantConfiguration#trees()} rolls
     * for the origin's biome, trying the origin first and random positions
     * around it after that. Positions closer than the configured spacing to
     * an already placed trunk are skipped.
     */
    private boolean placeTrees(FeaturePlaceContext<SpicePlantConfiguration> context, SpiceTree tree,
            BlockPos originSurface) {
        SpicePlantConfiguration config = context.config();
        SpicePlantTreeSettings trees = config.trees();
        int maxTrees = trees.sampleCount(context.level().getBiome(originSurface), context.random());
        int minDistanceSq = trees.spacing() * trees.spacing();

        List<BlockPos> trunks = new ArrayList<>();
        for (int i = 0; i < config.tries() && trunks.size() < maxTrees; i++) {
            BlockPos surfacePos = i == 0 ? originSurface
                    : randomSurfacePos(context.level(), context.random(), context.origin(), config.xzSpread());
            boolean tooClose = trunks.stream().anyMatch(trunk -> horizontalDistanceSq(trunk, surfacePos) < minDistanceSq);
            if (!tooClose && tryPlaceTree(context, tree, surfacePos))
                trunks.add(surfacePos);
        }
        return !trunks.isEmpty();
    }

    private static int horizontalDistanceSq(BlockPos a, BlockPos b) {
        int dx = a.getX() - b.getX();
        int dz = a.getZ() - b.getZ();
        return (dx * dx) + (dz * dz);
    }

    /**
     * Places a single tree using the tree's datapack-defined configured feature
     * (see {@link SpiceTree#getTreeFeature()}), if its sapling could survive at
     * {@code surfacePos}.
     */
    private boolean tryPlaceTree(FeaturePlaceContext<SpicePlantConfiguration> context, SpiceTree tree,
            BlockPos surfacePos) {
        WorldGenLevel level = context.level();
        if (!level.isEmptyBlock(surfacePos) || !tree.getSapling().get().defaultBlockState().canSurvive(level, surfacePos))
            return false;

        return level.registryAccess().registryOrThrow(Registries.CONFIGURED_FEATURE)
                .getHolder(tree.getTreeFeature())
                .map(feature -> feature.value().place(level, context.chunkGenerator(), context.random(), surfacePos))
                .orElse(false);
    }

    /** Places one {@code spice} plant at a random growth stage, if it can survive at {@code surfacePos}. */
    private boolean tryPlaceOne(WorldGenLevel level, RandomSource random, BlockPos surfacePos, Spice spice) {
        SpicePlants.RegisteredSpicePlant plant = SpicePlants.getRegistered().get(spice);
        if (plant == null) {
            // Resolved Spice isn't a patch/crop plant (trees are placed
            // separately, BUSH/VINE/RHIZOME have no block yet) - nothing to place.
            return false;
        }

        SpicePlantBlock block = plant.worldgenBlock().get();
        if (!level.isEmptyBlock(surfacePos))
            return false;

        BlockState defaultState = block.defaultBlockState();
        if (!defaultState.canSurvive(level, surfacePos))
            return false;

        int age = random.nextInt(block.getMaxAge() + 1);
        BlockState state = defaultState.setValue(block.getAgeProperty(), age);
        return level.setBlock(surfacePos, state, Block.UPDATE_CLIENTS);
    }

    /** @return The Spice the Spice Region resolves to at {@code pos}, using the climate found there. */
    private static Optional<Spice> resolveSpiceAt(WorldGenLevel level, BlockPos pos) {
        return SpiceRegionResolver
                .resolve(level.getSeed(), Services.CONFIG.getSpiceRegionSalt(),
                        Services.CONFIG.getSpiceRegionCellScale(),
                        Services.CONFIG.getSpiceRegionClusteringStrength(),
                        Climate.fromBiome(level.getBiome(pos), pos), pos.getX(), pos.getZ())
                .spice();
    }

    /** @return The surface position of a random column within {@code spread} blocks of {@code origin}. */
    private static BlockPos randomSurfacePos(WorldGenLevel level, RandomSource random, BlockPos origin, int spread) {
        int dx = random.nextInt(spread * 2 + 1) - spread;
        int dz = random.nextInt(spread * 2 + 1) - spread;
        return level.getHeightmapPos(Heightmap.Types.WORLD_SURFACE_WG, origin.offset(dx, 0, dz));
    }
}
