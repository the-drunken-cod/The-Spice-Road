package com.drunkencod.spice_road.stats;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.UUID;

import org.junit.jupiter.api.Test;

import com.drunkencod.spice_road.spice.Spice;

class SpiceFindingsTest {

    private static final UUID ALICE = UUID.randomUUID();
    private static final UUID BOB = UUID.randomUUID();

    @Test
    void aSpiceIsNewOnlyTheFirstTime() {
        SpiceFindings findings = new SpiceFindings();
        assertTrue(findings.add(ALICE, Spice.VANILLA));
        assertFalse(findings.add(ALICE, Spice.VANILLA));
        assertTrue(findings.add(ALICE, Spice.SAFFRON));
        assertEquals(2, findings.count(ALICE));
    }

    @Test
    void playersAreCountedSeparately() {
        SpiceFindings findings = new SpiceFindings();
        findings.add(ALICE, Spice.VANILLA);
        assertTrue(findings.add(BOB, Spice.VANILLA));
        assertEquals(0, findings.count(UUID.randomUUID()));
    }
}
