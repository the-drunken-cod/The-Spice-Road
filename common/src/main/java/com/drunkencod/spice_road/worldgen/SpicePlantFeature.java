package com.drunkencod.spice_road.worldgen;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.jetbrains.annotations.Nullable;

import com.mojang.serialization.Codec;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.levelgen.feature.ConfiguredFeature;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;

import com.drunkencod.spice_road.Constants;
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
 * By default the Spice is resolved once at the origin, so a patch on a region
 * border never mixes Spices. Frequency comes from the placement modifiers,
 * not from this class. With {@link SpicePlantConfiguration#heartSpiceOnly()},
 * every plant is the Heart Spice of the origin's Spice Region instead, which
 * is how Heart Groves are generated.
 * <p>
 * If the patch's Spice grows as a tree, trees are placed instead, as many as
 * {@link SpicePlantConfiguration#trees()} rolls for the origin's biome, using
 * that tree's datapack-defined configured feature. A vine Spice is placed the
 * same way, via its Host Tree, whose decorators seed the vine itself.
 * <p>
 * An Aquatic Spice's plants only generate waterlogged, in water exactly one
 * block deep. {@link SpicePlantConfiguration#aquaticPond()} can carve that
 * water first, so a Heart Grove doesn't depend on finding any. Likewise,
 * {@link SpicePlantConfiguration#oasis()} can carve an oasis with a
 * spice-growable shore wherever the ground can't grow other Spices, e.g. in a
 * desert.
 */
public class SpicePlantFeature extends Feature<SpicePlantConfiguration> {

    /**
     * Biomes where no patch, tree or grove generates, whatever Spice the region
     * resolves to.
     */
    public static final TagKey<Biome> NO_SPICE_PLANTS = TagKey.create(Registries.BIOME,
            ResourceLocation.fromNamespaceAndPath(Constants.MOD_ID, "no_spice_plants"));

    public SpicePlantFeature(Codec<SpicePlantConfiguration> codec) {
        super(codec);
    }

    @Override
    public boolean place(FeaturePlaceContext<SpicePlantConfiguration> context) {
        WorldGenLevel level = context.level();
        RandomSource random = context.random();
        SpicePlantConfiguration config = context.config();
        BlockPos originSurface = level.getHeightmapPos(SurfaceHeightmaps.surface(level), context.origin());
        if (level.getBiome(originSurface).is(NO_SPICE_PLANTS))
            return false;

        Optional<Spice> heartSpice = Optional.empty();
        if (config.heartSpiceOnly()) {
            heartSpice = RegionHeartSearch.heartAt(level.getLevel(), originSurface).map(RegionHeart::spice);
            if (heartSpice.isEmpty())
                return false;
        }

        Optional<Spice> originSpice = heartSpice.isPresent() ? heartSpice : resolveSpiceAt(level, originSurface);
        originSpice.flatMap(spice -> groundPond(context, spice, originSurface))
                .ifPresent(pond -> pond.value().place(level, context.chunkGenerator(), random, originSurface));

        SpiceTree tree = originSpice.map(SpiceTrees.getRegistered()::get).orElse(null);
        if (tree != null)
            return placeTrees(context, tree.getTreeFeature(), tree.getSapling().get(), originSurface);

        RegisteredSpiceVine vine = originSpice.map(SpiceVines.getRegistered()::get).orElse(null);
        if (vine != null)
            return placeTrees(context, vine.hostTreeFeature(), null, originSurface);

        if (originSpice.isEmpty())
            return false;

        boolean placedAny = false;
        for (int i = 0; i < config.tries(); i++) {
            BlockPos surfacePos = randomSurfacePos(level, random, context.origin(), config.xzSpread());
            if (tryPlaceOne(level, random, surfacePos, originSpice.get()))
                placedAny = true;
        }
        return placedAny;
    }

    // #region ground

    /**
     * Picks the pond to carve at the origin before anything else, so the
     * patch has something to grow in: the
     * {@link SpicePlantConfiguration#aquaticPond() aquatic pond} for an
     * Aquatic Spice, otherwise the biome's
     * {@link SpicePlantConfiguration#oasis() oasis} if too little of the
     * patch's ground is spice-growable (see {@link #lacksGrowableGround}).
     *
     * @return The pond to carve, or empty if none is needed or configured.
     */
    private static Optional<Holder<ConfiguredFeature<?, ?>>> groundPond(
            FeaturePlaceContext<SpicePlantConfiguration> context, Spice spice, BlockPos originSurface) {
        SpicePlantConfiguration config = context.config();
        if (spice.isAquatic())
            return config.aquaticPondFor(context.level().getBiome(originSurface));

        WorldGenLevel level = context.level();
        return config.oasis()
                .filter(oasis -> lacksGrowableGround(level, originSurface, config.xzSpread(),
                        oasis.minGrowableGround()))
                .flatMap(oasis -> oasis.pondFor(level.getBiome(originSurface)));
    }

    /**
     * Rates the ground of every column within {@code spread} blocks of
     * {@code origin}. Columns topped by fluid don't count either way, so a
     * patch next to a lake isn't mistaken for barren ground.
     *
     * @param minShare Share of dry columns that must be
     *                 {@link SpicePlantBlock#SPICE_GROWABLE}.
     * @return Whether fewer dry columns than {@code minShare} are
     *         spice-growable. {@code false} if no column is dry at all, since
     *         a pond couldn't be carved there anyway.
     */
    private static boolean lacksGrowableGround(WorldGenLevel level, BlockPos origin, int spread, float minShare) {
        int dry = 0;
        int growable = 0;
        BlockPos.MutableBlockPos column = new BlockPos.MutableBlockPos();
        for (int dx = -spread; dx <= spread; dx++) {
            for (int dz = -spread; dz <= spread; dz++) {
                column.set(origin.getX() + dx, origin.getY(), origin.getZ() + dz);
                BlockPos ground = level.getHeightmapPos(SurfaceHeightmaps.surface(level), column).below();
                if (!level.getFluidState(ground).isEmpty())
                    continue;

                dry++;
                if (level.getBlockState(ground).is(SpicePlantBlock.SPICE_GROWABLE))
                    growable++;
            }
        }
        return dry > 0 && growable < dry * minShare;
    }

    // #region trees

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
            boolean tooClose = trunks.stream()
                    .anyMatch(trunk -> horizontalDistanceSq(trunk, surfacePos) < minDistanceSq);
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
     * if {@code sapling} could survive at {@code surfacePos} and the trunk stands
     * on {@link #isSolidGround solid ground}.
     */
    private boolean tryPlaceTree(FeaturePlaceContext<SpicePlantConfiguration> context,
            ResourceKey<ConfiguredFeature<?, ?>> treeFeature, @Nullable Block sapling, BlockPos surfacePos) {
        WorldGenLevel level = context.level();
        if (!level.isEmptyBlock(surfacePos))
            return false;
        if (sapling != null && !sapling.defaultBlockState().canSurvive(level, surfacePos))
            return false;
        if (!isSolidGround(level, surfacePos, context.config().trees().groundRadius()))
            return false;

        return level.registryAccess().registryOrThrow(Registries.CONFIGURED_FEATURE)
                .getHolder(treeFeature)
                .map(feature -> feature.value().place(level, context.chunkGenerator(), context.random(), surfacePos))
                .orElse(false);
    }

    /**
     * Checks that {@code pos} isn't a lone patch of ground, like a single
     * dirt block in the middle of a lake or ocean: every column within
     * {@code radius} blocks must be topped by a non-fluid block, judged like
     * {@link #lacksGrowableGround} does.
     *
     * @param pos    The surface position a trunk would stand at.
     * @param radius Half-width of the square of columns to check.
     * @return Whether all those columns are dry.
     */
    private static boolean isSolidGround(WorldGenLevel level, BlockPos pos, int radius) {
        BlockPos.MutableBlockPos column = new BlockPos.MutableBlockPos();
        for (int dx = -radius; dx <= radius; dx++) {
            for (int dz = -radius; dz <= radius; dz++) {
                column.set(pos.getX() + dx, pos.getY(), pos.getZ() + dz);
                BlockPos ground = level.getHeightmapPos(SurfaceHeightmaps.surface(level), column).below();
                if (!level.getFluidState(ground).isEmpty())
                    return false;
            }
        }
        return true;
    }

    // #region plants

    /**
     * Places one {@code spice} plant at a random growth stage, if it can
     * survive at {@code surfacePos} - or, for an Aquatic Spice, waterlogged in
     * the water just below it (see {@link #findShallowWater}), displacing any
     * {@link AquaticSpiceRhizomeBlock#WATER_SURFACE_VEGETATION} above it.
     */
    private boolean tryPlaceOne(WorldGenLevel level, RandomSource random, BlockPos surfacePos, Spice spice) {
        SpicePlants.RegisteredSpicePlant plant = SpicePlants.getRegistered().get(spice);
        if (plant == null) {
            // Resolved Spice isn't a patch/bush/crop/rhizome plant (trees and
            // Host Trees are placed separately) - nothing to place.
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

        if (!block.canGenerateAt(defaultState, level, pos))
            return false;

        int age = random.nextInt(block.getMaxAge() + 1);
        BlockState state = defaultState.setValue(block.getAgeProperty(), age);
        if (block instanceof AquaticSpiceRhizomeBlock && level.getBlockState(pos.above())
                .is(AquaticSpiceRhizomeBlock.WATER_SURFACE_VEGETATION))
            level.setBlock(pos.above(), Blocks.AIR.defaultBlockState(), Block.UPDATE_CLIENTS);
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

    /**
     * @return The Spice the Spice Region resolves to at {@code pos}, using the
     *         climate found there.
     */
    private static Optional<Spice> resolveSpiceAt(WorldGenLevel level, BlockPos pos) {
        return SpiceRegionResolver
                .resolve(level.getSeed(), Services.CONFIG.getSpiceRegionSalt(),
                        Services.CONFIG.getSpiceRegionCellScale(),
                        Services.CONFIG.getSpiceRegionClusteringStrength(),
                        Climate.fromBiome(level.getBiome(pos), pos), pos.getX(), pos.getZ())
                .spice();
    }

    /**
     * @return The surface position of a random column within {@code spread} blocks
     *         of {@code origin}.
     */
    private static BlockPos randomSurfacePos(WorldGenLevel level, RandomSource random, BlockPos origin, int spread) {
        int dx = random.nextInt(spread * 2 + 1) - spread;
        int dz = random.nextInt(spread * 2 + 1) - spread;
        return level.getHeightmapPos(SurfaceHeightmaps.surface(level), origin.offset(dx, 0, dz));
    }
}
