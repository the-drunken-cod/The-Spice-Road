package com.drunkencod.spice_road.spice.dimension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.mojang.serialization.JsonOps;

import net.minecraft.SharedConstants;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

import com.drunkencod.spice_road.spice.Climate;
import com.drunkencod.spice_road.spice.Spice;
import com.drunkencod.spice_road.spice.dimension.SpiceDimensionLayer.BucketEdit;

class SpiceDimensionTest {

    private static final ResourceLocation NETHER = Level.NETHER.location();

    @BeforeAll
    static void bootstrap() {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
    }

    private static SpiceDimensionLayer layer(boolean replace, Map<Climate, BucketEdit> buckets,
            List<ClimateRule> rules) {
        return new SpiceDimensionLayer(replace, buckets, rules, Optional.empty(), Optional.empty(), Optional.empty(),
                Optional.empty());
    }

    private static SpiceDimensionLayer parse(String json) {
        JsonElement element = JsonParser.parseString(json);
        return SpiceDimensionLayer.CODEC.parse(JsonOps.INSTANCE, element).getOrThrow();
    }

    // #region Merging

    @Test
    void bucketsAccumulateAcrossLayersAndRemoveWinsPerLayer() {
        SpiceDimensionLayer low = layer(false,
                Map.of(Climate.ARID, new BucketEdit(List.of(Spice.THYME, Spice.SAGE), List.of())), List.of());
        SpiceDimensionLayer high = layer(false,
                Map.of(Climate.ARID, new BucketEdit(List.of(Spice.CAPER), List.of(Spice.SAGE))), List.of());

        SpiceDimension merged = SpiceDimension.merge(NETHER, List.of(low, high));

        assertEquals(List.of(Spice.CAPER, Spice.THYME), merged.bucket(Climate.ARID));
        assertTrue(merged.hasSpices());
    }

    @Test
    void replaceDropsLowerLayers() {
        SpiceDimensionLayer low = layer(false,
                Map.of(Climate.ARID, new BucketEdit(List.of(Spice.THYME), List.of())), List.of());
        SpiceDimensionLayer high = layer(true, Map.of(Climate.COLD, new BucketEdit(List.of(), List.of())),
                List.of());

        SpiceDimension merged = SpiceDimension.merge(NETHER, List.of(low, high));

        assertTrue(merged.bucket(Climate.ARID).isEmpty());
        assertFalse(merged.hasSpices());
        assertEquals(List.of(Climate.COLD), merged.knownClimates());
    }

    @Test
    void emptyBucketStillMakesTheClimateKnown() {
        SpiceDimension merged = SpiceDimension.merge(NETHER,
                List.of(layer(false, Map.of(Climate.NETHER, new BucketEdit(List.of(), List.of())), List.of())));

        assertEquals(List.of(Climate.NETHER), merged.knownClimates());
        assertFalse(merged.hasSpices());
    }

    @Test
    void higherLayersWinScalarsAndKeepLowerOnesWhenUnset() {
        SpiceDimensionLayer low = new SpiceDimensionLayer(false, Map.of(), List.of(),
                Optional.of(new SurfaceStrategy(SurfaceStrategy.Type.SCAN_DOWN, 90)), Optional.of(5L),
                Optional.of(2.0D), Optional.of(false));
        SpiceDimensionLayer high = new SpiceDimensionLayer(false, Map.of(), List.of(), Optional.empty(),
                Optional.empty(), Optional.of(3.0D), Optional.empty());

        SpiceDimension merged = SpiceDimension.merge(NETHER, List.of(low, high));

        assertEquals(90, merged.surface().startY());
        assertTrue(merged.surface().isScanDown());
        assertEquals(105L, merged.salt(100L));
        assertEquals(30.0D, merged.cellScale(10.0D));
        assertFalse(merged.allowsPonds());
    }

    @Test
    void defaultSaltOffsetIsZeroInTheOverworldAndDistinctElsewhere() {
        SpiceDimension overworld = SpiceDimension.merge(Level.OVERWORLD.location(), List.of());
        SpiceDimension nether = SpiceDimension.merge(NETHER, List.of());
        SpiceDimension end = SpiceDimension.merge(Level.END.location(), List.of());

        assertEquals(100L, overworld.salt(100L));
        assertFalse(nether.salt(100L) == 100L);
        assertFalse(nether.salt(100L) == end.salt(100L));
    }

    @Test
    void rulesOfHigherLayersComeFirstAndKeepTheirOrder() {
        ClimateRule coldRule = new ClimateRule(Climate.COLD, List.of(), ClimateRule.TemperatureRange.ANY,
                Optional.empty());
        ClimateRule aridRule = new ClimateRule(Climate.ARID, List.of(), ClimateRule.TemperatureRange.ANY,
                Optional.empty());
        ClimateRule endRule = new ClimateRule(Climate.END, List.of(), ClimateRule.TemperatureRange.ANY,
                Optional.empty());

        SpiceDimension merged = SpiceDimension.merge(NETHER, List.of(
                layer(false, Map.of(), List.of(coldRule)), layer(false, Map.of(), List.of(aridRule, endRule))));

        assertEquals(List.of(Climate.ARID, Climate.COLD, Climate.END), merged.knownClimates());
    }

    // #endregion

    // #region Codecs

