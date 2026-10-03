package com.drunkencod.spice_road.spice.board;

import com.drunkencod.spice_road.spice.FlavorAxis;

/**
 * The fixed, deliberately mediocre player behind Automatic Seasoning: a
 * straight-line walker. It picks the axis with the most points (ties by
 * {@link FlavorAxis} order) and walks in that axis' direction for as long as
 * it can pay and nothing blocks it, locking in every effect cell it lands on
 * and can afford. It never detours, never retreats and ignores mines and
 * anything a real player might know about the board.
 */
public final class SeasoningBot {

    private SeasoningBot() {
    }

    /**
     * Plays a whole run.
     *
     * @param run A fresh run, modified in place.
     */
    public static void play(SeasoningRun run) {
        FlavorAxis strongest = FlavorAxis.values()[0];
        for (FlavorAxis axis : FlavorAxis.values()) {
            if (run.points(axis) > run.points(strongest))
                strongest = axis;
        }
        Direction direction = Direction.of(strongest);
        while (run.canMove(direction)) {
            run.move(direction);
            if (run.canLockIn())
                run.lockIn();
        }
    }
}
