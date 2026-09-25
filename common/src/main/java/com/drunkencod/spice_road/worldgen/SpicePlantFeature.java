package com.drunkencod.spice_road.worldgen;

import java.util.Optional;

import com.mojang.serialization.Codec;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;
import net.minecraft.world.level.biome.Biome;

import com.drunkencod.spice_road.block.SpicePlantBlock;
import com.drunkencod.spice_road.block.SpicePlants;
import com.drunkencod.spice_road.platform.Services;
import com.drunkencod.spice_road.spice.Climate;
import com.drunkencod.spice_road.spice.Spice;
import com.drunkencod.spice_road.spice.region.SpiceRegionResolver;

/**
 * Places a small patch of one {@link Spice}'s plant block, chosen by
 * {@link SpiceRegionResolver} rather than a fixed per-biome roster.
 * <p>
 * {@link SpiceRegionResolver} always resolves to <i>some</i>
 * Spice for any populated Climate Bucket, so frequency must come from the
 * placement modifiers, not from this class. This class only decides
 * <b>which</b> Spice appears, consistently within a Spice Region.
 * <p>
 * Only {@code FLOWER_PATCH}/{@code CROP} Spices can actually place anything
 * (the only Source Types with a registered block so far).
 */
public class SpicePlantFeature extends Feature<NoneFeatureConfiguration> {

    /**
     * Placement attempts per feature invocation, spread around the chosen origin -
     * a small "patch", like vanilla's flower/patch features.
     */
    private static final int PATCH_TRIES = 6;

    /** Horizontal spread, in blocks, of patch attempts around the origin. */
    private static final int PATCH_RADIUS = 3;

    public SpicePlantFeature(Codec<NoneFeatureConfiguration> codec) {
        super(codec);
    }

    @Override
    public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> context) {

        WorldGenLevel level = context.level();
        RandomSource random = context.random();
        BlockPos origin = context.origin();
        long worldSeed = level.getSeed();

        double cellScale = Services.CONFIG.getSpiceRegionCellScale();
        double clusteringStrength = Services.CONFIG.getSpiceRegionClusteringStrength();

        boolean placedAny = false;
        for (int i = 0; i < PATCH_TRIES; i++) {
            int dx = random.nextInt(PATCH_RADIUS * 2 + 1) - PATCH_RADIUS;
            int dz = random.nextInt(PATCH_RADIUS * 2 + 1) - PATCH_RADIUS;
            BlockPos columnPos = origin.offset(dx, 0, dz);
            BlockPos surfacePos = level.getHeightmapPos(Heightmap.Types.WORLD_SURFACE_WG, columnPos);

            if (tryPlaceOne(level, random, surfacePos, worldSeed, cellScale, clusteringStrength)) {
                placedAny = true;
            }
        }
        return placedAny;
    }

    private boolean tryPlaceOne(WorldGenLevel level, RandomSource random, BlockPos surfacePos, long worldSeed,
            double cellScale, double clusteringStrength) {

        Holder<Biome> biome = level.getBiome(surfacePos);
        Climate climate = Climate.fromBiome(biome, surfacePos);

        Optional<Spice> resolved = SpiceRegionResolver
                .resolve(worldSeed, cellScale, clusteringStrength, climate, surfacePos.getX(), surfacePos.getZ())
                .spice();
        if (resolved.isEmpty()) {
            return false;
        }

        SpicePlants.RegisteredSpicePlant plant = SpicePlants.getRegistered().get(resolved.get());
        if (plant == null) {
            // Resolved Spice has a Source Type without a registered block yet
            // (TREE/BUSH/VINE/RHIZOME) - nothing to place.
            return false;
        }

        SpicePlantBlock block = plant.block().get();
        if (!level.isEmptyBlock(surfacePos)) {
            return false;
        }
        BlockState defaultState = block.defaultBlockState();
        if (!defaultState.canSurvive(level, surfacePos)) {
            return false;
        }

        int age = random.nextInt(block.getMaxAge() + 1);
        BlockState state = defaultState.setValue(block.getAgeProperty(), age);
        return level.setBlock(surfacePos, state, Block.UPDATE_CLIENTS);
    }
}
