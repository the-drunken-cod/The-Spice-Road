package com.drunkencod.spice_road.debug;

import java.util.List;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;

/**
 * Temporary stand-in {@link SpiceRegionDebugSource} that returns
 * hardcoded/dummy values.
 * <p>
 * TODO: delete this class once that system exists, and point
 * {@link SpiceRegionDebugManager#setSource(SpiceRegionDebugSource)} at a real
 * implementation instead (e.g. backed by
 * {@code SpiceRegionSystem.query(level, pos)}).
 */
public class StubSpiceRegionDebugSource implements SpiceRegionDebugSource {

    @Override
    public SpiceRegionDebugInfo getDebugInfo(Level level, BlockPos pos) {

        return new SpiceRegionDebugInfo(List.of(
                new SpiceRegionDebugInfo.Line("Cell Id", "stub-cell-0 (fake)"),
                new SpiceRegionDebugInfo.Line("Climate Bucket", "TEMPERATE (fake)"),
                new SpiceRegionDebugInfo.Line("Resolved Spice", "none (fake)"),
                new SpiceRegionDebugInfo.Line("Raw Noise Value", "0.0 (fake)")));
    }
}
