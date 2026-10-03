package com.drunkencod.spice_road.spice.board;

/**
 * What a cell of the Seasoning Board is.
 */
public enum CellKind {

    /** Nothing there; the pawn can stand on it. */
    BARE,
    /** Blocks movement. */
    WALL,
    /** Holds a bundle of effects a player can lock in. */
    EFFECT,
    /** Forces a bane onto the food when stepped on. */
    MINE
}
