package com.drunkencod.spice_road.spice.effect;

import com.mojang.serialization.Codec;

import net.minecraft.util.StringRepresentable;

/**
 * Whether a Seasoning Effect helps or hurts the one who eats the food.
 */
public enum EffectKind implements StringRepresentable {

    /** Helps the eater. */
    BOON("boon"),
    /** Hurts the eater. */
    BANE("bane");

    /** Codec reading and writing an {@link EffectKind} by its serialized name. */
    public static final Codec<EffectKind> CODEC = StringRepresentable.fromEnum(EffectKind::values);

    private final String id;

    EffectKind(String id) {
        this.id = id;
    }

    @Override
    public String getSerializedName() {
        return this.id;
    }
}
