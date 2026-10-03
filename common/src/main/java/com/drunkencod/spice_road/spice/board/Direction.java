package com.drunkencod.spice_road.spice.board;

import com.drunkencod.spice_road.spice.FlavorAxis;

/**
 * One of the eight directions the pawn can move on the Seasoning Board, each
 * permanently bound to the {@link FlavorAxis} whose zone lies in that direction
 * from the centre. A step pays from its bound axis wherever the pawn stands.
 */
public enum Direction {

    /** Up-left, bound to the heat axis. */
    UP_LEFT(-1, -1, FlavorAxis.HEAT_COOLING),
    /** Up, bound to the sweet axis. */
    UP(0, -1, FlavorAxis.SWEET_BITTER),
    /** Up-right, bound to the sour axis. */
    UP_RIGHT(1, -1, FlavorAxis.SOUR_MELLOW),
    /** Left, bound to the earthy axis. */
    LEFT(-1, 0, FlavorAxis.EARTHY_FLORAL),
    /** Right, bound to the woody axis. */
    RIGHT(1, 0, FlavorAxis.WOODY_GREEN),
    /** Down-left, bound to the pungent axis. */
    DOWN_LEFT(-1, 1, FlavorAxis.PUNGENT_SOFT),
    /** Down, bound to the resinous axis. */
    DOWN(0, 1, FlavorAxis.RESINOUS_CLEAN),
    /** Down-right, bound to the savory axis. */
    DOWN_RIGHT(1, 1, FlavorAxis.SAVORY_DELICATE);

    private final int dx;
    private final int dy;
    private final FlavorAxis axis;

    Direction(int dx, int dy, FlavorAxis axis) {
        this.dx = dx;
        this.dy = dy;
        this.axis = axis;
    }

    /** @return The column offset of a step, {@code -1}, {@code 0} or {@code 1}. */
    public int dx() {
        return dx;
    }

    /** @return The row offset of a step, {@code -1}, {@code 0} or {@code 1}; rows grow downwards. */
    public int dy() {
        return dy;
    }

    /** @return The axis a step in this direction pays from. */
    public FlavorAxis axis() {
        return axis;
    }

    /** @return The direction pointing the other way, bound to the opposite zone's axis. */
    public Direction opposite() {
        return values()[values().length - 1 - ordinal()];
    }

    /**
     * @param axis A Flavor Axis.
     * @return The direction bound to {@code axis}.
     */
    public static Direction of(FlavorAxis axis) {
        for (Direction direction : values()) {
            if (direction.axis == axis)
                return direction;
        }
        throw new IllegalArgumentException("No direction for " + axis);
    }
}
