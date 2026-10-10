package com.drunkencod.spice_road.stats;

import java.util.Collections;
import java.util.Set;

import net.minecraft.resources.ResourceLocation;

import com.drunkencod.spice_road.Constants;
import com.drunkencod.spice_road.spice.Spice;

/**
 * The client's copy of the Spices its player has found, as last received in a
 * {@link SpiceFindingsSync}. Empty until the first payload arrives.
 */
public final class ClientSpiceFindings {

    private static volatile Set<ResourceLocation> found = Set.of();

    private ClientSpiceFindings() {
    }

    /** @param items The item IDs of the Spices the player has found, replacing the previous set. */
    static void set(Set<ResourceLocation> items) {
        found = Collections.unmodifiableSet(items);
    }

    /**
     * @param spice A built-in Spice.
     * @return Whether the player has found it.
     */
    public static boolean isFound(Spice spice) {
        return found.contains(ResourceLocation.fromNamespaceAndPath(Constants.MOD_ID, spice.getId()));
    }
}
