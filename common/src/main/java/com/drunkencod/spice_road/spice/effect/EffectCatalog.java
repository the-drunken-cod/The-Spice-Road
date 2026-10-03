package com.drunkencod.spice_road.spice.effect;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalInt;

import net.minecraft.resources.ResourceLocation;

import com.drunkencod.spice_road.spice.FlavorAxis;

/**
 * A read-only, pre-indexed view of the Seasoning Effect catalog: what the
 * Seasoning Board and the effect rules need to know about each entry, without
 * the mob effect it applies. Candidate lists are in entry-ID order, so picking
 * from them is deterministic.
 */
public final class EffectCatalog {

    /** The empty catalog. */
    public static final EffectCatalog EMPTY = new EffectCatalog(Map.of());

    private final Map<ResourceLocation, Entry> entries;
    private final Map<FlavorAxis, Map<Boolean, Map<EffectKind, List<ResourceLocation>>>> poles = new EnumMap<>(
            FlavorAxis.class);
    private final List<ResourceLocation> randomPool = new ArrayList<>();

    /**
     * What an {@link EffectCatalog} knows of one entry.
     *
     * @param kind         Whether the entry helps or hurts.
     * @param pole         The axis pole it is a boon or bane of, if any.
     * @param randomWeight Its weight in the vanilla-random pool, {@code 0} if it isn't in it.
     * @param maxLevel     The highest level it stacks to.
     */
    public record Entry(EffectKind kind, Optional<SeasoningEffectDef.Pole> pole, int randomWeight, int maxLevel) {
    }

    /**
     * @param entries Every entry by ID.
     */
    public EffectCatalog(Map<ResourceLocation, Entry> entries) {
        this.entries = Map.copyOf(entries);
        List<ResourceLocation> sorted = new ArrayList<>(entries.keySet());
        sorted.sort(Comparator.naturalOrder());
        for (ResourceLocation id : sorted) {
            Entry entry = entries.get(id);
            entry.pole().ifPresent(pole -> poles
                    .computeIfAbsent(pole.axis(), axis -> new HashMap<>())
                    .computeIfAbsent(pole.positive(), positive -> new EnumMap<>(EffectKind.class))
                    .computeIfAbsent(entry.kind(), kind -> new ArrayList<>())
                    .add(id));
            if (entry.randomWeight() > 0)
                randomPool.add(id);
        }
    }

    /**
     * @param definitions The catalog as loaded from the datapacks.
     * @return The view of {@code definitions}.
     */
    public static EffectCatalog of(Map<ResourceLocation, SeasoningEffectDef> definitions) {
        Map<ResourceLocation, Entry> entries = new HashMap<>();
        definitions.forEach((id, def) -> entries.put(id,
                new Entry(def.kind(), def.pole(), def.randomWeight(), def.maxLevel())));
        return new EffectCatalog(entries);
    }

    /**
     * @param axis     A Flavor Axis.
     * @param positive Which pole of {@code axis}.
     * @param kind     Boon or bane.
     * @return The IDs of the entries that are that pole's boons or banes, in ID order.
     */
    public List<ResourceLocation> poleEntries(FlavorAxis axis, boolean positive, EffectKind kind) {
        return poles.getOrDefault(axis, Map.of()).getOrDefault(positive, Map.of()).getOrDefault(kind, List.of());
    }

    /** @return The IDs of the vanilla-random pool's entries, in ID order. */
    public List<ResourceLocation> randomPool() {
        return randomPool;
    }

    /**
     * @param id An entry ID.
     * @return Its weight in the vanilla-random pool, {@code 0} if unknown or not in it.
     */
    public int randomWeight(ResourceLocation id) {
        Entry entry = entries.get(id);
        return entry == null ? 0 : entry.randomWeight();
    }

    /**
     * @param id An entry ID.
     * @return The highest level the entry stacks to, empty if it isn't in the catalog.
     */
    public OptionalInt maxLevel(ResourceLocation id) {
        Entry entry = entries.get(id);
        return entry == null ? OptionalInt.empty() : OptionalInt.of(entry.maxLevel());
    }

    /**
     * @param id An entry ID.
     * @return Whether the entry is a boon or bane, empty if it isn't in the catalog.
     */
    public Optional<EffectKind> kind(ResourceLocation id) {
        return Optional.ofNullable(entries.get(id)).map(Entry::kind);
    }
}
