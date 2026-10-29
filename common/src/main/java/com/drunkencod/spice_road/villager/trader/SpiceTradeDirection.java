package com.drunkencod.spice_road.villager.trader;

import com.mojang.serialization.Codec;

import net.minecraft.util.StringRepresentable;

/**
 * Which way a Spice Trader offer goes, from the trader's point of view.
 */
public enum SpiceTradeDirection implements StringRepresentable {
    /** The trader pays emeralds for goods. */
    BUY("buy"),
    /** The trader sells goods for emeralds. */
    SELL("sell");

    /** Persistent (JSON) codec. */
    public static final Codec<SpiceTradeDirection> CODEC = StringRepresentable
            .fromEnum(SpiceTradeDirection::values);

    private final String name;

    SpiceTradeDirection(String name) {
        this.name = name;
    }

    @Override
    public String getSerializedName() {
        return name;
    }
}
