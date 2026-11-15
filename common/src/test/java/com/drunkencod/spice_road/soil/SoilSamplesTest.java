package com.drunkencod.spice_road.soil;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;

class SoilSamplesTest {

    private static final ResourceKey<Level> OVERWORLD = ResourceKey.create(Registries.DIMENSION,
            ResourceLocation.withDefaultNamespace("overworld"));
    private static final ResourceKey<Level> NETHER = ResourceKey.create(Registries.DIMENSION,
            ResourceLocation.withDefaultNamespace("the_nether"));

    private static SoilSamples overworldAt(BlockPos... positions) {
        SoilSamples samples = SoilSamples.startedWith(OVERWORLD, positions[0]);
        for (int i = 1; i < positions.length; i++)
            samples = samples.with(positions[i]);
        return samples;
    }

    @Test
    void firstSampleIsAlwaysAccepted() {
        assertEquals(SoilSamples.Spacing.OK, SoilSamples.check(null, OVERWORLD, new BlockPos(5, 70, 5), 8, 64));
    }

    @Test
    void otherDimensionIsRefused() {
        SoilSamples samples = overworldAt(new BlockPos(0, 64, 0));
        assertEquals(SoilSamples.Spacing.OTHER_DIMENSION,
                SoilSamples.check(samples, NETHER, new BlockPos(20, 64, 20), 8, 64));
    }

    @Test
    void tooCloseToAnyEarlierSampleIsRefused() {
        SoilSamples samples = overworldAt(new BlockPos(0, 64, 0), new BlockPos(30, 64, 0));
        assertEquals(SoilSamples.Spacing.TOO_CLOSE,
                SoilSamples.check(samples, OVERWORLD, new BlockPos(33, 64, 4), 8, 64));
        assertEquals(SoilSamples.Spacing.OK,
                SoilSamples.check(samples, OVERWORLD, new BlockPos(15, 64, 20), 8, 64));
    }

    @Test
    void tooFarFromTheFirstSampleIsRefused() {
        SoilSamples samples = overworldAt(new BlockPos(0, 64, 0));
        assertEquals(SoilSamples.Spacing.TOO_FAR,
                SoilSamples.check(samples, OVERWORLD, new BlockPos(100, 64, 0), 8, 64));
        assertEquals(SoilSamples.Spacing.OK,
                SoilSamples.check(samples, OVERWORLD, new BlockPos(64, 64, 0), 8, 64));
    }

    @Test
    void heightDoesNotCountTowardsDistance() {
        SoilSamples samples = overworldAt(new BlockPos(0, 64, 0));
        assertEquals(SoilSamples.Spacing.TOO_CLOSE,
                SoilSamples.check(samples, OVERWORLD, new BlockPos(2, 200, 2), 8, 64));
    }

    @Test
    void zeroMinimumDistanceAllowsAnySpacing() {
        SoilSamples samples = overworldAt(new BlockPos(0, 64, 0));
        assertEquals(SoilSamples.Spacing.OK, SoilSamples.check(samples, OVERWORLD, new BlockPos(0, 64, 0), 0, 64));
    }

    @Test
    void centroidIsTheRoundedAverage() {
        SoilSamples samples = overworldAt(new BlockPos(0, 60, 0), new BlockPos(10, 64, 0), new BlockPos(5, 65, 9));
        assertEquals(new BlockPos(5, 63, 3), samples.centroid());
    }
}
