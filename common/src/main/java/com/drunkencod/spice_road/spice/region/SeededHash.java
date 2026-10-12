package com.drunkencod.spice_road.spice.region;

import java.nio.charset.StandardCharsets;

/**
 * Small deterministic hashing/pseudo-random helpers that turn a world seed
 * plus integer coordinates into stable pseudo-random values, without
 * allocating a {@link java.util.Random} (or similar stateful generator) per
 * query. Used by {@link SpiceRegionResolver} to keep cell/pick resolution a
 * pure function of its inputs.
 * <p>
 * Built on SplitMix64's output mixer, used here purely as a fast integer
 * hash rather than for its original stream-splitting purpose.
 */
public final class SeededHash {

    private SeededHash() {
    }

    private static final long GOLDEN_GAMMA = 0x9E3779B97F4A7C15L;

    /**
     * SplitMix64's output mixing step. Deterministic, fast, and has good
     * avalanche behaviour, which is all that is required here.
     *
     * @param z Input value.
     * @return A well-mixed 64-bit hash of {@code z}.
     */
    public static long mix64(long z) {
        z = (z ^ (z >>> 30)) * 0xBF58476D1CE4E5B9L;
        z = (z ^ (z >>> 27)) * 0x94D049BB133111EBL;
        return z ^ (z >>> 31);
    }

    /**
     * Combines a base seed with two integer coordinates into a single
     * deterministic long, suitable as a per-cell or per-pick seed.
     *
     * @param baseSeed The base seed (e.g. the world seed, or a cell seed
     *                 being further specialised).
     * @param a        First coordinate/tag.
     * @param b        Second coordinate/tag.
     */
    public static long hash(long baseSeed, long a, long b) {
        long h = mix64(baseSeed);
        h = mix64(h ^ (a * GOLDEN_GAMMA));
        h = mix64(h ^ (b * GOLDEN_GAMMA));
        return h;
    }

    /**
     * Hashes a string to a stable 64-bit value, the same on every run and
     * platform (FNV-1a over its UTF-8 bytes).
     *
     * @param text The string to hash.
     * @return A 64-bit hash of {@code text}.
     */
    public static long hashString(String text) {
        long hash = 0xCBF29CE484222325L;
        for (byte b : text.getBytes(StandardCharsets.UTF_8)) {
            hash ^= b & 0xFF;
            hash *= 0x100000001B3L;
        }
        return hash;
    }

    /**
     * Derives a double in {@code [0, 1)} from a seed, for weighted picks and
     * jitter offsets.
     *
     * @param seed The seed to convert.
     * @return A pseudo-random double uniformly distributed in {@code [0, 1)}.
     */
    public static double toUnitDouble(long seed) {
        // Top 53 bits, matching java.util.Random/SplittableRandom's approach
        // to generating doubles with full mantissa precision.
        return (mix64(seed) >>> 11) * 0x1.0p-53;
    }
}
