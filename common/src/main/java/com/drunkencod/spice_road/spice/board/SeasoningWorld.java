package com.drunkencod.spice_road.spice.board;

import java.util.OptionalLong;

/**
 * Holds the seed of the world seasoning is happening in. Recipe output is
 * assembled without a level in reach, so Automatic Seasoning reads the seed
 * from here instead. Each loader sets it when a server starts and clears it
 * when it stops; on a dedicated server's client it stays empty.
 */
public final class SeasoningWorld {

    private static volatile OptionalLong seed = OptionalLong.empty();

    private SeasoningWorld() {
    }

    /**
     * @param worldSeed The seed of the world that just started.
     */
    public static void set(long worldSeed) {
        seed = OptionalLong.of(worldSeed);
    }

    /** Forgets the seed, once the server has stopped. */
    public static void clear() {
        seed = OptionalLong.empty();
    }

    /** @return The current world seed, empty while no server is running here. */
    public static OptionalLong seed() {
        return seed;
    }
}