    @Test
    void layerCodecAcceptsShorthandBucketsAndSingleBiomeStrings() {
        SpiceDimensionLayer layer = parse("""
                {
                  "buckets": {
                    "spice_road:nether": ["thyme"],
                    "mypack:swamp": { "add": ["basil"], "remove": ["thyme"] }
                  },
                  "rules": [
                    { "climate": "mypack:swamp", "biomes": "#minecraft:is_nether", "temperature": { "min": 0.5 } }
                  ],
                  "surface": { "type": "scan_down", "start_y": 64 },
                  "allow_ponds": false,
                  "cell_scale_multiplier": 0.5
                }
                """);

        assertEquals(List.of(Spice.THYME), layer.buckets().get(Climate.NETHER).add());
        assertEquals(List.of(Spice.THYME),
                layer.buckets().get(new Climate(ResourceLocation.fromNamespaceAndPath("mypack", "swamp"))).remove());
        assertEquals(List.of(new BiomeMatcher(ResourceLocation.withDefaultNamespace("is_nether"), true)),
                layer.rules().get(0).biomes());
        assertEquals(64, layer.surface().orElseThrow().startY());
        assertEquals(Optional.of(false), layer.allowPonds());
        assertEquals(Optional.of(0.5D), layer.cellScaleFactor());
    }

    @Test
    void layerCodecRejectsOutOfRangeCellScale() {
        JsonElement element = JsonParser.parseString("{ \"cell_scale_multiplier\": 0 }");

        assertTrue(SpiceDimensionLayer.CODEC.parse(JsonOps.INSTANCE, element).error().isPresent());
    }

    @Test
    void layerCodecRoundTrips() {
        SpiceDimensionLayer layer = new SpiceDimensionLayer(true,
                Map.of(Climate.ARID, new BucketEdit(List.of(Spice.THYME), List.of(Spice.SAGE))),
                List.of(new ClimateRule(Climate.COLD, List.of(), new ClimateRule.TemperatureRange(Optional.empty(),
                        Optional.empty(), Optional.empty(), Optional.of(0.15F)),
                        Optional.of(ClimateRule.Precipitation.NONE))),
                Optional.of(new SurfaceStrategy(SurfaceStrategy.Type.SCAN_DOWN, 100)), Optional.of(7L),
                Optional.of(2.0D), Optional.of(false));

        JsonElement json = SpiceDimensionLayer.CODEC.encodeStart(JsonOps.INSTANCE, layer).getOrThrow();

        assertEquals(layer, SpiceDimensionLayer.CODEC.parse(JsonOps.INSTANCE, json).getOrThrow());
    }

    @Test
    void temperatureRangeBoundsAreInclusiveForMinMaxAndExclusiveForAboveBelow() {
        ClimateRule.TemperatureRange inclusive = new ClimateRule.TemperatureRange(Optional.of(0.2F),
                Optional.of(0.8F), Optional.empty(), Optional.empty());
        ClimateRule.TemperatureRange exclusive = new ClimateRule.TemperatureRange(Optional.empty(),
                Optional.empty(), Optional.of(0.2F), Optional.of(0.8F));

        assertTrue(inclusive.contains(0.2F) && inclusive.contains(0.8F));
        assertFalse(exclusive.contains(0.2F) || exclusive.contains(0.8F));
        assertTrue(exclusive.contains(0.5F));
        assertTrue(ClimateRule.TemperatureRange.ANY.contains(-5F));
    }

    // #endregion

    // #region Surface scanning

    /** @return Blocks of a column where everything at or below {@code solidTop} is stone and above it air. */
    private static java.util.function.IntFunction<BlockState> column(int solidTop) {
        return y -> y <= solidTop ? Blocks.STONE.defaultBlockState() : Blocks.AIR.defaultBlockState();
    }

    @Test
    void scanDownFindsTheFirstOpenFloor() {
        Optional<SurfaceStrategy.Ground> ground = SurfaceStrategy.scanDown(column(70), 120, 0);

        assertEquals(71, ground.orElseThrow().floorY());
        assertTrue(ground.orElseThrow().isDry());
    }

    @Test
    void scanDownSkipsCeilingsAndFindsNothingInVoid() {
        // A ceiling from 100 up to the start and an open cave beneath it: the floor of the cave is found
        java.util.function.IntFunction<BlockState> cave = y -> y >= 100 || y <= 40 ? Blocks.STONE.defaultBlockState()
                : Blocks.AIR.defaultBlockState();
        assertEquals(41, SurfaceStrategy.scanDown(cave, 120, 0).orElseThrow().floorY());

        assertTrue(SurfaceStrategy.scanDown(y -> Blocks.AIR.defaultBlockState(), 120, 0).isEmpty());
    }

    @Test
    void scanDownTreatsWaterAsSurfaceAboveTheFloor() {
        java.util.function.IntFunction<BlockState> pond = y -> y <= 60 ? Blocks.STONE.defaultBlockState()
                : y <= 63 ? Blocks.WATER.defaultBlockState() : Blocks.AIR.defaultBlockState();

        SurfaceStrategy.Ground ground = SurfaceStrategy.scanDown(pond, 120, 0).orElseThrow();

        assertEquals(61, ground.floorY());
        assertEquals(64, ground.surfaceY());
        assertFalse(ground.isDry());
    }

    @Test
    void scanDownIgnoresBedrockFloors() {
        assertTrue(SurfaceStrategy.scanDown(y -> y <= 5 ? Blocks.BEDROCK.defaultBlockState()
                : Blocks.AIR.defaultBlockState(), 120, 0).isEmpty());
    }

    // #endregion
}
