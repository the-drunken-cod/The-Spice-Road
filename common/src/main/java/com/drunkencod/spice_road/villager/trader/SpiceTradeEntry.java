package com.drunkencod.spice_road.villager.trader;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.util.ExtraCodecs;

/**
 * One weighted choice in a Spice Trader trade catalog: what may be traded,
 * and how often, but never at what price.
 *
 * @param weight How likely this entry is relative to the others of its slot.
 * @param source The pool of goods it picks from.
 * @param uses   How many times the resulting offer can be made before it locks
 *               until the villager restocks.
 * @param xp     The villager XP each trade gives.
 */
public record SpiceTradeEntry(int weight, SpiceTradeSource source, int uses, int xp) {

    /** Default for {@link #uses}. */
    public static final int DEFAULT_USES = 12;
    /** Default for {@link #xp}. */
    public static final int DEFAULT_XP = 5;

    /** Persistent (JSON) codec. */
    public static final Codec<SpiceTradeEntry> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            ExtraCodecs.POSITIVE_INT.optionalFieldOf("weight", 1).forGetter(SpiceTradeEntry::weight),
            SpiceTradeSource.CODEC.fieldOf("source").forGetter(SpiceTradeEntry::source),
            ExtraCodecs.POSITIVE_INT.optionalFieldOf("uses", DEFAULT_USES).forGetter(SpiceTradeEntry::uses),
            ExtraCodecs.NON_NEGATIVE_INT.optionalFieldOf("xp", DEFAULT_XP).forGetter(SpiceTradeEntry::xp))
            .apply(instance, SpiceTradeEntry::new));
}
