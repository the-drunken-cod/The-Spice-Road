package com.drunkencod.spice_road.spice.board;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.mojang.serialization.JsonOps;

import com.drunkencod.spice_road.spice.FlavorAxis;
import com.drunkencod.spice_road.spice.SpiceProfile;
import com.drunkencod.spice_road.spice.effect.TestCatalog;

class RunStateTest {

    private static final SeasoningRules RULES = new SeasoningRules(1.0, 1.5, 2.5, 3.5, 4, TestCatalog.full());

    private static SpiceProfile heat(double value) {
        double[] raw = new double[FlavorAxis.values().length];
        raw[FlavorAxis.HEAT_COOLING.ordinal()] = value;
        return new SpiceProfile(raw);
    }

    @Test
    void aSavedRunContinuesExactlyWhereItWas() {
        SeasoningBoard board = BoardGenerator.generate(99L, BoardLayout.DEFAULT);
        SeasoningRun run = new SeasoningRun(board, RULES, PointsLedger.start(heat(8), value -> value));
        run.move(Direction.UP_LEFT);
        run.move(Direction.UP_LEFT);
        if (run.canLockIn())
            run.lockIn();

        RunState saved = run.state();
        SeasoningRun restored = SeasoningRun.restore(board, RULES, heat(8), value -> value, saved);
        assertEquals(saved, restored.state());
        assertEquals(run.points(FlavorAxis.HEAT_COOLING), restored.points(FlavorAxis.HEAT_COOLING), 1e-9);
        assertEquals(run.effects(), restored.effects());
        assertEquals(run.canMove(Direction.UP_LEFT), restored.canMove(Direction.UP_LEFT));
    }

    @Test
    void theStateSurvivesAnEncodeAndParseRoundTrip() {
        SeasoningBoard board = BoardGenerator.generate(5L, BoardLayout.DEFAULT);
        SeasoningRun run = new SeasoningRun(board, RULES, PointsLedger.start(heat(8), value -> value));
        run.move(Direction.UP_LEFT);
        RunState state = run.state();
        var json = RunState.CODEC.encodeStart(JsonOps.INSTANCE, state).getOrThrow();
        assertEquals(state, RunState.CODEC.parse(JsonOps.INSTANCE, json).getOrThrow());
    }

    @Test
    void aRestoredRunRemembersWhichMinesWentOffAndWhichCellsWereLockedIn() {
        List<Cell> cells = new ArrayList<>();
        for (int y = 0; y < BoardGeometry.SIZE; y++) {
            for (int x = 0; x < BoardGeometry.SIZE; x++) {
                CellKind kind = x == 2 && y == 2 ? CellKind.MINE : CellKind.BARE;
                List<EffectSlot> slots = kind == CellKind.MINE
                        ? List.of(new EffectSlot(EffectSlot.SlotKind.BANE, 1, 0))
                        : List.of();
                cells.add(new Cell(kind, BoardGeometry.ring(x, y), BoardGeometry.zoneAxis(x, y), slots));
            }
        }
        SeasoningBoard board = new SeasoningBoard(cells);
        SeasoningRun run = new SeasoningRun(board, RULES, PointsLedger.start(heat(10), value -> value));
        run.move(Direction.UP_LEFT);
        assertTrue(run.move(Direction.UP_LEFT).mineHit());
        SeasoningRun restored = SeasoningRun.restore(board, RULES, heat(10), value -> value, run.state());
        assertTrue(restored.isDetonated(2, 2));
        assertEquals(run.effects(), restored.effects());
    }
}
