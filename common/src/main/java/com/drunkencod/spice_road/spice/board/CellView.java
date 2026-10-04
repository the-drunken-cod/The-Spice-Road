package com.drunkencod.spice_road.spice.board;

import java.util.List;

import com.drunkencod.spice_road.spice.effect.SeasoningEffect;

/**
 * What one player may see of one cell of the {@link SeasoningBoard}. This, not
 * the board, is what is sent to the client, so hidden information (which blank
 * looking cell is a mine, what an unrevealed effect cell holds) never leaves
 * the server.
 *
 * @param kind        How the cell looks.
 * @param effectCount For an {@link Kind#UNKNOWN} cell, how many effects it
 *                    holds; otherwise {@code 0}.
 * @param boons       For an {@link Kind#UNKNOWN} cell within
 *                    {@value CellViews#HINT_RANGE} cells of the pawn, how many
 *                    of them are boons; {@code -1} if unknown.
 * @param banes       Like {@code boons}, for banes.
 * @param randoms     Like {@code boons}, for vanilla-random effects.
 * @param effects     For a {@link Kind#REVEALED} cell, the effects it holds.
 * @param lockedIn    Whether the player locked the cell in during this run.
 */
public record CellView(Kind kind, int effectCount, int boons, int banes, int randoms, List<SeasoningEffect> effects,
        boolean lockedIn) {

    /** A blank-looking cell, as drawn for bare cells and mines that haven't gone off. */
    public static final CellView BLANK = new CellView(Kind.BLANK, 0, -1, -1, -1, List.of(), false);

    /** A wall. */
    public static final CellView WALL = new CellView(Kind.WALL, 0, -1, -1, -1, List.of(), false);

    /** A mine that already went off. */
    public static final CellView SPENT_MINE = new CellView(Kind.SPENT_MINE, 0, -1, -1, -1, List.of(), false);

    /**
     * @param effects The cell's effects, copied.
     */
    public CellView {
        effects = List.copyOf(effects);
    }

    /** How a cell looks. */
    public enum Kind {
        /** Nothing is known about it, or nothing is there. */
        BLANK,
        /** Blocks movement. */
        WALL,
        /** An effect cell whose content isn't known; shown as {@code ???}. */
        UNKNOWN,
        /** An effect cell whose content is known. */
        REVEALED,
        /** A mine that went off. */
        SPENT_MINE
    }
}
