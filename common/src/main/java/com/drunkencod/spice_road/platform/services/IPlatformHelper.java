package com.drunkencod.spice_road.platform.services;

/**
 * Cross-loader service interface for platform and environment queries.
 */
public interface IPlatformHelper {

    /**
     * @return The name of the current platform (mod loader).
     */
    String getPlatformName();

    /**
     * Checks if a mod with the given id is loaded.
     *
     * @param modId The mod to check if it is loaded.
     * @return Whether the mod is loaded.
     */
    boolean isModLoaded(String modId);

    /**
     * @return Whether the game is currently in a development environment.
     */
    boolean isDevelopmentEnvironment();

    /**
     * @return Whether the current physical distribution is a dedicated server,
     *         as opposed to a client (which also hosts integrated/singleplayer
     *         servers).
     */
    boolean isDedicatedServer();

    /**
     * @return The name of the environment type, either {@code "development"}
     *         or {@code "production"}.
     */
    default String getEnvironmentName() {
        return isDevelopmentEnvironment() ? "development" : "production";
    }
}
