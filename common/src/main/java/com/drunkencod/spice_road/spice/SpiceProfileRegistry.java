package com.drunkencod.spice_road.spice;

import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

import net.minecraft.world.item.Item;

/**
 * Holds the datapack-registered default {@link SpiceProfile} for every item
 * loaded by {@link SpiceProfileReloadListener}, keyed by {@link Item}.
 * <p>
 * Repopulated wholesale on every {@code /reload} (and on (re)connect to a
 * server, since this data is server/datapack driven) - never mutated
 * incrementally. Not thread-safe against concurrent {@link #setAll}, but
 * datapack reloads and lookups both happen on the server thread.
 * <p>
 * Use {@link SpiceProfiles#get(net.minecraft.world.item.ItemStack)} rather
 * than this class directly - it also accounts for the per-stack
 * {@code spice_road:spice_profile} data component override.
 */
public final class SpiceProfileRegistry {

    private static volatile Map<Item, SpiceProfile> PROFILES = Map.of();

    private SpiceProfileRegistry() {
    }

    /**
     * Replaces the entire registry contents. Called once per resource
     * reload by {@link SpiceProfileReloadListener}.
     *
     * @param entries The freshly loaded datapack entries. Later entries for
     *                the same item win over earlier ones (matches vanilla's
     *                usual "last loaded datapack wins" override behaviour).
     */
    static void setAll(Collection<SpiceProfileEntry> entries) {
        Map<Item, SpiceProfile> profiles = new ConcurrentHashMap<>();
        for (SpiceProfileEntry entry : entries) {
            profiles.put(entry.item(), entry.profile());
        }
        PROFILES = Map.copyOf(profiles);
    }

    /**
     * @param item The item to look up.
     * @return The datapack-registered default {@link SpiceProfile} for
     *         {@code item}, if any item has been registered as a spice.
     */
    public static Optional<SpiceProfile> getDefault(Item item) {
        return Optional.ofNullable(PROFILES.get(item));
    }
}
