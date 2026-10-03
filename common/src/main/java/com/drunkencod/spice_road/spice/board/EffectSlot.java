package com.drunkencod.spice_road.spice.board;

/**
 * One effect of an effect cell's bundle (or a mine's bane), before it is
 * resolved against the catalog for a pole. The board layout fixes the slot's
 * kind, level and a selector, so which catalog entry it ends up as is a pure
 * function of the board and the pole.
 *
 * @param kind     What the slot draws from.
 * @param level    The level of the effect.
 * @param selector Picks among the catalog candidates, fixed per cell by the
 *                 board seed.
 */
public record EffectSlot(SlotKind kind, int level, long selector) {

    /**
     * What an {@link EffectSlot} draws its effect from.
     */
    public enum SlotKind {
        /** The zone axis pole's boons. */
        BOON,
        /** The zone axis pole's banes. */
        BANE,
        /** The vanilla-random pool. */
        RANDOM
    }
}
