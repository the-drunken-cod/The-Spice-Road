package com.drunkencod.spice_road.soil;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Optional;

import org.junit.jupiter.api.Test;

import com.drunkencod.spice_road.spice.Spice;
import com.drunkencod.spice_road.spice.Tier;

class SoilAnalysisTest {

    private static final int ANY_TIER = Tier.EPIC.ordinal();

    @Test
    void noSpiceMeansNoneRegardlessOfFound() {
        assertEquals(SoilAnalysis.Outcome.NONE, SoilAnalysis.outcome(Optional.empty(), ANY_TIER, true));
        assertEquals(SoilAnalysis.Outcome.NONE, SoilAnalysis.outcome(Optional.empty(), ANY_TIER, false));
    }

    @Test
    void foundSpiceIsNamed() {
        assertEquals(SoilAnalysis.Outcome.FOUND, SoilAnalysis.outcome(Optional.of(Spice.WASABI), ANY_TIER, true));
    }

    @Test
    void unfoundSpiceIsUnknown() {
        assertEquals(SoilAnalysis.Outcome.UNKNOWN, SoilAnalysis.outcome(Optional.of(Spice.WASABI), ANY_TIER, false));
    }

    @Test
    void spiceAboveTheTierCapIsUnknownEvenWhenFound() {
        int cap = Spice.WASABI.getTier().ordinal() - 1;
        assertEquals(SoilAnalysis.Outcome.UNKNOWN, SoilAnalysis.outcome(Optional.of(Spice.WASABI), cap, true));
        assertEquals(SoilAnalysis.Outcome.FOUND,
                SoilAnalysis.outcome(Optional.of(Spice.WASABI), Spice.WASABI.getTier().ordinal(), true));
    }
}
