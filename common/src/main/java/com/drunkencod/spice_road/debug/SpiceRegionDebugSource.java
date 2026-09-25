package com.drunkencod.spice_road.debug;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;

/**
 * Loader-agnostic source of Spice Region (cell) debug info, consumed by the F3
 * debug renderer.
 * <p>
 * TODO: replace {@link StubSpiceRegionDebugSource} with a real implementation
 * once the Perlin/Voronoi region (cell) system exists, e.g. delegating to
 * something like {@code SpiceRegionSystem.query(level, pos)} and mapping
 * its result (cell id, Climate Bucket, resolved Spice, raw noise value, ...)
 * onto {@link SpiceRegionDebugInfo.Line}s. Wire the replacement in via
 * {@link SpiceRegionDebugManager#setSource(SpiceRegionDebugSource)}.
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
