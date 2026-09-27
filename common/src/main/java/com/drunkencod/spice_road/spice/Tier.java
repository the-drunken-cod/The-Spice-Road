package com.drunkencod.spice_road.spice;

import java.util.Locale;

import com.mojang.serialization.Codec;

import net.minecraft.util.StringRepresentable;
import net.minecraft.world.item.Rarity;

/**
 * A rarity classification for a Spice, derived from harvest and cultivation
 * difficulty.
 */
public enum Tier implements StringRepresentable {

    COMMON(Rarity.COMMON),
    UNCOMMON(Rarity.UNCOMMON),
    RARE(Rarity.RARE),
    EPIC(Rarity.EPIC);

    /** Codec reading and writing a {@link Tier} by its lowercase name, e.g. {@code "epic"}. */
    public static final Codec<Tier> CODEC = StringRepresentable.fromEnum(Tier::values);

    private final Rarity rarity;

    Tier(Rarity rarity) {
        this.rarity = rarity;
    }

    /** @return The vanilla item {@link Rarity} matching this tier, which colors item names. */
    public Rarity getRarity() {
        return rarity;
    }

    @Override
    public String getSerializedName() {
        return name().toLowerCase(Locale.ROOT);
    }

    /**
     * Derives a {@link Tier} from a 1-5 harvest and cultivation difficulty value.
     * <p>
     * Mapping: {@code 1 -> COMMON}, {@code {2, 3} -> UNCOMMON},
     * {@code 4 -> RARE}, {@code 5 -> EPIC}.
     *
     * @param harvestDifficulty The 1-5 harvest and cultivation difficulty value.
     * @throws IllegalArgumentException If {@code harvestDifficulty} is not in the
     *                                  range 1-5.
     */
    public static Tier fromHarvestDifficulty(int harvestDifficulty) {
        return switch (harvestDifficulty) {
            case 1 -> COMMON;
            case 2, 3 -> UNCOMMON;
            case 4 -> RARE;
            case 5 -> EPIC;
            default -> throw new IllegalArgumentException(
                    "Harvest difficulty must be in range 1-5, got " + harvestDifficulty);
        };
    }
}
