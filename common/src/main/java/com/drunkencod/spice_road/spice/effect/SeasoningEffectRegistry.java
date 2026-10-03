package com.drunkencod.spice_road.spice.effect;

import java.util.Map;
import java.util.Optional;

import net.minecraft.resources.ResourceLocation;

/**
 * The Seasoning Effect catalog: every {@link SeasoningEffectDef} by its entry
 * ID. On the logical server, {@link SeasoningEffectReloadListener} fills it
 * from the datapacks; the client receives the same map through
 * {@link SeasoningEffectSync}.
 */
public final class SeasoningEffectRegistry {

    private static volatile Map<ResourceLocation, SeasoningEffectDef> ENTRIES = Map.of();

    private SeasoningEffectRegistry() {
    }

    /**
     * Replaces the catalog, on the server after a reload and on the client
     * after a sync.
     *
     * @param entries The new catalog, keyed by entry ID.
     */
    public static void set(Map<ResourceLocation, SeasoningEffectDef> entries) {
        ENTRIES = Map.copyOf(entries);
    }

    /**
     * @param id An entry ID.
     * @return The entry, if the catalog has it.
     */
    public static Optional<SeasoningEffectDef> get(ResourceLocation id) {
        return Optional.ofNullable(ENTRIES.get(id));
    }

    /** @return Every catalog entry, keyed by entry ID. */
    public static Map<ResourceLocation, SeasoningEffectDef> getAll() {
        return ENTRIES;
    }
}
