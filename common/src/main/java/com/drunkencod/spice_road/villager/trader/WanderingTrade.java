package com.drunkencod.spice_road.villager.trader;

import java.util.List;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

/**
 * One wandering trade file: the entries the wandering trader draws one pool's
 * offer from. Files for the same pool are merged.
 *
 * @param pool    Which of the wandering trader's offers these are for.
 * @param entries The weighted choices.
 */
public record WanderingTrade(WanderingTradePool pool, List<SpiceTradeEntry> entries) {

    /** Persistent (JSON) codec. */
    public static final Codec<WanderingTrade> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            WanderingTradePool.CODEC.fieldOf("pool").forGetter(WanderingTrade::pool),
            SpiceTradeEntry.CODEC.listOf().fieldOf("entries").forGetter(WanderingTrade::entries))
            .apply(instance, WanderingTrade::new));
}
