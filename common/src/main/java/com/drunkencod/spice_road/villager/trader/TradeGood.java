package com.drunkencod.spice_road.villager.trader;

import net.minecraft.world.item.ItemStack;

/**
 * Something the Spice Trader can trade, before any count or price is chosen.
 *
 * @param unit         One unit of the good, as the trader hands it out and as
 *                     it accepts it. A Spice Mix is matched by its exact contents.
 * @param emeraldValue What one unit is worth in emeralds, at the configured
 *                     rates and price multiplier.
 * @param divisible    Whether a worthless-ish unit may be traded in bulk (many
 *                     units for one emerald). Otherwise one unit costs at
 *                     least one emerald.
 */
public record TradeGood(ItemStack unit, double emeraldValue, boolean divisible) {
}
