package com.drunkencod.spice_road.spice.board;

import com.drunkencod.spice_road.spice.region.SeededHash;

/**
 * Derives the seed of a food item's Seasoning Board: the same for every player
 * and every attempt, different per world, salt and food item.
 */
public final class BoardSeed {

    private static final long FNV_OFFSET = 0xCBF29CE484222325L;
    private static final long FNV_PRIME = 0x100000001B3L;

    private BoardSeed() {
    }

    /**
     * @param worldSeed The world seed.
     * @param salt      The configured board salt; {@code 0} leaves the unsalted boards.
     * @param itemId    The registry ID of the food item, e.g. {@code "minecraft:bread"}.
     * @return The board seed.
     */
    public static long of(long worldSeed, long salt, String itemId) {
        // mix64(0) == 0, so a salt of 0 changes nothing
        long seed = worldSeed ^ SeededHash.mix64(salt);
        return SeededHash.hash(seed, hashString(itemId), 0x5EA50A1L);
    }

    /**
     * A 64-bit FNV-1a hash of a string's UTF-16 code units, stable across JVMs.
     *
     * @param text The string to hash.
     * @return Its hash.
     */
    public static long hashString(String text) {
        long hash = FNV_OFFSET;
        for (int i = 0; i < text.length(); i++) {
            hash ^= text.charAt(i);
            hash *= FNV_PRIME;
        }
        return hash;
    }
}
