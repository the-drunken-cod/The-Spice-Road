package com.drunkencod.spice_road.platform.services;

public interface IPlatformHelper {

    /**
     * Gets the name of the current platform
     */
    String getPlatformName();

    /**
     * Checks if a mod with the given id is loaded.
     *
     * @param modId The mod to check if it is loaded.
     */
    boolean isModLoaded(String modId);

    /**
     * Check if the game is currently in a development environment.
     */
    boolean isDevelopmentEnvironment();

    /**
     * Check if the current physical distribution is a dedicated server, as
     * opposed to a client (which also hosts integrated/singleplayer servers).
     */
    boolean isDedicatedServer();

    /**
     * Gets the name of the environment type as a string. Can be "development" or
     * "production".
     */
    default String getEnvironmentName() {

        return isDevelopmentEnvironment() ? "development" : "production";
    }
}
