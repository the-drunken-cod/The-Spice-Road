package com.drunkencod.spice_road.spice.region;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

import org.junit.jupiter.api.Test;

class SeededHashTest {

    @Test
    void isDeterministicAndSensitiveToEveryInput() {
        assertEquals(SeededHash.hash(1, 2, 3), SeededHash.hash(1, 2, 3));
        assertNotEquals(SeededHash.hash(1, 2, 3), SeededHash.hash(2, 2, 3));
        assertNotEquals(SeededHash.hash(1, 2, 3), SeededHash.hash(1, 3, 3));
        assertNotEquals(SeededHash.hash(1, 2, 3), SeededHash.hash(1, 2, 4));
    }
}
