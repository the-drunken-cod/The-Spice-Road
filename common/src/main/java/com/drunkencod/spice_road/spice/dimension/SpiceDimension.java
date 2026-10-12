package com.drunkencod.spice_road.spice.dimension;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;

import com.drunkencod.spice_road.spice.Climate;
import com.drunkencod.spice_road.spice.Spice;
import com.drunkencod.spice_road.spice.region.SeededHash;

/**
 * A dimension's Spice Region setup, as merged from all datapacks: which Spices
 * fill each of its Climate Buckets, how biomes get their Climate, how ground
 * is found and what its region layout looks like. Immutable.
 * <p>
 * A biome's Climate is resolved in layers: first the biome tag of each Climate
 * the dimension knows ({@link Climate#biomeTag()}), then the dimension's
 * {@linkplain ClimateRule rules} in order. A Climate is known to a dimension if
 * it has a bucket (even an empty one) or is the target of a rule there.
 */
public final class SpiceDimension {

    private final ResourceLocation id;
    private final Map<Climate, List<Spice>> buckets;
    private final List<ClimateRule> rules;
    private final SurfaceStrategy surface;
    private final Optional<Long> saltOffset;
    private final double cellScaleMultiplier;
    private final boolean allowPonds;
    /** Every Climate the dimension knows, in ID order. */
    private final List<Climate> knownClimates;

    /**
     * @param id                  The dimension's ID.
     * @param buckets             Spices per Climate, each sorted by Spice order.
     * @param rules               Fallback rules, in checking order.
     * @param surface             How ground is found.
     * @param saltOffset          Added to the global region salt, or empty for
     *                            the default (see {@link #salt}).
     * @param cellScaleMultiplier Multiplies the global region cell scale.
     * @param allowPonds          Whether Spice Ponds and Oases may be carved.
     */
    public SpiceDimension(ResourceLocation id, Map<Climate, List<Spice>> buckets, List<ClimateRule> rules,
            SurfaceStrategy surface, Optional<Long> saltOffset, double cellScaleMultiplier, boolean allowPonds) {
        this.id = id;
        this.buckets = buckets;
        this.rules = rules;
        this.surface = surface;
        this.saltOffset = saltOffset;
        this.cellScaleMultiplier = cellScaleMultiplier;
        this.allowPonds = allowPonds;

        Set<Climate> known = new TreeSet<>(Comparator.comparing(climate -> climate.id().toString()));
        known.addAll(buckets.keySet());
        rules.forEach(rule -> known.add(rule.climate()));
        this.knownClimates = List.copyOf(known);
    }

    /** @return The dimension's ID. */
    public ResourceLocation id() {
        return id;
    }

    /** @return How ground is found in this dimension. */
    public SurfaceStrategy surface() {
        return surface;
    }

    /** @return Whether Spice Ponds and Oases may be carved here. */
    public boolean allowsPonds() {
        return allowPonds;
    }

    /** @return Every Climate this dimension knows, in ID order. */
    public List<Climate> knownClimates() {
        return knownClimates;
    }

    /**
     * @param climate The Climate.
     * @return The Spices in {@code climate}'s bucket here, in Spice order;
     *         empty if the bucket is empty or missing.
     */
    public List<Spice> bucket(Climate climate) {
        return buckets.getOrDefault(climate, List.of());
    }

    /** @return Whether any Climate Bucket holds a Spice, i.e. whether Spice Regions can resolve to anything. */
    public boolean hasSpices() {
        return buckets.values().stream().anyMatch(bucket -> !bucket.isEmpty());
    }

    /**
     * Resolves a biome's Climate here.
     *
     * @param biome The biome.
     * @param pos   The position within it, as precipitation can vary with
     *              height.
     * @return The Climate, or empty if no tag and no rule gives one, which
     *         means the biome resolves to no Spice.
     */
    public Optional<Climate> climateOf(Holder<Biome> biome, BlockPos pos) {
        for (Climate climate : knownClimates) {
            if (biome.is(climate.biomeTag()))
                return Optional.of(climate);
        }
        for (ClimateRule rule : rules) {
            if (rule.matches(biome, pos))
                return Optional.of(rule.climate());
        }
        return Optional.empty();
    }

    /**
     * @param globalSalt The configured global region salt.
     * @return The salt this dimension's regions use: the global one plus this
     *         dimension's offset, which by default comes from its ID so layouts
     *         and rolls differ between dimensions, except in the Overworld,
     *         which keeps the global salt.
     */
    public long salt(long globalSalt) {
        return globalSalt + saltOffset.orElseGet(() -> defaultSaltOffset(id));
    }

    /**
     * @param globalScale The configured global region cell scale, in blocks.
     * @return This dimension's cell scale, in blocks.
     */
    public double cellScale(double globalScale) {
        return globalScale * cellScaleMultiplier;
    }

    /**
     * @param id A dimension's ID.
     * @return The salt offset a dimension without an explicit one gets: zero
     *         for the Overworld, otherwise a hash of its ID.
     */
    public static long defaultSaltOffset(ResourceLocation id) {
        return id.equals(Level.OVERWORLD.location()) ? 0L : SeededHash.hashString(id.toString());
    }

    /**
     * Merges the files of all datapacks into one dimension. Buckets accumulate
     * (add, then remove, per file), fallback rules of higher-priority packs
     * come first, and the other settings are won by the highest-priority file
     * that sets them. A file with {@code replace} drops everything below it.
     *
     * @param id     The dimension's ID.
     * @param layers The files, lowest priority first.
     * @return The merged dimension.
     */
    public static SpiceDimension merge(ResourceLocation id, List<SpiceDimensionLayer> layers) {
        Map<Climate, Set<Spice>> buckets = new LinkedHashMap<>();
        List<List<ClimateRule>> ruleLayers = new ArrayList<>();
        SurfaceStrategy surface = SurfaceStrategy.TOP;
        Optional<Long> saltOffset = Optional.empty();
        double cellScaleMultiplier = 1.0D;
        boolean allowPonds = true;

        for (SpiceDimensionLayer layer : layers) {
            if (layer.replace()) {
                buckets.clear();
                ruleLayers.clear();
                surface = SurfaceStrategy.TOP;
                saltOffset = Optional.empty();
                cellScaleMultiplier = 1.0D;
                allowPonds = true;
            }
            for (Map.Entry<Climate, SpiceDimensionLayer.BucketEdit> entry : layer.buckets().entrySet()) {
                Set<Spice> bucket = buckets.computeIfAbsent(entry.getKey(), climate -> new LinkedHashSet<>());
                bucket.addAll(entry.getValue().add());
                bucket.removeAll(entry.getValue().remove());
            }
            ruleLayers.add(layer.rules());
            surface = layer.surface().orElse(surface);
            saltOffset = layer.saltOffset().isPresent() ? layer.saltOffset() : saltOffset;
            cellScaleMultiplier = layer.cellScaleFactor().orElse(cellScaleMultiplier);
            allowPonds = layer.allowPonds().orElse(allowPonds);
        }

        Map<Climate, List<Spice>> sorted = new LinkedHashMap<>();
        buckets.forEach((climate, spices) -> sorted.put(climate,
                spices.stream().sorted(Comparator.comparingInt(Spice::ordinal)).toList()));

        List<ClimateRule> rules = new ArrayList<>();
        for (List<ClimateRule> ruleLayer : ruleLayers.reversed())
            rules.addAll(ruleLayer);

        return new SpiceDimension(id, Collections.unmodifiableMap(sorted), List.copyOf(rules), surface, saltOffset,
                cellScaleMultiplier, allowPonds);
    }
}
