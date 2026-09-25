package com.drunkencod.spice_road.spice;

/**
 * A rarity classification for a Spice, derived from harvest and cultivation
 * difficulty.
 */
public enum Tier {

    COMMON,
    UNCOMMON,
    RARE,
    EPIC;

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
