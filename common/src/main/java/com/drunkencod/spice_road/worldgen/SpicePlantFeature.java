package com.drunkencod.spice_road.worldgen;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.jetbrains.annotations.Nullable;

import com.mojang.serialization.Codec;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.feature.ConfiguredFeature;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;

import com.drunkencod.spice_road.block.AquaticSpiceRhizomeBlock;
import com.drunkencod.spice_road.block.SpicePlantBlock;
import com.drunkencod.spice_road.block.SpicePlants;
import com.drunkencod.spice_road.block.SpiceTree;
import com.drunkencod.spice_road.block.SpiceTrees;
import com.drunkencod.spice_road.block.SpiceVines;
import com.drunkencod.spice_road.block.SpiceVines.RegisteredSpiceVine;
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
 * that tree's datapack-defined configured feature. A vine Spice is placed the
 * same way, via its Host Tree, whose decorators seed the vine itself.
 * <p>
 * An Aquatic Spice's plants only generate waterlogged, in water exactly one
 * block deep. {@link SpicePlantConfiguration#aquaticPond()} can carve that
 * water first, so a Heart Grove doesn't depend on finding any.
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
            return placeTrees(context, tree.getTreeFeature(), tree.getSapling().get(), originSurface);

        RegisteredSpiceVine vine = originSpice.map(SpiceVines.getRegistered()::get).orElse(null);
        if (vine != null)
            return placeTrees(context, vine.hostTreeFeature(), null, originSurface);

        if (originSpice.isPresent() && originSpice.get().isAquatic())
            config.aquaticPond().ifPresent(pond -> pond.value().place(level, context.chunkGenerator(), random,
                    originSurface));

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
     *
     * @param treeFeature The configured feature placed per tree - a Spice
     *                    Tree's own species, or a vine Spice's Host Tree.
     * @param sapling     Sapling whose survival gates each position, or
     *                    {@code null} to leave the ground check to
     *                    {@code treeFeature} itself, as Host Trees have no
     *                    sapling of their own.
     */
    private boolean placeTrees(FeaturePlaceContext<SpicePlantConfiguration> context,
            ResourceKey<ConfiguredFeature<?, ?>> treeFeature, @Nullable Block sapling, BlockPos originSurface) {
        SpicePlantConfiguration config = context.config();
        SpicePlantTreeSettings trees = config.trees();
        int maxTrees = trees.sampleCount(context.level().getBiome(originSurface), context.random());
        int minDistanceSq = trees.spacing() * trees.spacing();

        List<BlockPos> trunks = new ArrayList<>();
        for (int i = 0; i < config.tries() && trunks.size() < maxTrees; i++) {
            BlockPos surfacePos = i == 0 ? originSurface
                    : randomSurfacePos(context.level(), context.random(), context.origin(), config.xzSpread());
            boolean tooClose = trunks.stream().anyMatch(trunk -> horizontalDistanceSq(trunk, surfacePos) < minDistanceSq);
            if (!tooClose && tryPlaceTree(context, treeFeature, sapling, surfacePos))
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
     * Places a single tree using its datapack-defined configured feature (see
     * {@link SpiceTree#getTreeFeature()} /
     * {@link com.drunkencod.spice_road.block.SpiceVines.RegisteredSpiceVine#hostTreeFeature()}),
     * if {@code sapling} could survive at {@code surfacePos}.
     */
    private boolean tryPlaceTree(FeaturePlaceContext<SpicePlantConfiguration> context,
            ResourceKey<ConfiguredFeature<?, ?>> treeFeature, @Nullable Block sapling, BlockPos surfacePos) {
        WorldGenLevel level = context.level();
        if (!level.isEmptyBlock(surfacePos))
            return false;
        if (sapling != null && !sapling.defaultBlockState().canSurvive(level, surfacePos))
            return false;

        return level.registryAccess().registryOrThrow(Registries.CONFIGURED_FEATURE)
                .getHolder(treeFeature)
                .map(feature -> feature.value().place(level, context.chunkGenerator(), context.random(), surfacePos))
                .orElse(false);
    }

    /**
     * Places one {@code spice} plant at a random growth stage, if it can
     * survive at {@code surfacePos} - or, for an Aquatic Spice, waterlogged in
     * the water just below it (see {@link #findShallowWater}).
     */
    private boolean tryPlaceOne(WorldGenLevel level, RandomSource random, BlockPos surfacePos, Spice spice) {
        SpicePlants.RegisteredSpicePlant plant = SpicePlants.getRegistered().get(spice);
        if (plant == null) {
            // Resolved Spice isn't a patch/crop/rhizome plant (trees and Host
            // Trees are placed separately, BUSH has no block yet) - nothing to
            // place.
            return false;
        }

        SpicePlantBlock block = plant.worldgenBlock().get();
        BlockState defaultState = block.defaultBlockState();
        BlockPos pos = surfacePos;
        if (block instanceof AquaticSpiceRhizomeBlock) {
            pos = findShallowWater(level, surfacePos);
            if (pos == null)
                return false;
            defaultState = defaultState.setValue(AquaticSpiceRhizomeBlock.WATERLOGGED, true);
        } else if (!level.isEmptyBlock(pos)) {
            return false;
        }

        if (!defaultState.canSurvive(level, pos))
            return false;

        int age = random.nextInt(block.getMaxAge() + 1);
        BlockState state = defaultState.setValue(block.getAgeProperty(), age);
        return level.setBlock(pos, state, Block.UPDATE_CLIENTS);
    }

    /**
     * Looks for the topmost water source just below {@code surfacePos}, or
     * ice - a Spice Pond freezes its own surface before its plants are
     * placed, so they're set into the ice as waterlogged plants, the way
     * vanilla's freezing would have left them. Checks two blocks, since the
     * worldgen heightmap may not reflect water carved by a Spice Pond earlier
     * in the same feature. Whether that water is exactly one block deep is
     * left to the plant's own survival check.
     *
     * @return The water source's or ice's position, or {@code null} if there
     *         is neither.
     */
    private static @Nullable BlockPos findShallowWater(WorldGenLevel level, BlockPos surfacePos) {
        for (int depth = 1; depth <= 2; depth++) {
            BlockPos pos = surfacePos.below(depth);
            BlockState state = level.getBlockState(pos);
            FluidState fluid = level.getFluidState(pos);
            boolean water = fluid.isSource() && fluid.is(AquaticSpiceRhizomeBlock.AQUATIC_SPICE_WATER)
                    && state.getBlock() instanceof LiquidBlock;
            if (water || state.is(Blocks.ICE))
                return pos;
        }
        return null;
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
