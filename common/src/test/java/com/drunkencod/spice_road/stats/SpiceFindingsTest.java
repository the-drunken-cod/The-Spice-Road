package com.drunkencod.spice_road.stats;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import net.minecraft.resources.ResourceLocation;

import com.drunkencod.spice_road.Constants;

class SpiceFindingsTest {

    private static final UUID ALICE = UUID.randomUUID();
    private static final UUID BOB = UUID.randomUUID();

    private static final ResourceLocation VANILLA = ResourceLocation.fromNamespaceAndPath(Constants.MOD_ID, "vanilla");
    private static final ResourceLocation SAFFRON = ResourceLocation.fromNamespaceAndPath(Constants.MOD_ID, "saffron");
    private static final ResourceLocation DATAPACK = ResourceLocation.fromNamespaceAndPath("some_pack", "pepper");

    @Test
    void aSpiceIsNewOnlyTheFirstTime() {
        SpiceFindings findings = new SpiceFindings();
        assertTrue(findings.add(ALICE, VANILLA));
        assertFalse(findings.add(ALICE, VANILLA));
        assertTrue(findings.add(ALICE, SAFFRON));
        assertEquals(2, findings.count(ALICE));
    }

    @Test
    void playersAreCountedSeparately() {
        SpiceFindings findings = new SpiceFindings();
        findings.add(ALICE, VANILLA);
        assertTrue(findings.add(BOB, VANILLA));
        assertEquals(0, findings.count(UUID.randomUUID()));
    }

    @Test
    void datapackSpicesAreRecordedLikeBuiltInOnes() {
        SpiceFindings findings = new SpiceFindings();
        assertTrue(findings.add(ALICE, DATAPACK));
        assertTrue(findings.itemsOf(ALICE).contains(DATAPACK));
    }

    @Test
    void allFoundNeedsEverySpiceAndAtLeastOne() {
        SpiceFindings findings = new SpiceFindings();
        List<ResourceLocation> all = List.of(VANILLA, SAFFRON, DATAPACK);
        assertFalse(findings.hasFoundAll(ALICE, all));
        findings.add(ALICE, VANILLA);
        findings.add(ALICE, SAFFRON);
        assertFalse(findings.hasFoundAll(ALICE, all));
        findings.add(ALICE, DATAPACK);
        assertTrue(findings.hasFoundAll(ALICE, all));
        assertFalse(findings.hasFoundAll(BOB, all));
        assertFalse(findings.hasFoundAll(ALICE, List.of()));
    }

    @Test
    void bareIdsFromOlderWorldsBelongToTheMod() {
        assertEquals(VANILLA, SpiceFindings.parseId("vanilla"));
        assertEquals(DATAPACK, SpiceFindings.parseId("some_pack:pepper"));
        assertNull(SpiceFindings.parseId("Not Valid"));
    }
}
