package com.drunkencod.spice_road.spice;

import java.util.Locale;

import org.jetbrains.annotations.Nullable;

import com.mojang.serialization.Codec;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;

import com.drunkencod.spice_road.Constants;

/**
 * A rarity classification for a Spice, derived from harvest and cultivation
 * difficulty.
 */
public enum Tier implements StringRepresentable {

    COMMON(Rarity.COMMON, 0.5F),
    UNCOMMON(Rarity.UNCOMMON, 0.4F),
    RARE(Rarity.RARE, 0.3F),
    EPIC(Rarity.EPIC, 0.2F);

    /** Codec reading and writing a {@link Tier} by its lowercase name, e.g. {@code "epic"}. */
    public static final Codec<Tier> CODEC = StringRepresentable.fromEnum(Tier::values);

    private final Rarity rarity;
    private final float seedDropChance;

    Tier(Rarity rarity, float seedDropChance) {
        this.rarity = rarity;
        this.seedDropChance = seedDropChance;
    }

    /**
     * @return The chance, from {@code 0.0} to {@code 1.0}, that harvesting a
     *         mature plant of this tier drops its planting item. Rarer tiers
     *         are less likely to give their seeds back.
     */
    public float getSeedDropChance() {
        return seedDropChance;
    }

    /** @return The vanilla item {@link Rarity} matching this tier, which colors item names. */
    public Rarity getRarity() {
        return rarity;
    }

    /**
     * @return The item tag of every item belonging to a Spice of this tier
     *         (Spice Items, seeds and saplings), e.g.
     *         {@code #spice_road:spice_tiers/epic}.
     */
    public TagKey<Item> getItemTag() {
        return TagKey.create(Registries.ITEM,
                ResourceLocation.fromNamespaceAndPath(Constants.MOD_ID, "spice_tiers/" + getSerializedName()));
    }

    @Override
    public String getSerializedName() {
        return name().toLowerCase(Locale.ROOT);
    }

    /**
     * Looks up a Tier by its {@link #getSerializedName() name}.
     *
     * @param name The Tier's name, e.g. {@code "epic"}.
     * @return The matching Tier, or {@code null} if there is none.
     */
    public static @Nullable Tier byName(String name) {
        for (Tier tier : values()) {
            if (tier.getSerializedName().equals(name))
                return tier;
        }
        return null;
    }

    /**
     * @return The lowest 1-5 harvest and cultivation difficulty that maps to
     *         this tier. See {@link #fromHarvestDifficulty}.
     */
    public int getMinHarvestDifficulty() {
        return switch (this) {
            case COMMON -> 1;
            case UNCOMMON -> 2;
            case RARE -> 4;
            case EPIC -> 5;
        };
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
