package com.drunkencod.spice_road.villager.trader;

import java.util.List;
import java.util.function.UnaryOperator;

import org.jetbrains.annotations.Nullable;

import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.npc.VillagerTrades;
import net.minecraft.world.item.trading.MerchantOffer;

/**
 * A Spice Trader's offer slot for one villager level and direction. It picks a
 * {@link SpiceTradeEntry} from the {@link SpiceTradeCatalog} by weight, then a
 * good from the entry's source, and prices it from the config. Entries whose
 * source has no qualifying good right now are passed over, so a slot only
 * yields no offer if the catalog has nothing for it.
 */
public class SpiceTraderListing implements VillagerTrades.ItemListing {

    private final int level;
    private final SpiceTradeDirection direction;

    /**
     * @param level     The villager level, 1 to 5.
     * @param direction Whether this slot is the trader's buy or sell offer.
     */
    public SpiceTraderListing(int level, SpiceTradeDirection direction) {
        this.level = level;
        this.direction = direction;
    }

    /**
     * @param level The villager level, 1 to 5.
     * @return The level's two slots: what the trader buys, then what it sells.
     */
    public static List<VillagerTrades.ItemListing> forLevel(int level) {
        return List.of(new SpiceTraderListing(level, SpiceTradeDirection.BUY),
                new SpiceTraderListing(level, SpiceTradeDirection.SELL));
    }

    @Override
    public @Nullable MerchantOffer getOffer(Entity trader, RandomSource random) {
        return SpiceTradeRoller.roll(SpiceTradeCatalog.entriesFor(level, direction), direction, random,
                UnaryOperator.identity(), good -> true);
    }
}
