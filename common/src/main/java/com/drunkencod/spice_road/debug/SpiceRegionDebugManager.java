package com.drunkencod.spice_road.debug;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;

/**
 * Loader-agnostic entry point for gathering Spice Region debug lines for the F3
 * debug screen.
 * <p>
 * Each loader's client-side F3 hook (NeoForge:
 * {@code CustomizeGuiOverlayEvent.DebugText};
 * Fabric: a small custom debug HUD overlay - see the respective loader modules
 * for why) calls
 * {@link #getDebugLines(Level, BlockPos)} and appends the result to the debug
 * display.
 * <p>
 * Gating on {@code Services.PLATFORM.isDevelopmentEnvironment()} happens once,
 * at hook <b>registration</b> time, in each loader module - this class does not
 * re-check it per frame, and is never even wired up outside a development
 * environment.
 */
public final class SpiceRegionDebugManager {

    private static SpiceRegionDebugSource source = new SpiceRegionDebugSourceImpl();

    private SpiceRegionDebugManager() {
    }

    /**
     * Overrides the active debug info source. Exposed so the real region-system
     * implementation
     * can be wired in later without touching the F3 rendering code in either loader
     * module.
     *
     * @param newSource The new source to use.
     */
    public static void setSource(SpiceRegionDebugSource newSource) {
        source = newSource;
    }

    /**
     * Gathers formatted "label: value" debug lines to append to the F3 debug
     * screen.
     *
     * @param level The level the position is in.
     * @param pos   The position to query.
     * @return The formatted debug lines; empty if no info is available.
     */
    public static List<String> getDebugLines(Level level, BlockPos pos) {
        final List<String> result = new ArrayList<>();
        final SpiceRegionDebugInfo info = source.getDebugInfo(level, pos);
        if (info == null) {
            return result;
        }

        for (SpiceRegionDebugInfo.Line line : info.lines()) {
            result.add("[Spice Road] " + line);
        }
        return result;
    }
}
