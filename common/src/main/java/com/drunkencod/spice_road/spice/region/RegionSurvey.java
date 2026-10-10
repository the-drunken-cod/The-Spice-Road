package com.drunkencod.spice_road.spice.region;

import java.util.EnumMap;
import java.util.Map;

import org.jetbrains.annotations.Nullable;

import com.drunkencod.spice_road.spice.Spice;

/**
 * Tally of what a walk over Spice Region cells found: how many cells ended in
 * each {@link HeartOutcome}, and how many {@link HeartOutcome#HEART}s each
 * Spice got. Pure bookkeeping, so the numbers it derives can be tested without
 * a world.
 */
public final class RegionSurvey {

    private final Map<HeartOutcome, Integer> outcomes = new EnumMap<>(HeartOutcome.class);
    private final Map<Spice, Integer> hearts = new EnumMap<>(Spice.class);
    private final int cellsTotal;
    private int cellsVisited;

    /** @param cellsTotal How many cells the walk set out to visit. */
    public RegionSurvey(int cellsTotal) {
        this.cellsTotal = cellsTotal;
    }

    /**
     * Counts one visited cell.
     *
     * @param outcome What the cell amounts to.
     * @param spice   The cell's Heart Spice, required for {@link HeartOutcome#HEART}
     *                and ignored otherwise.
     */
    public void record(HeartOutcome outcome, @Nullable Spice spice) {
        cellsVisited++;
        outcomes.merge(outcome, 1, Integer::sum);
        if (outcome == HeartOutcome.HEART && spice != null)
            hearts.merge(spice, 1, Integer::sum);
    }

    /** @return How many cells the walk set out to visit. */
    public int cellsTotal() {
        return cellsTotal;
    }

    /** @return How many cells were visited so far. */
    public int cellsVisited() {
        return cellsVisited;
    }

    /** @return Whether every cell was visited, i.e. the walk wasn't cut short. */
    public boolean isComplete() {
        return cellsVisited >= cellsTotal;
    }

    /**
     * @param outcome An outcome.
     * @return How many visited cells ended in it.
     */
    public int count(HeartOutcome outcome) {
        return outcomes.getOrDefault(outcome, 0);
    }

    /**
     * @param spice A Spice.
     * @return How many Region Hearts have it as their Heart Spice.
     */
    public int heartsOf(Spice spice) {
        return hearts.getOrDefault(spice, 0);
    }

    /** @return How many Region Hearts a player can be sent to, whatever their Spice. */
    public int heartCount() {
        return count(HeartOutcome.HEART);
    }

    /**
     * @param spice A Spice.
     * @return Its share of all Region Hearts, from {@code 0.0} to {@code 1.0}.
     */
    public double shareOf(Spice spice) {
        return heartCount() == 0 ? 0D : (double) heartsOf(spice) / heartCount();
    }

    /**
     * @param spice A Spice.
     * @return On average, how many visited cells per Region Heart of that
     *         Spice, or {@link Double#POSITIVE_INFINITY} if it got none.
     */
    public double cellsPerHeartOf(Spice spice) {
        int count = heartsOf(spice);
        return count == 0 ? Double.POSITIVE_INFINITY : (double) cellsVisited / count;
    }
}
