package com.drunkencod.spice_road.spice.board;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import net.minecraft.resources.ResourceLocation;

import com.drunkencod.spice_road.spice.FlavorAxis;
import com.drunkencod.spice_road.spice.board.EffectSlot.SlotKind;
import com.drunkencod.spice_road.spice.effect.EffectKind;
import com.drunkencod.spice_road.spice.effect.SeasoningEffect;
import com.drunkencod.spice_road.spice.effect.SeasoningEffects;

/**
 * One run on a {@link SeasoningBoard}: the pawn, the points left on every axis,
 * the cells locked in or blown up so far and the effects gathered. The same
 * rules drive a player's run in the Spice Grinder and the automatic bot, so a
 * route always gives the same effects. Mutable, validates every action.
 */
public final class SeasoningRun {

    private static final double EPSILON = 1e-9;

    private final SeasoningBoard board;
    private final SeasoningRules rules;
    private final SpiceBudget budget;
    private final double[] points = new double[FlavorAxis.values().length];
    private final Set<Integer> lockedIn = new HashSet<>();
    private final Set<Integer> detonated = new HashSet<>();
    private List<SeasoningEffect> effects = List.of();
    private int x = BoardGeometry.CENTER;
    private int y = BoardGeometry.CENTER;

    /**
     * What a step did.
     *
     * @param moved   Whether the pawn moved at all.
     * @param mineHit Whether it stepped on a mine that hadn't gone off yet.
     */
    public record MoveResult(boolean moved, boolean mineHit) {
    }

    /**
     * Starts a run with the pawn on the centre cell.
     *
     * @param board  The board to play.
     * @param rules  The costs and catalog to play by.
     * @param budget What the food has to spend.
     */
    public SeasoningRun(SeasoningBoard board, SeasoningRules rules, SpiceBudget budget) {
        this.board = board;
        this.rules = rules;
        this.budget = budget;
        for (FlavorAxis axis : FlavorAxis.values())
            points[axis.ordinal()] = budget.points(axis);
    }

    /** @return The pawn's column. */
    public int x() {
        return x;
    }

    /** @return The pawn's row. */
    public int y() {
        return y;
    }

    /**
     * @param axis A Flavor Axis.
     * @return The points left on {@code axis}.
     */
    public double points(FlavorAxis axis) {
        return points[axis.ordinal()];
    }

    /** @return The effects gathered so far, in canonical order. */
    public List<SeasoningEffect> effects() {
        return effects;
    }

    /**
     * @param cellX A column on the board.
     * @param cellY A row on the board.
     * @return Whether the player locked in the cell at {@code (cellX, cellY)}.
     */
    public boolean isLockedIn(int cellX, int cellY) {
        return lockedIn.contains(BoardGeometry.index(cellX, cellY));
    }

    /**
     * @param direction A direction.
     * @return Whether the pawn can step that way: the target is on the board
     *         and not a wall, and the direction's axis can pay the step.
     */
    public boolean canMove(Direction direction) {
        int targetX = x + direction.dx();
        int targetY = y + direction.dy();
        return BoardGeometry.inBounds(targetX, targetY)
                && board.cell(targetX, targetY).kind() != CellKind.WALL
                && points(direction.axis()) + EPSILON >= rules.stepCost();
    }

    /**
     * Steps the pawn, if it can. Stepping on a mine that hasn't gone off forces
     * its bane onto the food.
     *
     * @param direction The direction to step in.
     * @return What the step did.
     */
    public MoveResult move(Direction direction) {
        if (!canMove(direction))
            return new MoveResult(false, false);
        points[direction.axis().ordinal()] -= rules.stepCost();
        x += direction.dx();
        y += direction.dy();
        Cell cell = board.cell(x, y);
        if (cell.kind() != CellKind.MINE || !detonated.add(BoardGeometry.index(x, y)))
            return new MoveResult(true, false);
        triggerMine(cell);
        return new MoveResult(true, true);
    }

    /**
     * @return Whether the pawn can lock in the cell it stands on: it is an
     *         effect cell not locked in yet, its zone's axis has a pole and
     *         enough points, and its effects fit within the food's effect cap.
     */
    public boolean canLockIn() {
        Cell cell = board.cell(x, y);
        if (cell.kind() != CellKind.EFFECT || lockedIn.contains(BoardGeometry.index(x, y)))
            return false;
        int pole = budget.pole(cell.zone());
        if (pole == 0 || points(cell.zone()) + EPSILON < rules.lockInCost(cell.ring()))
            return false;
        List<SeasoningEffect> bundle = resolve(cell, pole > 0);
        if (bundle.isEmpty())
            return false;
        Set<ResourceLocation> added = new HashSet<>();
        for (SeasoningEffect effect : bundle) {
            if (effects.stream().noneMatch(existing -> existing.id().equals(effect.id())))
                added.add(effect.id());
        }
        return effects.size() + added.size() <= rules.maxEffects();
    }

    /**
     * Spends the points and takes the whole bundle of the cell the pawn stands on.
     *
     * @return Whether it could be locked in, see {@link #canLockIn()}.
     */
    public boolean lockIn() {
        if (!canLockIn())
            return false;
        Cell cell = board.cell(x, y);
        points[cell.zone().ordinal()] -= rules.lockInCost(cell.ring());
        lockedIn.add(BoardGeometry.index(x, y));
        for (SeasoningEffect effect : resolve(cell, budget.pole(cell.zone()) > 0))
            effects = SeasoningEffects.gain(effects, effect, false, rules.maxEffects(), rules.catalog());
        return true;
    }

    private void triggerMine(Cell cell) {
        int pole = budget.pole(cell.zone());
        if (pole == 0)
            return;
        for (SeasoningEffect effect : resolve(cell, pole > 0))
            effects = SeasoningEffects.gain(effects, effect, true, rules.maxEffects(), rules.catalog());
    }

    /** Resolves a cell's slots to catalog entries for a pole; slots with no candidate are skipped. */
    private List<SeasoningEffect> resolve(Cell cell, boolean positive) {
        List<SeasoningEffect> resolved = new ArrayList<>();
        for (EffectSlot slot : cell.slots()) {
            pick(slot, cell.zone(), positive).ifPresent(id -> resolved.add(new SeasoningEffect(id, slot.level())));
        }
        return resolved;
    }

    private Optional<ResourceLocation> pick(EffectSlot slot, FlavorAxis axis, boolean positive) {
        if (slot.kind() == SlotKind.RANDOM) {
            List<ResourceLocation> pool = rules.catalog().randomPool();
            long total = 0;
            for (ResourceLocation id : pool)
                total += rules.catalog().randomWeight(id);
            if (total <= 0)
                return Optional.empty();
            long roll = Long.remainderUnsigned(slot.selector(), total);
            for (ResourceLocation id : pool) {
                roll -= rules.catalog().randomWeight(id);
                if (roll < 0)
                    return Optional.of(id);
            }
            return Optional.empty();
        }
        EffectKind kind = slot.kind() == SlotKind.BOON ? EffectKind.BOON : EffectKind.BANE;
        List<ResourceLocation> candidates = rules.catalog().poleEntries(axis, positive, kind);
        if (candidates.isEmpty())
            return Optional.empty();
        return Optional.of(candidates.get((int) Long.remainderUnsigned(slot.selector(), candidates.size())));
    }
}
