package com.drunkencod.spice_road.villager.trader;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;
import java.util.function.UnaryOperator;

import org.jetbrains.annotations.Nullable;

import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.npc.AbstractVillager;
import net.minecraft.world.entity.npc.VillagerTrades;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraft.world.item.trading.MerchantOffers;

import com.drunkencod.spice_road.platform.Services;

/**
 * One of the vanilla wandering trader's optional Spice offers, added to its
 * generic offer pool as many times as the config says. When vanilla draws one
 * of its entries, it makes its offer only if the feature is enabled and the
 * pool's configured chance comes up; otherwise it returns {@code null}, which
 * vanilla skips and replaces with another draw, so a miss never costs the
 * trader an offer. Several entries can each make an offer, but the trader
 * never ends up offering the same good twice.
 */
public class WanderingTraderListing implements VillagerTrades.ItemListing {

    private final WanderingTradePool pool;

    /** @param pool Which of the wandering trader's offers this is. */
    public WanderingTraderListing(WanderingTradePool pool) {
        this.pool = pool;
    }

    /**
     * @return Each {@link WanderingTradePool}'s listing, repeated as many times
     *         as its configured number of entries.
     */
    public static List<VillagerTrades.ItemListing> all() {
        List<VillagerTrades.ItemListing> listings = new ArrayList<>();
        for (WanderingTradePool pool : WanderingTradePool.values()) {
            VillagerTrades.ItemListing listing = new WanderingTraderListing(pool);
            for (int i = 0; i < pool.getEntries(); i++)
                listings.add(listing);
        }
        return listings;
    }

    @Override
    public @Nullable MerchantOffer getOffer(Entity trader, RandomSource random) {
        if (!Services.CONFIG.isWanderingTraderEnabled() || random.nextDouble() >= pool.getChance())
            return null;
        MerchantOffers offered = trader instanceof AbstractVillager villager ? villager.getOffers() : null;
        Predicate<TradeGood> notOffered = good -> offered == null || offered.stream()
                .noneMatch(offer -> ItemStack.isSameItemSameComponents(offer.getResult(), good.unit()));
        return SpiceTradeRoller.roll(WanderingTradeCatalog.entriesFor(pool), SpiceTradeDirection.SELL, random,
                pool == WanderingTradePool.MIXES ? WanderingTraderListing::markUp : UnaryOperator.identity(),
                notOffered);
    }

    /**
     * @param good A Spice Mix, valued at the Spice Trader's rates.
     * @return The same good, valued with the wandering trader's Spice Mix
     *         price multiplier on top.
     */
    private static TradeGood markUp(TradeGood good) {
        return new TradeGood(good.unit(),
                good.emeraldValue() * Services.CONFIG.getWanderingTraderMixPriceMultiplier(), good.divisible());
    }
}
