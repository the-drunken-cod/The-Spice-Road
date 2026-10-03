package com.drunkencod.spice_road.spice.effect;

import com.mojang.serialization.Codec;

import net.minecraft.util.StringRepresentable;

/**
 * What a higher Seasoning Effect level does to its mob effect: some effects
 * have a meaningful amplifier, others (e.g. night vision) only ever get longer.
 */
public enum LevelScaling implements StringRepresentable {

    /** Level {@code n} applies amplifier {@code n - 1} for the base duration. */
    AMPLIFIER("amplifier"),
    /** Level {@code n} applies amplifier {@code 0} for {@code n} times the base duration. */
    DURATION("duration");

    /** Codec reading and writing a {@link LevelScaling} by its serialized name. */
    public static final Codec<LevelScaling> CODEC = StringRepresentable.fromEnum(LevelScaling::values);

    private final String id;

    LevelScaling(String id) {
        this.id = id;
    }

    @Override
    public String getSerializedName() {
        return this.id;
    }
}
