package com.drunkencod.spice_road.spice.board;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

import org.junit.jupiter.api.Test;

class BoardSeedTest {

    @Test
    void isDeterministicAndDependsOnWorldSaltAndItem() {
        long base = BoardSeed.of(1L, 2L, "minecraft:bread");
        assertEquals(base, BoardSeed.of(1L, 2L, "minecraft:bread"));
        assertNotEquals(base, BoardSeed.of(9L, 2L, "minecraft:bread"));
        assertNotEquals(base, BoardSeed.of(1L, 3L, "minecraft:bread"));
        assertNotEquals(base, BoardSeed.of(1L, 2L, "minecraft:pumpkin_pie"));
    }

    @Test
    void aSaltOfZeroLeavesTheWorldSeedUnsalted() {
        assertEquals(BoardSeed.of(77L, 0L, "minecraft:bread"), BoardSeed.of(77L, 0L, "minecraft:bread"));
        assertNotEquals(BoardSeed.of(77L, 0L, "minecraft:bread"), BoardSeed.of(77L, 1L, "minecraft:bread"));
    }

    @Test
    void stringHashIsFnv1aAndStable() {
        assertEquals(0xCBF29CE484222325L, BoardSeed.hashString(""));
        assertEquals(0xAF63DC4C8601EC8CL, BoardSeed.hashString("a"));
    }
}
