package com.drunkencod.spice_road.villager.trader;

import java.util.Map;
import java.util.Optional;

import org.jetbrains.annotations.Nullable;

import net.minecraft.util.Mth;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.trading.ItemCost;
import net.minecraft.world.item.trading.MerchantOffer;

import com.drunkencod.spice_road.mix.SpiceMix;
import com.drunkencod.spice_road.mix.SpiceMixes;
import com.drunkencod.spice_road.platform.Services;
import com.drunkencod.spice_road.registry.ModDataComponents;
import com.drunkencod.spice_road.spice.Tier;

/**
 * Turns a {@link TradeGood} into concrete counts and a {@link MerchantOffer},
 * using the Spice Trader rates from the config.
 */
public final class SpiceTradePricing {

    /** Most emeralds, or most units of a stackable good, one offer asks for. */
    private static final int MAX_COUNT = 64;

    private SpiceTradePricing() {
    }

    /**
     * @param tier      A {@link Tier}.
     * @param direction Which way the trade goes.
     * @return What one loose spice of that Tier is worth in emeralds: the
     *         price multiplier over the Tier's rate for that direction.
     */
    public static double valueOf(Tier tier, SpiceTradeDirection direction) {
        double rate = direction == SpiceTradeDirection.BUY ? Services.CONFIG.getSpiceTraderBuyRate(tier)
                : Services.CONFIG.getSpiceTraderSellRate(tier);
        return Services.CONFIG.getSpiceTraderPriceMultiplier() / rate;
    }

    /**
     * @param item An item.
     * @return The {@link Tier} whose item tag holds it, or {@code null} if no
     *         Tier does.
     */
    public static @Nullable Tier tierOf(Item item) {
        for (Tier tier : Tier.values()) {
            if (item.builtInRegistryHolder().is(tier.getItemTag()))
                return tier;
        }
        return null;
    }

    /**
     * @param spices Counted spices, e.g. the contents of a Spice Mix.
     * @return The rarest {@link Tier} among them; {@link Tier#COMMON} if none
     *         has one.
     */
    public static Tier rarestTierOf(Map<Item, Integer> spices) {
        Tier rarest = Tier.COMMON;
        for (Item item : spices.keySet()) {
            Tier tier = tierOf(item);
            if (tier != null && tier.ordinal() > rarest.ordinal())
                rarest = tier;
        }
        return rarest;
    }

    /**
     * Builds the offer for one good. A good worth less than an emerald is
     * traded in bulk, many units for one emerald; anything else is one unit for
     * its value rounded to whole emeralds, at least one.
     *
     * @param good      What is traded.
     * @param direction Which way the trade goes.
     * @param uses      How many times it can be made before it locks.
     * @param xp        The villager XP each trade gives.
     * @return The offer.
     */
    public static MerchantOffer offerFor(TradeGood good, SpiceTradeDirection direction, int uses, int xp) {
        int units = 1;
        int emeralds;
        if (good.divisible() && good.emeraldValue() < 1.0) {
            int cap = Math.min(MAX_COUNT, good.unit().getMaxStackSize());
            units = Mth.clamp((int) Math.round(1.0 / good.emeraldValue()), 1, cap);
            emeralds = 1;
        } else {
            emeralds = Mth.clamp((int) Math.round(good.emeraldValue()), 1, MAX_COUNT);
        }
        ItemStack goods = good.unit().copyWithCount(units);
        if (direction == SpiceTradeDirection.SELL)
            return new MerchantOffer(new ItemCost(Items.EMERALD, emeralds), goods, uses, xp, 0.05F);
        return new MerchantOffer(costOf(good.unit(), units), new ItemStack(Items.EMERALD, emeralds), uses, xp,
                0.05F);
    }

    /**
     * @param unit  One unit of a good.
     * @param count How many are asked for.
     * @return The cost, which for a Spice Mix only accepts one with exactly
     *         the same contents (so a preset is bought as its standard batch).
     */
    private static ItemCost costOf(ItemStack unit, int count) {
        ItemCost cost = new ItemCost(unit.getItem(), count);
        Optional<SpiceMix> mix = SpiceMixes.contentsOf(unit);
        if (mix.isEmpty())
            return cost;
        return cost.withComponents(builder -> builder.expect(ModDataComponents.SPICE_MIX.get(), mix.get()));
    }
}
