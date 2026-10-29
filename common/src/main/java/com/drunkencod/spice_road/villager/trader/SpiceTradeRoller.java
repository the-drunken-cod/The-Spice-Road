package com.drunkencod.spice_road.villager.trader;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;
import java.util.function.UnaryOperator;

import org.jetbrains.annotations.Nullable;

import net.minecraft.util.RandomSource;
import net.minecraft.world.item.trading.MerchantOffer;

/**
 * Rolls one offer from a list of catalog entries: it picks an entry by weight,
 * then a good from the entry's source, and prices it from the config. Entries
 * whose source has no qualifying good right now are passed over.
 */
final class SpiceTradeRoller {

    private SpiceTradeRoller() {
    }

    /**
     * @param entries   The weighted choices.
     * @param direction Which way the offer goes.
     * @param random    The random source to roll with.
     * @param adjust    Applied to each good before it is priced, e.g. to scale
     *                  its value.
     * @param allowed   Which goods may be picked at all, e.g. to leave out
     *                  ones the trader already offers.
     * @return The offer, or {@code null} if no entry holds an allowed good
     *         right now.
     */
    static @Nullable MerchantOffer roll(List<SpiceTradeEntry> entries, SpiceTradeDirection direction,
            RandomSource random, UnaryOperator<TradeGood> adjust, Predicate<TradeGood> allowed) {
        List<SpiceTradeEntry> usable = new ArrayList<>();
        List<List<TradeGood>> goods = new ArrayList<>();
        int totalWeight = 0;
        for (SpiceTradeEntry entry : entries) {
            List<TradeGood> available = entry.source().goods(direction).stream().filter(allowed).toList();
            if (available.isEmpty())
                continue;
            usable.add(entry);
            goods.add(available);
            totalWeight += entry.weight();
        }
        if (usable.isEmpty())
            return null;
        int roll = random.nextInt(totalWeight);
        for (int i = 0; i < usable.size(); i++) {
            roll -= usable.get(i).weight();
            if (roll < 0) {
                List<TradeGood> pool = goods.get(i);
                TradeGood good = adjust.apply(pool.get(random.nextInt(pool.size())));
                return SpiceTradePricing.offerFor(good, direction, usable.get(i).uses(), usable.get(i).xp());
            }
        }
        return null;
    }
}
