package com.drunkencod.spice_road.grinder;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Test;

import com.mojang.serialization.JsonOps;

import com.drunkencod.spice_road.grinder.DiscoveryLog.Entry;

class DiscoveryLogTest {

    private static Entry entry(long seed, int cell) {
        return new Entry(seed, 7, cell);
    }

    @Test
    void remembersCellsPerBoard() {
        DiscoveryLog log = new DiscoveryLog();
        log.record(entry(1, 10), 100);
        log.record(entry(1, 20), 100);
        log.record(entry(2, 30), 100);
        assertEquals(Set.of(10, 20), log.revealed(1, 7));
        assertEquals(Set.of(30), log.revealed(2, 7));
        assertTrue(log.revealed(3, 7).isEmpty());
    }

    @Test
    void aDifferentLayoutIsADifferentBoard() {
        DiscoveryLog log = new DiscoveryLog();
        log.record(entry(1, 10), 100);
        assertTrue(log.revealed(1, 8).isEmpty());
    }

    @Test
    void forgetsTheOldestEntriesPastTheLimit() {
        DiscoveryLog log = new DiscoveryLog();
        for (int cell = 0; cell < 5; cell++)
            log.record(entry(1, cell), 3);
        assertEquals(List.of(entry(1, 2), entry(1, 3), entry(1, 4)), log.entries());
    }

    @Test
    void lockingACellInAgainMakesItTheNewest() {
        DiscoveryLog log = new DiscoveryLog();
        log.record(entry(1, 0), 3);
        log.record(entry(1, 1), 3);
        log.record(entry(1, 2), 3);
        log.record(entry(1, 0), 3);
        log.record(entry(1, 3), 3);
        assertEquals(List.of(entry(1, 2), entry(1, 0), entry(1, 3)), log.entries());
    }

    @Test
    void aLimitOfZeroKeepsNothingAndTrimmingDropsTheOldest() {
        DiscoveryLog log = new DiscoveryLog();
        log.record(entry(1, 0), 0);
        assertTrue(log.isEmpty());
        for (int cell = 0; cell < 4; cell++)
            log.record(entry(1, cell), 10);
        log.trim(2);
        assertEquals(List.of(entry(1, 2), entry(1, 3)), log.entries());
    }

    @Test
    void recordingReportsWhetherTheCellWasNew() {
        DiscoveryLog log = new DiscoveryLog();
        assertTrue(log.record(entry(1, 0), 3));
        assertFalse(log.record(entry(1, 0), 3));
        for (int cell = 1; cell <= 3; cell++)
            log.record(entry(1, cell), 3);
        assertTrue(log.record(entry(1, 0), 3), "a forgotten cell is new again");
        assertTrue(log.record(entry(1, 9), 0));
        assertTrue(log.record(entry(1, 9), 0), "with no memory every cell is new");
    }

    @Test
    void entriesSurviveAnEncodeAndParseRoundTrip() {
        Entry entry = new Entry(-7256293617404057666L, -12345, 40);
        var json = Entry.CODEC.encodeStart(JsonOps.INSTANCE, entry).getOrThrow();
        assertEquals(entry, Entry.CODEC.parse(JsonOps.INSTANCE, json).getOrThrow());
    }
}
