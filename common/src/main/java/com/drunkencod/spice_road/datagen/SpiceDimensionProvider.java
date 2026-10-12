package com.drunkencod.spice_road.datagen;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import com.google.gson.JsonElement;
import com.mojang.serialization.JsonOps;

import net.minecraft.data.CachedOutput;
import net.minecraft.data.DataProvider;
import net.minecraft.data.PackOutput;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;

import com.drunkencod.spice_road.spice.Climate;
import com.drunkencod.spice_road.spice.Spice;
import com.drunkencod.spice_road.spice.dimension.BiomeMatcher;
import com.drunkencod.spice_road.spice.dimension.ClimateRule;
import com.drunkencod.spice_road.spice.dimension.SpiceDimensionLayer;
import com.drunkencod.spice_road.spice.dimension.SpiceDimensionLayer.BucketEdit;
import com.drunkencod.spice_road.spice.dimension.SpiceDimensionReloadListener;
import com.drunkencod.spice_road.spice.dimension.SurfaceStrategy;

/**
 * Writes the mod's own Spice Dimensions as
 * {@code data/minecraft/spice_dimension/*.json}:
 * <ul>
 * <li>The Overworld, whose four Climates hold the Spices that name them as
 * their {@link Spice#getClimate() own}, and whose rules sort every biome into
 * one by base temperature and precipitation.</li>
 * <li>The Nether and the End, as a scaffold: one empty Climate Bucket each,
 * ground found by scanning down and no ponds in the Nether (water vaporizes
 * there). They resolve to no Spice until a datapack fills a bucket.</li>
 * </ul>
 * Raw JSON written through the real codec, so it runs unchanged on both
 * loaders and datagen fails on data the game would reject.
 */
public class SpiceDimensionProvider implements DataProvider {

    /** Below this base temperature, a biome is cold. */
    private static final float COLD_BELOW = 0.15F;

    /**
     * Up to this base temperature, a biome that isn't cold or arid is temperate,
     * above it tropical.
     */
    private static final float TEMPERATE_UP_TO = 0.8F;

    /** Where scanning the Nether starts, just below its bedrock roof. */
    private static final int NETHER_START_Y = 120;

    /** Where scanning the End starts, above its tallest islands. */
    private static final int END_START_Y = 100;

    private final PackOutput.PathProvider pathProvider;

    /** @param output The pack output to write into. */
    public SpiceDimensionProvider(PackOutput output) {
        this.pathProvider = output.createPathProvider(PackOutput.Target.DATA_PACK,
                SpiceDimensionReloadListener.DIRECTORY);
    }

    @Override
    public CompletableFuture<?> run(CachedOutput cachedOutput) {
        Map<ResourceLocation, SpiceDimensionLayer> dimensions = new LinkedHashMap<>();
        dimensions.put(Level.OVERWORLD.location(), overworld());
        dimensions.put(Level.NETHER.location(), scaffold(Climate.NETHER,
                new SurfaceStrategy(SurfaceStrategy.Type.SCAN_DOWN, NETHER_START_Y), false));
        dimensions.put(Level.END.location(), scaffold(Climate.END,
                new SurfaceStrategy(SurfaceStrategy.Type.SCAN_DOWN, END_START_Y), true));

        List<CompletableFuture<?>> writes = new ArrayList<>();
        dimensions.forEach((id, layer) -> {
            JsonElement json = SpiceDimensionLayer.CODEC.encodeStart(JsonOps.INSTANCE, layer).getOrThrow(
                    error -> new IllegalStateException("Invalid Spice Dimension " + id + ": " + error));
            writes.add(DataProvider.saveStable(cachedOutput, json, pathProvider.json(id)));
        });
        return CompletableFuture.allOf(writes.toArray(CompletableFuture[]::new));
    }

    /** @return The Overworld, with today's four Climates and their heuristic. */
    private static SpiceDimensionLayer overworld() {
        List<Climate> climates = List.of(Climate.TROPICAL, Climate.TEMPERATE, Climate.ARID, Climate.COLD);
        Map<Climate, BucketEdit> buckets = new LinkedHashMap<>();
        for (Climate climate : climates) {
            List<Spice> spices = Arrays.stream(Spice.values()).filter(spice -> spice.getClimate().equals(climate))
                    .toList();
            buckets.put(climate, new BucketEdit(spices, List.of()));
        }

        // First match wins: cold, then no precipitation, then temperate, and tropical
        // as the catch-all
        List<ClimateRule> rules = List.of(
                rule(Climate.COLD, new ClimateRule.TemperatureRange(Optional.empty(), Optional.empty(),
                        Optional.empty(), Optional.of(COLD_BELOW)), Optional.empty()),
                rule(Climate.ARID, ClimateRule.TemperatureRange.ANY, Optional.of(ClimateRule.Precipitation.NONE)),
                rule(Climate.TEMPERATE, new ClimateRule.TemperatureRange(Optional.empty(),
                        Optional.of(TEMPERATE_UP_TO), Optional.empty(), Optional.empty()), Optional.empty()),
                rule(Climate.TROPICAL, ClimateRule.TemperatureRange.ANY, Optional.empty()));

        return new SpiceDimensionLayer(false, buckets, rules, Optional.empty(), Optional.empty(), Optional.empty(),
                Optional.empty());
    }

    /**
     * @param climate    The dimension's only Climate, whose bucket is empty.
     * @param surface    How its ground is found.
     * @param allowPonds Whether ponds may be carved.
     * @return A dimension that resolves to no Spice until a datapack fills the
     *         bucket. Biomes
     *         reach the Climate through its biome tag, written by hand.
     */
    private static SpiceDimensionLayer scaffold(Climate climate, SurfaceStrategy surface, boolean allowPonds) {
        return new SpiceDimensionLayer(false, Map.of(climate, new BucketEdit(List.of(), List.of())), List.of(),
                Optional.of(surface), Optional.empty(), Optional.empty(), Optional.of(allowPonds));
    }

    private static ClimateRule rule(Climate climate, ClimateRule.TemperatureRange temperature,
            Optional<ClimateRule.Precipitation> precipitation) {
        return new ClimateRule(climate, List.<BiomeMatcher>of(), temperature, precipitation);
    }

    @Override
    public String getName() {
        return "Spice Dimensions";
    }
}
