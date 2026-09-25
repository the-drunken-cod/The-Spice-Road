package com.drunkencod.spice_road.debug;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;

/**
 * Loader-agnostic source of Spice Region (cell) debug info, consumed by the F3
 * debug renderer. See {@link SpiceRegionDebugSourceImpl} for the real,
 * {@code SpiceRegionResolver}-backed implementation currently wired in via
 * {@link SpiceRegionDebugManager}.
 */
public interface SpiceRegionDebugSource {

    /**
     * Gathers debug info for the Spice Region system at the given position.
     *
     * @param level The level the position is in.
     * @param pos   The position to query (usually the client player's position).
     * @return The debug info to display on the F3 screen, or {@code null} if
     *         unavailable.
     */
    SpiceRegionDebugInfo getDebugInfo(Level level, BlockPos pos);
}
