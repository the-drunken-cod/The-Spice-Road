package com.drunkencod.spice_road.spice.board;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import com.drunkencod.spice_road.spice.FlavorAxis;
import com.drunkencod.spice_road.spice.SpiceProfile;
import com.drunkencod.spice_road.spice.board.EffectSlot.SlotKind;
import com.drunkencod.spice_road.spice.effect.TestCatalog;

class SeasoningBotTest {

    private static final SeasoningRules RULES = new SeasoningRules(1.0, 1.5, 2.5, 3.5, 4, TestCatalog.full());

    private static SeasoningBoard board(Map<Integer, Cell> replaced) {
        List<Cell> cells = new ArrayList<>();
        for (int y = 0; y < BoardGeometry.SIZE; y++) {
            for (int x = 0; x < BoardGeometry.SIZE; x++) {
                Cell custom = replaced.get(BoardGeometry.index(x, y));
                cells.add(custom != null ? custom
                        : new Cell(CellKind.BARE, BoardGeometry.ring(x, y), BoardGeometry.zoneAxis(x, y), List.of()));
            }
        }
        return new SeasoningBoard(cells);
    }

    private static Cell at(int x, int y, CellKind kind, EffectSlot... slots) {
        return new Cell(kind, BoardGeometry.ring(x, y), BoardGeometry.zoneAxis(x, y), List.of(slots));
    }

    private static PointsLedger budget(Object... pairs) {
        double[] raw = new double[FlavorAxis.values().length];
        for (int i = 0; i < pairs.length; i += 2)
            raw[((FlavorAxis) pairs[i]).ordinal()] = ((Number) pairs[i + 1]).doubleValue();
        return PointsLedger.start(new SpiceProfile(raw), value -> value);
    }

    @Test
    void walksStraightInTheStrongestAxisDirectionAndLocksInWhatItLandsOn() {
        SeasoningBoard board = board(Map.of(BoardGeometry.index(2, 2), at(2, 2, CellKind.EFFECT,
                new EffectSlot(SlotKind.BOON, 1, 1))));
        SeasoningRun run = new SeasoningRun(board, RULES, budget(FlavorAxis.HEAT_COOLING, 5, FlavorAxis.SWEET_BITTER, 4));
        SeasoningBot.play(run);
        // 2 steps (2.0), a lock-in (1.5), then 1 more step with the remaining 1.5 points.
        assertEquals(1, run.x());
        assertEquals(1, run.y());
        assertEquals(1, run.effects().size());
        assertEquals(0.5, run.points(FlavorAxis.HEAT_COOLING), 1e-9);
        assertEquals(4D, run.points(FlavorAxis.SWEET_BITTER), "it never touches the weaker axes");
    }

    @Test
    void stopsAtAWallWithoutDetouring() {
        SeasoningBoard board = board(Map.of(BoardGeometry.index(3, 3), at(3, 3, CellKind.WALL)));
        SeasoningRun run = new SeasoningRun(board, RULES, budget(FlavorAxis.HEAT_COOLING, 9));
        SeasoningBot.play(run);
        assertEquals(BoardGeometry.CENTER, run.x());
        assertEquals(BoardGeometry.CENTER, run.y());
        assertEquals(9D, run.points(FlavorAxis.HEAT_COOLING));
    }

    @Test
    void breaksTiesByFlavorAxisOrderAndDoesNothingWithoutPoints() {
        SeasoningRun tie = new SeasoningRun(board(Map.of()), RULES,
                budget(FlavorAxis.SOUR_MELLOW, 2, FlavorAxis.SWEET_BITTER, 2));
        SeasoningBot.play(tie);
        assertEquals(BoardGeometry.CENTER, tie.x(), "sweet comes before sour, so it walks up");
        assertEquals(BoardGeometry.CENTER - 2, tie.y());

        SeasoningRun broke = new SeasoningRun(board(Map.of()), RULES, budget());
        SeasoningBot.play(broke);
        assertEquals(BoardGeometry.CENTER, broke.x());
        assertTrue(broke.effects().isEmpty());
    }

    @Test
    void ignoresMinesAndWalksOverThem() {
        SeasoningBoard board = board(Map.of(BoardGeometry.index(3, 3), at(3, 3, CellKind.BARE),
                BoardGeometry.index(2, 2), at(2, 2, CellKind.MINE, new EffectSlot(SlotKind.BANE, 1, 5))));
        SeasoningRun run = new SeasoningRun(board, RULES, budget(FlavorAxis.HEAT_COOLING, 4));
        SeasoningBot.play(run);
        assertEquals(0, run.x(), "it kept walking after the mine");
        assertEquals(1, run.effects().size());
    }

    @Test
    void sameInputsAlwaysGiveTheSameEffectsOnAGeneratedBoard() {
        SeasoningBoard board = BoardGenerator.generate(1234L, BoardLayout.DEFAULT);
        SeasoningRun first = new SeasoningRun(board, RULES, budget(FlavorAxis.SWEET_BITTER, 8));
        SeasoningRun second = new SeasoningRun(board, RULES, budget(FlavorAxis.SWEET_BITTER, 8));
        SeasoningBot.play(first);
        SeasoningBot.play(second);
        assertEquals(first.effects(), second.effects());
        assertEquals(first.x(), second.x());
        assertEquals(first.y(), second.y());
    }
}
