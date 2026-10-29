package com.drunkencod.spice_road.villager.trader;

import java.util.List;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

/**
 * One trade catalog file: the entries a Spice Trader of one villager level
 * draws its single offer in one direction from. Files for the same level and
 * direction are merged.
 *
 * @param level     The villager level, 1 (Novice) to 5 (Master).
 * @param direction Whether these are offers the trader buys or sells.
 * @param entries   The weighted choices.
 */
public record SpiceTrade(int level, SpiceTradeDirection direction, List<SpiceTradeEntry> entries) {

    /** Persistent (JSON) codec. */
    public static final Codec<SpiceTrade> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.intRange(1, 5).fieldOf("level").forGetter(SpiceTrade::level),
            SpiceTradeDirection.CODEC.fieldOf("direction").forGetter(SpiceTrade::direction),
            SpiceTradeEntry.CODEC.listOf().fieldOf("entries").forGetter(SpiceTrade::entries))
            .apply(instance, SpiceTrade::new));
}
