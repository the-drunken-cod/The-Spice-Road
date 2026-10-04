package com.drunkencod.spice_road.spice.board;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import com.drunkencod.spice_road.spice.board.EffectSlot.SlotKind;

/**
 * Builds what a player may see of a board during a {@link SeasoningRun}.
 */
public final class CellViews {

    /** Cells (Chebyshev) from the pawn within which an unknown effect cell shows its makeup. */
    public static final int HINT_RANGE = 2;

    private CellViews() {
    }

    /**
     * @param board    The board being played.
     * @param run      The run.
     * @param revealed Indices of the effect cells this player already knows
     *                 the content of, besides the ones locked in this run.
     * @return The view of every cell in row-major order, {@code 81} of them.
     */
    public static List<CellView> of(SeasoningBoard board, SeasoningRun run, Set<Integer> revealed) {
        List<CellView> views = new ArrayList<>(board.cells().size());
        for (int y = 0; y < BoardGeometry.SIZE; y++) {
            for (int x = 0; x < BoardGeometry.SIZE; x++)
                views.add(view(board.cell(x, y), x, y, run, revealed));
        }
        return views;
    }

    private static CellView view(Cell cell, int x, int y, SeasoningRun run, Set<Integer> revealed) {
        switch (cell.kind()) {
            case WALL:
                return CellView.WALL;
            case MINE:
                return run.isDetonated(x, y) ? CellView.SPENT_MINE : CellView.BLANK;
            case EFFECT:
                return effectView(cell, x, y, run, revealed);
            default:
                return CellView.BLANK;
        }
    }

    private static CellView effectView(Cell cell, int x, int y, SeasoningRun run, Set<Integer> revealed) {
        boolean locked = run.isLockedIn(x, y);
        if (locked || revealed.contains(BoardGeometry.index(x, y))) {
            List<com.drunkencod.spice_road.spice.effect.SeasoningEffect> effects = run.resolveCell(x, y);
            if (!effects.isEmpty())
                return new CellView(CellView.Kind.REVEALED, 0, -1, -1, -1, effects, locked);
        }
        boolean near = Math.max(Math.abs(x - run.x()), Math.abs(y - run.y())) <= HINT_RANGE;
        int boons = -1;
        int banes = -1;
        int randoms = -1;
        if (near) {
            boons = count(cell, SlotKind.BOON);
            banes = count(cell, SlotKind.BANE);
            randoms = count(cell, SlotKind.RANDOM);
        }
        return new CellView(CellView.Kind.UNKNOWN, cell.slots().size(), boons, banes, randoms, List.of(), locked);
    }

    private static int count(Cell cell, SlotKind kind) {
        return (int) cell.slots().stream().filter(slot -> slot.kind() == kind).count();
    }
}
