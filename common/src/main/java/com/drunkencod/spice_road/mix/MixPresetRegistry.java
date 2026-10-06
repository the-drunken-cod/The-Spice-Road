package com.drunkencod.spice_road.mix;

import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

import net.minecraft.world.item.Item;
import net.minecraft.resources.ResourceLocation;

/**
 * Every {@link MixPreset} by its ID. On the logical server,
 * {@link MixPresetReloadListener} fills it from the datapacks; the client
 * receives the same map through {@link MixPresetSync}.
 */
public final class MixPresetRegistry {

    private static volatile Map<ResourceLocation, MixPreset> PRESETS = Map.of();

    private MixPresetRegistry() {
    }

    /**
     * Replaces every preset, on the server after a reload and on the client
     * after a sync. They are kept in ID order, so the first matching preset is
     * always the same one.
     *
     * @param presets The new presets, keyed by ID.
     */
    public static void set(Map<ResourceLocation, MixPreset> presets) {
        Map<ResourceLocation, MixPreset> sorted = new LinkedHashMap<>();
        presets.entrySet().stream()
                .sorted(Map.Entry.comparingByKey(Comparator.comparing(ResourceLocation::toString)))
                .forEach(entry -> sorted.put(entry.getKey(), entry.getValue()));
        PRESETS = Collections.unmodifiableMap(sorted);
    }

    /**
     * @param id A preset ID.
     * @return The preset, if there is one.
     */
    public static Optional<MixPreset> get(ResourceLocation id) {
        return Optional.ofNullable(PRESETS.get(id));
    }

    /** @return Every preset, keyed by ID, in ID order. */
    public static Map<ResourceLocation, MixPreset> getAll() {
        return PRESETS;
    }

    /**
     * @param counts How many of each spice a grid holds.
     * @return The ID of the first preset (in ID order) whose proportions
     *         {@code counts} is a multiple of, if any.
     */
    public static Optional<ResourceLocation> match(Map<Item, Integer> counts) {
        for (Map.Entry<ResourceLocation, MixPreset> entry : PRESETS.entrySet()) {
            if (entry.getValue().multipleOf(counts) > 0)
                return Optional.of(entry.getKey());
        }
        return Optional.empty();
    }
}
