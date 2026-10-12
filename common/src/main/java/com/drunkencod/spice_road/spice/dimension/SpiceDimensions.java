package com.drunkencod.spice_road.spice.dimension;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;

import com.drunkencod.spice_road.spice.Climate;

/**
 * The loaded {@link SpiceDimension}s by dimension ID. {@link
 * SpiceDimensionReloadListener} fills it from the datapacks. Worldgen threads
 * read it while a reload may replace it, so it only ever holds a complete,
 * immutable snapshot.
 */
public final class SpiceDimensions {

    private static volatile Map<ResourceLocation, SpiceDimension> dimensions = Map.of();

    private SpiceDimensions() {
    }

    /**
     * Replaces everything loaded.
     *
     * @param loaded The new dimensions.
     */
    public static void set(Map<ResourceLocation, SpiceDimension> loaded) {
        dimensions = Map.copyOf(loaded);
    }

    /**
     * @param id A dimension's ID.
     * @return Its Spice Dimension, or empty if no datapack has one, in which
     *         case nothing in it resolves to a Spice.
     */
    public static Optional<SpiceDimension> get(ResourceLocation id) {
        return Optional.ofNullable(dimensions.get(id));
    }

    /**
     * @param level A level.
     * @return The Spice Dimension of {@code level}'s dimension, if any.
     */
    public static Optional<SpiceDimension> get(Level level) {
        return get(level.dimension().location());
    }

    /**
     * @return Every Climate this mod ships or any loaded dimension knows, without
     *         repeats, for command suggestions.
     */
    public static List<Climate> allClimates() {
        Set<Climate> climates = new LinkedHashSet<>(Climate.BUILT_IN);
        dimensions.values().forEach(dimension -> climates.addAll(dimension.knownClimates()));
        return List.copyOf(climates);
    }
}
