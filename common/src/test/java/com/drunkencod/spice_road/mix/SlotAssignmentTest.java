package com.drunkencod.spice_road.mix;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class SlotAssignmentTest {

    @Test
    void singleSpicePerSlotNeedsExactCounts() {
        boolean[][] fits = { { true, false }, { false, true } };
        assertTrue(SlotAssignment.exact(new int[] { 2, 4 }, new int[] { 2, 4 }, fits));
        assertFalse(SlotAssignment.exact(new int[] { 3, 3 }, new int[] { 2, 4 }, fits));
    }

    @Test
    void tagSlotAcceptsAnyCombinationOfItsMembers() {
        // Spices: cassia, cinnamon; one slot (the tag) taking 2.
        boolean[][] fits = { { true }, { true } };
        assertTrue(SlotAssignment.exact(new int[] { 2, 0 }, new int[] { 2 }, fits));
        assertTrue(SlotAssignment.exact(new int[] { 1, 1 }, new int[] { 2 }, fits));
        assertFalse(SlotAssignment.exact(new int[] { 1, 2 }, new int[] { 2 }, fits));
    }

    @Test
    void spiceOutsideEverySlotNeverFits() {
        boolean[][] fits = { { true }, { false } };
        assertFalse(SlotAssignment.exact(new int[] { 1, 1 }, new int[] { 2 }, fits));
    }

    @Test
    void overlappingSlotsUseWhicheverAssignmentFits() {
        // Spice 0 fits both slots, spice 1 only slot 1: 0 must go to slot 0.
        boolean[][] fits = { { true, true }, { false, true } };
        assertTrue(SlotAssignment.exact(new int[] { 1, 1 }, new int[] { 1, 1 }, fits));
        // Both spices fit only slot 1, so slot 0 can never be filled.
        boolean[][] crowded = { { false, true }, { false, true } };
        assertFalse(SlotAssignment.exact(new int[] { 1, 1 }, new int[] { 1, 1 }, crowded));
    }
}
