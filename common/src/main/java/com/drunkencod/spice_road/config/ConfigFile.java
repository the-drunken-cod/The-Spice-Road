package com.drunkencod.spice_road.config;

import com.drunkencod.spice_road.Constants;

/**
 * One of the mod's config files. Each loader maps these onto its own notion of
 * config scope - {@code ModConfig.Type} on NeoForge, a separate AutoConfig
 * class on Fabric.
 */
public enum ConfigFile {

    /** Gameplay values. Synced from the logical server on NeoForge. */
    SERVER("server"),
    /** Client-only display values, never synced. */
    CLIENT("client");

    private final String suffix;

    ConfigFile(String suffix) {
        this.suffix = suffix;
    }

    /**
     * @return The file's name suffix, e.g. {@code "server"} for
     *         {@code spice_road-server.toml}.
     */
    public String getSuffix() {
        return suffix;
    }

    /**
     * @return The AutoConfig config name backing this file on Fabric, e.g.
     *         {@code "spice_road_server"}.
     */
    public String getAutoConfigName() {
        return Constants.MOD_ID + "_" + suffix;
    }
}
