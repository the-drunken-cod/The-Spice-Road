package com.drunkencod.spice_road.villager.trader;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Every {@link SpiceTradeEntry} by villager level and direction, filled from the
 * datapacks by {@link SpiceTradeReloadListener}. Server only: offers are rolled
 * there.
 */
public final class SpiceTradeCatalog {

    private record Key(int level, SpiceTradeDirection direction) {
    }

    private static volatile Map<Key, List<SpiceTradeEntry>> ENTRIES = Map.of();

    private SpiceTradeCatalog() {
    }

    /**
     * Replaces the catalog, merging files of the same level and direction in
     * the order given.
     *
     * @param trades The catalog files.
     */
    public static void set(List<SpiceTrade> trades) {
        Map<Key, List<SpiceTradeEntry>> merged = new HashMap<>();
        for (SpiceTrade trade : trades)
            merged.computeIfAbsent(new Key(trade.level(), trade.direction()), key -> new ArrayList<>())
                    .addAll(trade.entries());
        merged.replaceAll((key, entries) -> List.copyOf(entries));
        ENTRIES = Map.copyOf(merged);
    }

    /**
     * @param level     A villager level, 1 to 5.
     * @param direction The direction of the offer.
     * @return The entries its slot picks from; empty if the catalog has none.
     */
    public static List<SpiceTradeEntry> entriesFor(int level, SpiceTradeDirection direction) {
        return ENTRIES.getOrDefault(new Key(level, direction), List.of());
    }
}
