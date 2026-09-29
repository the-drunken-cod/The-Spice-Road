package com.drunkencod.spice_road.config;

import java.util.Map;

/**
 * Values received from {@link ConfigSync}, overriding the client's local
 * {@code SERVER} config file for as long as it stays connected to that
 * server. Cleared on disconnect, so singleplayer and later servers read the
 * local file again.
 */
public final class ConfigSyncOverride {

    private static volatile Map<String, Object> VALUES = Map.of();

    private ConfigSyncOverride() {
    }

    /**
     * Applies a freshly received sync payload, replacing any previous
     * override.
     *
     * @param values Every synced option's value, keyed by dotted path.
     */
    public static void accept(Map<String, Object> values) {
        VALUES = Map.copyOf(values);
    }

    /** Clears the active override, reverting reads to the local config file. */
    public static void clear() {
        VALUES = Map.of();
    }

    /**
     * @param <T>    The option's value type.
     * @param option The option to resolve.
     * @return The server-synced value, or {@code null} if none was received
     *         for it, or its type doesn't match {@code option}'s.
     */
    @SuppressWarnings("unchecked")
    public static <T extends Comparable<T>> T get(ConfigOption<T> option) {
        Object value = VALUES.get(option.getDottedPath());
        return option.getType().isInstance(value) ? (T) value : null;
    }
}
