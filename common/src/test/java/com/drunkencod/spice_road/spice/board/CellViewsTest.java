package com.drunkencod.spice_road.spice.board;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Test;

import com.drunkencod.spice_road.spice.FlavorAxis;
import com.drunkencod.spice_road.spice.SpiceProfile;
import com.drunkencod.spice_road.spice.board.EffectSlot.SlotKind;
import com.drunkencod.spice_road.spice.effect.TestCatalog;

class CellViewsTest {

    private static final SeasoningRules RULES = new SeasoningRules(1.0, 1.5, 2.5, 3.5, 4, TestCatalog.full());

    private static SpiceProfile heat(double value) {
        double[] raw = new double[FlavorAxis.values().length];
        raw[FlavorAxis.HEAT_COOLING.ordinal()] = value;
        return new SpiceProfile(raw);
    }

    private static SeasoningBoard board() {
        List<Cell> cells = new ArrayList<>();
        for (int y = 0; y < BoardGeometry.SIZE; y++) {
            for (int x = 0; x < BoardGeometry.SIZE; x++) {
                CellKind kind = CellKind.BARE;
                List<EffectSlot> slots = List.of();
                if (x == 2 && y == 2) {
                    kind = CellKind.EFFECT;
                    slots = List.of(new EffectSlot(SlotKind.BOON, 1, 1), new EffectSlot(SlotKind.BANE, 1, 2),
                            new EffectSlot(SlotKind.RANDOM, 1, 3));
                } else if (x == 0 && y == 0) {
                    kind = CellKind.EFFECT;
                    slots = List.of(new EffectSlot(SlotKind.BOON, 2, 1));
                } else if (x == 1 && y == 1) {
                    kind = CellKind.MINE;
                    slots = List.of(new EffectSlot(SlotKind.BANE, 2, 4));
                } else if (x == 4 && y == 3) {
                    kind = CellKind.WALL;
                }
                cells.add(new Cell(kind, BoardGeometry.ring(x, y), BoardGeometry.zoneAxis(x, y), slots));
            }
        }
        return new SeasoningBoard(cells);
    }

    private static CellView at(List<CellView> views, int x, int y) {
        return views.get(BoardGeometry.index(x, y));
    }

    @Test
    void minesAndBareCellsLookTheSameUntilTheMineGoesOff() {
        SeasoningBoard board = board();
        SeasoningRun run = new SeasoningRun(board, RULES, PointsLedger.start(heat(10), value -> value));
        List<CellView> views = CellViews.of(board, run, Set.of());
        assertEquals(at(views, 5, 5), at(views, 1, 1));
        assertEquals(CellView.WALL, at(views, 4, 3));

        run.move(Direction.UP_LEFT);
        run.move(Direction.UP_LEFT);
        run.move(Direction.UP_LEFT);
        assertEquals(CellView.Kind.SPENT_MINE, at(CellViews.of(board, run, Set.of()), 1, 1).kind());
    }

    @Test
    void anUnknownEffectCellShowsOnlyItsCountUntilThePawnIsWithinTwoCells() {
        SeasoningBoard board = board();
        SeasoningRun run = new SeasoningRun(board, RULES, PointsLedger.start(heat(10), value -> value));
        // The pawn starts at (4, 4): (2, 2) is 2 cells away, (0, 0) is 4.
        CellView near = at(CellViews.of(board, run, Set.of()), 2, 2);
        assertEquals(CellView.Kind.UNKNOWN, near.kind());
        assertEquals(3, near.effectCount());
        assertEquals(1, near.boons());
        assertEquals(1, near.banes());
        assertEquals(1, near.randoms());
        CellView farther = at(CellViews.of(board, run, Set.of()), 0, 0);
        assertEquals(1, farther.effectCount());
        assertEquals(-1, farther.boons());
    }

    @Test
    void lockingACellInOrKnowingItRevealsItsEffects() {
        SeasoningBoard board = board();
        SeasoningRun run = new SeasoningRun(board, RULES, PointsLedger.start(heat(10), value -> value));
        run.move(Direction.UP_LEFT);
        run.move(Direction.UP_LEFT);
        run.lockIn();
        CellView locked = at(CellViews.of(board, run, Set.of()), 2, 2);
        assertEquals(CellView.Kind.REVEALED, locked.kind());
        assertTrue(locked.lockedIn());
        assertEquals(3, locked.effects().size());

        CellView known = at(CellViews.of(board, run, Set.of(BoardGeometry.index(0, 0))), 0, 0);
        assertEquals(CellView.Kind.REVEALED, known.kind());
        assertEquals(1, known.effects().size());
    }
}
