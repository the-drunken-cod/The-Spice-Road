package com.drunkencod.spice_road.spice.board;

import java.util.List;

import com.drunkencod.spice_road.spice.FlavorAxis;

/**
 * One cell of a {@link SeasoningBoard}.
 *
 * @param kind  What the cell is.
 * @param ring  Its Chebyshev distance from the centre cell.
 * @param zone  The axis of the zone it lies in, {@code null} for the neutral zone.
 * @param slots The effects of an {@link CellKind#EFFECT} cell's bundle, or the
 *              single bane of a {@link CellKind#MINE}; empty for other cells.
 */
public record Cell(CellKind kind, int ring, FlavorAxis zone, List<EffectSlot> slots) {

    /**
     * @param slots The cell's slots, copied.
     */
    public Cell {
        slots = List.copyOf(slots);
    }
}
