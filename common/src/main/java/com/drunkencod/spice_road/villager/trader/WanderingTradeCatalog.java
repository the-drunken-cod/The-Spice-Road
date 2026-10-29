package com.drunkencod.spice_road.villager.trader;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * Every {@link SpiceTradeEntry} of the wandering trader by pool, filled from
 * the datapacks by {@link WanderingTradeReloadListener}. Server only: offers
 * are rolled there.
 */
public final class WanderingTradeCatalog {

    private static volatile Map<WanderingTradePool, List<SpiceTradeEntry>> ENTRIES = Map.of();

    private WanderingTradeCatalog() {
    }

    /**
     * Replaces the catalog, merging files of the same pool in the order given.
     *
     * @param trades The catalog files.
     */
    public static void set(List<WanderingTrade> trades) {
        Map<WanderingTradePool, List<SpiceTradeEntry>> merged = new EnumMap<>(WanderingTradePool.class);
        for (WanderingTrade trade : trades)
            merged.computeIfAbsent(trade.pool(), pool -> new ArrayList<>()).addAll(trade.entries());
        merged.replaceAll((pool, entries) -> List.copyOf(entries));
        ENTRIES = Map.copyOf(merged);
    }

    /**
     * @param pool One of the wandering trader's offers.
     * @return The entries it picks from; empty if the catalog has none.
     */
    public static List<SpiceTradeEntry> entriesFor(WanderingTradePool pool) {
        return ENTRIES.getOrDefault(pool, List.of());
    }
}
