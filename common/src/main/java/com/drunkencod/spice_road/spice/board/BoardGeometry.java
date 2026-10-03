package com.drunkencod.spice_road.spice.board;

import com.drunkencod.spice_road.spice.FlavorAxis;

/**
 * The fixed shape of the Seasoning Board: a 9x9 grid of nine 3x3 zones, a
 * neutral one in the centre and one per {@link FlavorAxis} around it, in
 * {@link FlavorAxis} order from the top left in reading order. Columns grow to
 * the right, rows downwards, so the centre cell is {@code (4, 4)}.
 */
public final class BoardGeometry {

    /** Cells along each edge. */
    public static final int SIZE = 9;

    /** Cells along each edge of a zone. */
    public static final int ZONE_SIZE = 3;

    /** Column and row of the centre cell, where the pawn starts. */
    public static final int CENTER = SIZE / 2;

    /** The outermost ring, by Chebyshev distance from the centre cell. */
    public static final int MAX_RING = CENTER;

    /** The innermost ring that belongs to an axis zone, the outermost of the neutral one being the one before. */
    public static final int FIRST_AXIS_RING = 2;

    private static final FlavorAxis[][] ZONES = {
            { FlavorAxis.HEAT_COOLING, FlavorAxis.SWEET_BITTER, FlavorAxis.SOUR_MELLOW },
            { FlavorAxis.EARTHY_FLORAL, null, FlavorAxis.WOODY_GREEN },
            { FlavorAxis.PUNGENT_SOFT, FlavorAxis.RESINOUS_CLEAN, FlavorAxis.SAVORY_DELICATE },
    };

    private BoardGeometry() {
    }

    /**
     * @param x A column.
     * @param y A row.
     * @return Whether {@code (x, y)} is on the board.
     */
    public static boolean inBounds(int x, int y) {
        return x >= 0 && x < SIZE && y >= 0 && y < SIZE;
    }

    /**
     * @param x A column on the board.
     * @param y A row on the board.
     * @return The axis of the zone {@code (x, y)} lies in, or {@code null} for the neutral zone.
     */
    public static FlavorAxis zoneAxis(int x, int y) {
        return ZONES[y / ZONE_SIZE][x / ZONE_SIZE];
    }

    /**
     * @param x A column on the board.
     * @param y A row on the board.
     * @return The Chebyshev distance of {@code (x, y)} from the centre cell, {@code 0} to {@value #MAX_RING}.
     */
    public static int ring(int x, int y) {
        return Math.max(Math.abs(x - CENTER), Math.abs(y - CENTER));
    }

    /**
     * @param x A column on the board.
     * @param y A row on the board.
     * @return The index of {@code (x, y)} in a row-major array of every cell.
     */
    public static int index(int x, int y) {
        return y * SIZE + x;
    }
}
