package com.drunkencod.spice_road.client;

import java.util.List;

import com.drunkencod.spice_road.Constants;
import com.drunkencod.spice_road.debug.SpiceRegionDebugManager;
import com.drunkencod.spice_road.platform.Services;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.client.event.CustomizeGuiOverlayEvent;
import net.neoforged.neoforge.common.NeoForge;

/**
 * NeoForge-side F3 debug hook for {@link SpiceRegionDebugManager}.
 * <p>
 * Hooks into {@code CustomizeGuiOverlayEvent.DebugText} to append Spice Region
 * debug lines to the left-side vanilla F3 debug text.
 * <p>
 * Only ever registered by {@link #registerIfDevelopment()}, which is called
 * once during client setup and gates on
 * {@code Services.PLATFORM.isDevelopmentEnvironment()} - never registered (and
 * therefore never rendering) outside a development environment.
 */
public final class NeoForgeSpiceRegionDebugOverlay {

    private NeoForgeSpiceRegionDebugOverlay() {
    }

    /**
     * Registers the F3 debug text listener, but only if running in a development
     * environment. Must be called only once during client setup.
     */
    public static void registerIfDevelopment() {
        if (!Services.PLATFORM.isDevelopmentEnvironment())
            return;

        NeoForge.EVENT_BUS.addListener(NeoForgeSpiceRegionDebugOverlay::onDebugText);
        Constants.LOG.debug("Registered Spice Region F3 debug overlay (development environment)");
    }

    private static void onDebugText(CustomizeGuiOverlayEvent.DebugText event) {
        final Minecraft minecraft = Minecraft.getInstance();
        final LocalPlayer player = minecraft.player;
        final Level level = minecraft.level;
        if (player == null || level == null)
            return;

        final List<String> lines = SpiceRegionDebugManager.getDebugLines(level, player.blockPosition());
        event.getLeft().addAll(lines);
    }
}
