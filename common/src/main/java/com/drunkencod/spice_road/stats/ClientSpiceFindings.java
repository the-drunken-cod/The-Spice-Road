package com.drunkencod.spice_road.stats;

import java.util.Collections;
import java.util.Set;

import com.drunkencod.spice_road.spice.Spice;

/**
 * The client's copy of the Spices its player has found, as last received in a
 * {@link SpiceFindingsSync}. Empty until the first payload arrives.
 */
public final class ClientSpiceFindings {

    private static volatile Set<Spice> found = Set.of();

    private ClientSpiceFindings() {
    }

    /** @param spices The Spices the player has found, replacing the previous set. */
    static void set(Set<Spice> spices) {
        found = Collections.unmodifiableSet(spices);
    }

    /**
     * @param spice A Spice.
     * @return Whether the player has found it.
     */
    public static boolean isFound(Spice spice) {
        return found.contains(spice);
    }
}
