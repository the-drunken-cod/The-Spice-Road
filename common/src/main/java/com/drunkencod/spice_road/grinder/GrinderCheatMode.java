package com.drunkencod.spice_road.grinder;

import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * The players who have the Spice Grinder's cheat mode on: every Spice Item is
 * in unlimited supply for them, and using one takes nothing (see
 * {@link SpiceSources}). Kept in memory only, so it ends with the server and
 * can't linger unnoticed.
 */
public final class GrinderCheatMode {

    /**
     * How many of each Spice Item a player in cheat mode appears to have, enough
     * for the biggest food stack at the biggest per-kind cap.
     */
    public static final int STOCK = 9999;

    private static final Set<UUID> ENABLED = ConcurrentHashMap.newKeySet();

    private GrinderCheatMode() {
    }

    /**
     * @param player A player's UUID.
     * @return Whether they have cheat mode on.
     */
    public static boolean isEnabled(UUID player) {
        return ENABLED.contains(player);
    }

    /**
     * Turns cheat mode on for a player who has it off and off for one who has
     * it on.
     *
     * @param player A player's UUID.
     * @return Whether it is on now.
     */
    public static boolean toggle(UUID player) {
        if (ENABLED.add(player))
            return true;
        ENABLED.remove(player);
        return false;
    }

    /** Turns cheat mode off for everyone, e.g. when the server stops. */
    public static void clear() {
        ENABLED.clear();
    }
}
