package com.drunkencod.spice_road.spice.board;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.Test;

import com.drunkencod.spice_road.spice.FlavorAxis;
import com.drunkencod.spice_road.spice.SpiceProfile;
import com.drunkencod.spice_road.spice.board.EffectSlot.SlotKind;
import com.drunkencod.spice_road.spice.effect.EffectKind;
import com.drunkencod.spice_road.spice.effect.SeasoningEffect;
import com.drunkencod.spice_road.spice.effect.TestCatalog;

class SeasoningRunTest {

    private static final SeasoningRules RULES = new SeasoningRules(1.0, 1.5, 2.5, 3.5, 4, TestCatalog.full());

    /** A board of bare cells with the given cells replaced. */
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

    private static PointsLedger budget(Map<FlavorAxis, Double> values) {
        double[] raw = new double[FlavorAxis.values().length];
        values.forEach((axis, value) -> raw[axis.ordinal()] = value);
        return PointsLedger.start(new SpiceProfile(raw), value -> value);
    }

    private static Map<FlavorAxis, Double> axes(Object... pairs) {
        Map<FlavorAxis, Double> map = new EnumMap<>(FlavorAxis.class);
        for (int i = 0; i < pairs.length; i += 2)
            map.put((FlavorAxis) pairs[i], ((Number) pairs[i + 1]).doubleValue());
        return map;
    }

    private static SeasoningEffect effect(FlavorAxis axis, boolean positive, EffectKind kind, int level) {
        return new SeasoningEffect(TestCatalog.id(axis, positive, kind), level);
    }

    @Test
    void aStepPaysTheStepCostFromTheAxisItsDirectionIsBoundTo() {
        SeasoningRun run = new SeasoningRun(board(Map.of()), RULES, budget(axes(FlavorAxis.HEAT_COOLING, 2.5)));
        assertTrue(run.move(Direction.UP_LEFT).moved());
        assertTrue(run.move(Direction.UP_LEFT).moved());
        assertEquals(0.5, run.points(FlavorAxis.HEAT_COOLING), 1e-9);
        assertFalse(run.canMove(Direction.UP_LEFT));
        assertFalse(run.move(Direction.UP_LEFT).moved());
        assertEquals(2, BoardGeometry.CENTER - run.x());
        assertFalse(run.canMove(Direction.UP), "the sweet axis has no points");
    }

    @Test
    void wallsAndTheBoardEdgeBlockButOnlyTheTargetCellDoes() {
        SeasoningBoard board = board(Map.of(
                BoardGeometry.index(4, 3), at(4, 3, CellKind.WALL),
                BoardGeometry.index(3, 4), at(3, 4, CellKind.WALL)));
        SeasoningRun run = new SeasoningRun(board, RULES, budget(axes(
                FlavorAxis.SWEET_BITTER, 20, FlavorAxis.HEAT_COOLING, 20, FlavorAxis.EARTHY_FLORAL, 20)));
        assertFalse(run.canMove(Direction.UP));
        assertFalse(run.canMove(Direction.LEFT));
        assertTrue(run.canMove(Direction.UP_LEFT), "cutting the corner between two walls is allowed");
        for (int i = 0; i < 4; i++)
            run.move(Direction.UP_LEFT);
        assertEquals(0, run.x());
        assertFalse(run.canMove(Direction.UP_LEFT), "the board edge blocks");
    }

    @Test
    void retreatingNeedsPointsOnTheOppositeAxis() {
        SeasoningRun run = new SeasoningRun(board(Map.of()), RULES, budget(axes(FlavorAxis.HEAT_COOLING, 5)));
        run.move(Direction.UP_LEFT);
        assertFalse(run.canMove(Direction.DOWN_RIGHT));
        SeasoningRun withRetreat = new SeasoningRun(board(Map.of()), RULES,
                budget(axes(FlavorAxis.HEAT_COOLING, 5, FlavorAxis.SAVORY_DELICATE, 1)));
        withRetreat.move(Direction.UP_LEFT);
        assertTrue(withRetreat.move(Direction.DOWN_RIGHT).moved());
        assertEquals(BoardGeometry.CENTER, withRetreat.x());
        assertEquals(0D, withRetreat.points(FlavorAxis.SAVORY_DELICATE), 1e-9);
    }

    @Test
    void lockingInTakesTheWholeBundleOfThePolesAndSpendsTheZoneAxis() {
        SeasoningBoard board = board(Map.of(BoardGeometry.index(2, 2), at(2, 2, CellKind.EFFECT,
                new EffectSlot(SlotKind.BOON, 1, 7), new EffectSlot(SlotKind.BANE, 1, 9))));
        SeasoningRun run = new SeasoningRun(board, RULES, budget(axes(FlavorAxis.HEAT_COOLING, 10)));
        assertFalse(run.canLockIn(), "not standing on it yet");
        run.move(Direction.UP_LEFT);
        run.move(Direction.UP_LEFT);
        assertTrue(run.canLockIn());
        assertTrue(run.lockIn());
        assertEquals(10 - 2 - 1.5, run.points(FlavorAxis.HEAT_COOLING), 1e-9);
        assertEquals(Set.of(
                effect(FlavorAxis.HEAT_COOLING, true, EffectKind.BANE, 1),
                effect(FlavorAxis.HEAT_COOLING, true, EffectKind.BOON, 1)), Set.copyOf(run.effects()));
        assertFalse(run.canLockIn(), "a cell can only be locked in once");
        assertTrue(run.isLockedIn(2, 2));
    }

