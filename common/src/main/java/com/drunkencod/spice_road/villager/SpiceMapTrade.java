package com.drunkencod.spice_road.villager;

import java.util.Map;
import java.util.Optional;

import org.jetbrains.annotations.Nullable;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.npc.VillagerTrades;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.trading.ItemCost;
import net.minecraft.world.item.trading.MerchantOffer;

import com.drunkencod.spice_road.map.SpiceMaps;
import com.drunkencod.spice_road.platform.Services;
import com.drunkencod.spice_road.spice.Tier;
import com.drunkencod.spice_road.spice.region.RegionHeartSearch;

/**
 * Cartographer offer selling a Spice Map for a random Spice of one
 * {@link Tier}, searching from the villager's position. Yields no offer
 * while Spice Map trades are disabled or no Spice of that tier has a Region
 * Heart in range, in which case the villager simply rolls another listing.
 */
public class SpiceMapTrade implements VillagerTrades.ItemListing {

    /**
     * Tier of the Spice Map listing added to each cartographer level's pool.
     * One tier per level, so the same map is never offered at two prices.
     * Novices and apprentices get none, and EPIC maps are loot-only.
     */
    public static final Map<Integer, Tier> TIER_BY_LEVEL = Map.of(
            3, Tier.COMMON,
            4, Tier.UNCOMMON,
            5, Tier.RARE);

    /** Highest total emerald price, i.e. two full stacks. */
    private static final int MAX_PRICE = 128;

    /** Uses before the offer locks until restocked, like vanilla explorer maps. */
    private static final int MAX_USES = 12;

    private final Tier tier;

    /**
     * @param tier The {@link Tier} of Spices this listing maps.
     */
    public SpiceMapTrade(Tier tier) {
        this.tier = tier;
    }

    @Override
    public @Nullable MerchantOffer getOffer(Entity trader, RandomSource random) {
        if (!Services.CONFIG.isSpiceMapTradesEnabled() || !(trader.level() instanceof ServerLevel level))
            return null;

        Optional<ItemStack> map = SpiceMaps.createForTier(level, trader.blockPosition(),
                RegionHeartSearch.cellsToBlocks(Services.CONFIG.getSpiceMapVillagerSearchRadiusCells()), tier, random,
                SpiceMaps.DEFAULT_ZOOM, null);
        if (map.isEmpty())
            return null;

        int price = Mth.clamp((int) Math.round(
                Services.CONFIG.getSpiceMapBasePrice() * Services.CONFIG.getSpiceMapPriceMultiplier(tier)), 1,
                MAX_PRICE);
        ItemCost costA = new ItemCost(Items.EMERALD, Math.min(price, 64));
        // past one stack, second slot holds the remaining emeralds instead of compass:
        ItemCost costB = price > 64 ? new ItemCost(Items.EMERALD, price - 64) : new ItemCost(Items.COMPASS);
        return new MerchantOffer(costA, Optional.of(costB), map.get(), MAX_USES, villagerXpOf(tier), 0.2F);
    }

    /** @return Villager XP granted per trade, rising with the mapped tier. */
    private static int villagerXpOf(Tier tier) {
        return switch (tier) {
            case COMMON -> 5;
            case UNCOMMON -> 10;
            case RARE -> 15;
            case EPIC -> 20;
        };
    }
}
