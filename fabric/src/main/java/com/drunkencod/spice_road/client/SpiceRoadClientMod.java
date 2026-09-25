package com.drunkencod.spice_road.client;

import net.fabricmc.api.ClientModInitializer;

/**
 * Fabric client-only entry point. Keep this limited to client-only setup that
 * must not run on a dedicated server.
 */
public class SpiceRoadClientMod implements ClientModInitializer {

    @Override
    public void onInitializeClient() {
        // Debug-only F3 overlay; registerIfDevelopment() itself gates on
        // Services.PLATFORM.isDevelopmentEnvironment(), so this is a no-op in
        // production.
        FabricSpiceRegionDebugOverlay.registerIfDevelopment();
    }
}