    @Test
    void theSignOfTheFoodsValueDecidesWhichPoleATheZoneGives() {
        SeasoningBoard board = board(Map.of(BoardGeometry.index(2, 2), at(2, 2, CellKind.EFFECT,
                new EffectSlot(SlotKind.BOON, 1, 0))));
        SeasoningRun run = new SeasoningRun(board, RULES, budget(axes(FlavorAxis.HEAT_COOLING, -10)));
        run.move(Direction.UP_LEFT);
        run.move(Direction.UP_LEFT);
        run.lockIn();
        assertEquals(List.of(effect(FlavorAxis.HEAT_COOLING, false, EffectKind.BOON, 1)), run.effects());
    }

    @Test
    void lockingInIsRefusedWithoutEnoughPointsOrWithoutAPole() {
        SeasoningBoard board = board(Map.of(BoardGeometry.index(2, 2), at(2, 2, CellKind.EFFECT,
                new EffectSlot(SlotKind.BOON, 1, 0))));
        SeasoningRun poor = new SeasoningRun(board, RULES, budget(axes(FlavorAxis.HEAT_COOLING, 3.4)));
        poor.move(Direction.UP_LEFT);
        poor.move(Direction.UP_LEFT);
        assertFalse(poor.lockIn(), "1.4 points left, 1.5 needed");
        assertTrue(poor.effects().isEmpty());

        // The sweet, earthy and savory axes get the pawn there without any heat points, so heat has no pole.
        SeasoningRun poleless = new SeasoningRun(board, RULES,
                budget(axes(FlavorAxis.SWEET_BITTER, 3, FlavorAxis.EARTHY_FLORAL, 3)));
        poleless.move(Direction.UP);
        poleless.move(Direction.UP);
        poleless.move(Direction.LEFT);
        poleless.move(Direction.LEFT);
        assertEquals(2, poleless.x());
        assertEquals(2, poleless.y());
        assertFalse(poleless.canLockIn());
    }

    @Test
    void aBundleThatDoesNotFitTheEffectCapCannotBeLockedIn() {
        SeasoningRules tight = new SeasoningRules(1.0, 1.5, 2.5, 3.5, 1, TestCatalog.full());
        SeasoningBoard board = board(Map.of(BoardGeometry.index(2, 2), at(2, 2, CellKind.EFFECT,
                new EffectSlot(SlotKind.BOON, 1, 7), new EffectSlot(SlotKind.BANE, 1, 9))));
        SeasoningRun run = new SeasoningRun(board, tight, budget(axes(FlavorAxis.HEAT_COOLING, 10)));
        run.move(Direction.UP_LEFT);
        run.move(Direction.UP_LEFT);
        assertFalse(run.canLockIn(), "two distinct effects don't fit one slot");
    }

    @Test
    void aMineForcesItsBaneOnceAndOnlyWhenTheZoneHasAPole() {
        SeasoningBoard board = board(Map.of(BoardGeometry.index(2, 2), at(2, 2, CellKind.MINE,
                new EffectSlot(SlotKind.BANE, 2, 3))));
        SeasoningRun run = new SeasoningRun(board, RULES,
                budget(axes(FlavorAxis.HEAT_COOLING, 5, FlavorAxis.SAVORY_DELICATE, 5)));
        run.move(Direction.UP_LEFT);
        SeasoningRun.MoveResult hit = run.move(Direction.UP_LEFT);
        assertTrue(hit.mineHit());
        assertEquals(List.of(effect(FlavorAxis.HEAT_COOLING, true, EffectKind.BANE, 2)), run.effects());
        run.move(Direction.DOWN_RIGHT);
        assertFalse(run.move(Direction.UP_LEFT).mineHit(), "a mine only goes off once");
        assertEquals(2, run.effects().get(0).level());

        // Reaching the mine through other axes leaves the heat axis without a pole: the mine is a dud.
        SeasoningRun poleless = new SeasoningRun(board, RULES,
                budget(axes(FlavorAxis.SWEET_BITTER, 3, FlavorAxis.EARTHY_FLORAL, 3)));
        poleless.move(Direction.UP);
        poleless.move(Direction.UP);
        poleless.move(Direction.LEFT);
        assertTrue(poleless.move(Direction.LEFT).mineHit());
        assertTrue(poleless.effects().isEmpty());
    }

    @Test
    void aMineIsForcedEvenWhenTheFoodIsFullAndReplacesTheWeakestBoon() {
        SeasoningRules tight = new SeasoningRules(1.0, 1.5, 2.5, 3.5, 1, TestCatalog.full());
        SeasoningBoard board = board(Map.of(
                BoardGeometry.index(2, 2), at(2, 2, CellKind.EFFECT, new EffectSlot(SlotKind.BOON, 1, 0)),
                BoardGeometry.index(1, 1), at(1, 1, CellKind.MINE, new EffectSlot(SlotKind.BANE, 2, 3))));
        SeasoningRun run = new SeasoningRun(board, tight, budget(axes(FlavorAxis.HEAT_COOLING, 10)));
        run.move(Direction.UP_LEFT);
        run.move(Direction.UP_LEFT);
        assertTrue(run.lockIn());
        assertEquals(List.of(effect(FlavorAxis.HEAT_COOLING, true, EffectKind.BOON, 1)), run.effects());
        assertTrue(run.move(Direction.UP_LEFT).mineHit());
        assertEquals(List.of(effect(FlavorAxis.HEAT_COOLING, true, EffectKind.BANE, 2)), run.effects());
    }
}
