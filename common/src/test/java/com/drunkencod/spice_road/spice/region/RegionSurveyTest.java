package com.drunkencod.spice_road.spice.region;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import com.drunkencod.spice_road.spice.Spice;
import com.drunkencod.spice_road.spice.Tier;

class RegionSurveyTest {

    @Test
    void countsOutcomesAndHeartsPerSpice() {
        RegionSurvey survey = new RegionSurvey(5);
        survey.record(HeartOutcome.HEART, Spice.CINNAMON);
        survey.record(HeartOutcome.HEART, Spice.CINNAMON);
        survey.record(HeartOutcome.HEART, Spice.CLOVE);
        survey.record(HeartOutcome.SPICELESS, null);
        survey.record(HeartOutcome.BARREN_BIOME, null);

        assertEquals(5, survey.cellsVisited());
        assertEquals(3, survey.heartCount());
        assertEquals(1, survey.count(HeartOutcome.SPICELESS));
        assertEquals(0, survey.count(HeartOutcome.NO_GROVE_SITE));
        assertEquals(2, survey.heartsOf(Spice.CINNAMON));
        assertEquals(0, survey.heartsOf(Spice.ALLSPICE));
    }

    @Test
    void sharesAreRelativeToAllHeartsAndSpacingToVisitedCells() {
        RegionSurvey survey = new RegionSurvey(8);
        survey.record(HeartOutcome.HEART, Spice.CINNAMON);
        survey.record(HeartOutcome.HEART, Spice.CLOVE);
        for (int i = 0; i < 6; i++)
            survey.record(HeartOutcome.SPICELESS, null);

        assertEquals(0.5, survey.shareOf(Spice.CINNAMON), 1e-9);
        assertEquals(8D, survey.cellsPerHeartOf(Spice.CINNAMON), 1e-9);
        assertEquals(0D, survey.shareOf(Spice.ALLSPICE), 1e-9);
        assertEquals(Double.POSITIVE_INFINITY, survey.cellsPerHeartOf(Spice.ALLSPICE));
    }

    @Test
    void anEmptySurveyHasNoShares() {
        RegionSurvey survey = new RegionSurvey(0);
        assertEquals(0D, survey.shareOf(Spice.CINNAMON), 1e-9);
        assertTrue(survey.isComplete());
    }

    @Test
    void aWalkCutShortIsIncomplete() {
        RegionSurvey survey = new RegionSurvey(9);
        survey.record(HeartOutcome.SPICELESS, null);
        assertFalse(survey.isComplete());
        assertEquals(9, survey.cellsTotal());
    }

    @Test
    void ignoresTheSpiceOfACellWithoutAHeart() {
        RegionSurvey survey = new RegionSurvey(1);
        survey.record(HeartOutcome.NO_GROVE_SITE, Spice.CINNAMON);
        assertEquals(0, survey.heartsOf(Spice.CINNAMON));
    }

    @Test
    void tiersAreLookedUpByName() {
        assertEquals(Tier.EPIC, Tier.byName("epic"));
        assertNull(Tier.byName("legendary"));
    }
}
