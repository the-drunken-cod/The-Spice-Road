package com.drunkencod.spice_road.client;

import java.util.List;

import com.drunkencod.spice_road.Constants;
import com.drunkencod.spice_road.debug.SpiceRegionDebugManager;
import com.drunkencod.spice_road.platform.Services;

import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.level.Level;

/**
 * Fabric-side F3 debug overlay for {@link SpiceRegionDebugManager}.
 * <p>
 * Fabric API does not expose an equivalent of NeoForge's
 * {@code CustomizeGuiOverlayEvent.DebugText} for appending to the vanilla
 * left-side F3 debug text, so this instead draws a small, self-contained
 * overlay via {@link HudRenderCallback}, only while the F3 screen is open.
 * Its vertical position is computed from {@link #recordVanillaLeftColumnLineCount(int)},
 * which a mixin feeds with the actual number of lines vanilla's left debug
 * column just rendered, so this overlay still lands below it even when other
 * mods add or remove lines from that column. Since it's only visible in
 * development mode, this implementation is good enough.
 */
public final class FabricSpiceRegionDebugOverlay {

    /**
     * Matches vanilla {@code DebugScreenOverlay}'s own top margin and
     * per-line height, so our overlay lines up flush below its left column.
     */
    private static final int START_X = 2;
    private static final int TOP_MARGIN = 2;
    private static final int LINE_HEIGHT = 9;
    private static final int TEXT_COLOR = 0xE0E0E0;

    private static volatile int vanillaLeftColumnLineCount = 0;

    private FabricSpiceRegionDebugOverlay() {
    }

    /**
     * Called by {@code DebugScreenOverlayMixin} with the number of lines
     * vanilla's left F3 debug column rendered this frame, so
     * {@link #onHudRender(GuiGraphics, DeltaTracker)} knows where that column
     * ends regardless of what other mods add to it.
     *
     * @param lineCount number of lines vanilla just rendered in its left debug column
     */
    public static void recordVanillaLeftColumnLineCount(int lineCount) {
        vanillaLeftColumnLineCount = lineCount;
    }

    /**
     * Registers the HUD render callback, but only if running in a development
     * environment. Must be called only once during client init.
     */
    public static void registerIfDevelopment() {
        if (!Services.PLATFORM.isDevelopmentEnvironment())
            return;

        HudRenderCallback.EVENT.register(FabricSpiceRegionDebugOverlay::onHudRender);
        Constants.LOG.debug("Registered Spice Region F3 debug overlay (development environment)");
    }

    private static void onHudRender(GuiGraphics guiGraphics, DeltaTracker deltaTracker) {
        final Minecraft minecraft = Minecraft.getInstance();
        if (!isDebugScreenShown(minecraft))
            return;

        final LocalPlayer player = minecraft.player;
        final Level level = minecraft.level;
        if (player == null || level == null)
            return;

        final List<String> lines = SpiceRegionDebugManager.getDebugLines(level, player.blockPosition());
        int y = TOP_MARGIN + LINE_HEIGHT * vanillaLeftColumnLineCount;
        for (String line : lines) {
            guiGraphics.drawString(minecraft.font, line, START_X, y, TEXT_COLOR);
            y += LINE_HEIGHT;
        }
    }

    private static boolean isDebugScreenShown(Minecraft minecraft) {
        return minecraft.gui.getDebugOverlay().showDebugScreen();
    }
}
