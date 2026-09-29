package com.drunkencod.spice_road.client;

import net.neoforged.fml.ModContainer;
import net.neoforged.neoforge.client.gui.ConfigurationScreen;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;

/**
 * Wires this mod's registered
 * {@link net.neoforged.neoforge.common.ModConfigSpec}s
 * into NeoForge's built-in {@link ConfigurationScreen}, which is what makes the
 * "Config" button in the mod list screen open instead of staying greyed out.
 */
public final class NeoForgeConfigScreenHandler {

    private NeoForgeConfigScreenHandler() {
    }

    /**
     * Registers the config screen factory. Must be called once during client setup.
     */
    public static void register(ModContainer modContainer) {
        modContainer.registerExtensionPoint(IConfigScreenFactory.class, ConfigurationScreen::new);
    }
}
