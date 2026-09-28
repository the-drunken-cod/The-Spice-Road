package com.drunkencod.spice_road.spice.region;

import org.joml.Vector3d;

import dev.ryanhcode.sable.companion.SableCompanion;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;

/**
 * Projects a position out of a Sable sub-level (e.g. a moving airship
 * contraption) before it feeds into Spice Region math, so a Spice Plant
 * built on one is judged against whichever region the contraption is
 * currently over, instead of the arbitrary, fixed coordinates its block is
 * actually stored at.
 */
public final class SublevelPositions {

    private SublevelPositions() {
    }

    /**
     * @param level The level {@code pos} is read against.
     * @param pos   The position to project.
     * @return {@code pos} unchanged if it isn't inside a Sable sub-level
     *         plot, otherwise the position the sub-level currently,
     *         logically occupies.
     */
    public static BlockPos projectOutOfSubLevel(Level level, BlockPos pos) {
        Vector3d center = new Vector3d(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5);
        Vector3d projected = SableCompanion.INSTANCE.projectOutOfSubLevel(level, center);
        return BlockPos.containing(projected.x, projected.y, projected.z);
    }
}
