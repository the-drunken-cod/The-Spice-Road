package com.drunkencod.spice_road.spice.effect;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;

import net.minecraft.resources.ResourceLocation;

import com.drunkencod.spice_road.spice.FlavorAxis;

class SeasoningEffectsTest {

    private static final EffectCatalog CATALOG = TestCatalog.full();

    private static final SeasoningEffect HEAT_BOON = new SeasoningEffect(
            TestCatalog.id(FlavorAxis.HEAT_COOLING, true, EffectKind.BOON), 1);
    private static final SeasoningEffect SWEET_BOON = new SeasoningEffect(
            TestCatalog.id(FlavorAxis.SWEET_BITTER, true, EffectKind.BOON), 1);
    private static final SeasoningEffect HEAT_BANE = new SeasoningEffect(
            TestCatalog.id(FlavorAxis.HEAT_COOLING, true, EffectKind.BANE), 1);
    private static final SeasoningEffect SOUR_BANE = new SeasoningEffect(
            TestCatalog.id(FlavorAxis.SOUR_MELLOW, true, EffectKind.BANE), 1);

    private static SeasoningEffect level(SeasoningEffect effect, int level) {
        return new SeasoningEffect(effect.id(), level);
    }

    @Test
    void aFoodIsFlawlessWithBoonsAndNoBanes() {
        assertTrue(SeasoningEffects.isFlawless(List.of(HEAT_BOON, SWEET_BOON), CATALOG));
        assertFalse(SeasoningEffects.isFlawless(List.of(HEAT_BOON, SOUR_BANE), CATALOG));
        assertFalse(SeasoningEffects.isFlawless(List.of(), CATALOG), "no effects isn't flawless");
    }

    @Test
    void anEntryMissingFromTheCatalogCountsAsABoon() {
        SeasoningEffect unknown = new SeasoningEffect(ResourceLocation.parse("spice_road:gone"), 1);
        assertTrue(SeasoningEffects.isFlawless(List.of(unknown), CATALOG));
    }

    @Test
    void theSameEntryMergesIntoOneEffectOfAHigherLevelUpToItsMaximum() {
        List<SeasoningEffect> once = SeasoningEffects.gain(List.of(HEAT_BOON), HEAT_BOON, false, 4, CATALOG);
        assertEquals(List.of(level(HEAT_BOON, 2)), once);
        List<SeasoningEffect> capped = SeasoningEffects.gain(List.of(level(HEAT_BOON, 3)), level(HEAT_BOON, 2),
                false, 4, CATALOG);
        assertEquals(List.of(level(HEAT_BOON, TestCatalog.MAX_LEVEL)), capped);
    }

    @Test
    void aNewEntryIsAddedWhileThereIsRoomAndRefusedOnceTheFoodIsFull() {
        assertEquals(List.of(HEAT_BOON, SWEET_BOON),
                SeasoningEffects.gain(List.of(HEAT_BOON), SWEET_BOON, false, 2, CATALOG));
        assertEquals(List.of(HEAT_BOON, SWEET_BOON),
                SeasoningEffects.gain(List.of(HEAT_BOON, SWEET_BOON), SOUR_BANE, false, 2, CATALOG));
    }

    @Test
    void aForcedEffectReplacesTheWeakestBoon() {
        List<SeasoningEffect> result = SeasoningEffects.gain(List.of(level(HEAT_BOON, 2), SWEET_BOON), SOUR_BANE,
                true, 2, CATALOG);
        assertEquals(List.of(level(HEAT_BOON, 2), SOUR_BANE), result);
    }

    @Test
    void aForcedEffectReplacesTheWeakestBaneWhenThereIsNoBoon() {
        List<SeasoningEffect> result = SeasoningEffects.gain(List.of(level(HEAT_BANE, 2), level(SOUR_BANE, 1)),
                new SeasoningEffect(TestCatalog.id(FlavorAxis.SWEET_BITTER, true, EffectKind.BANE), 1), true, 2,
                CATALOG);
        assertEquals(2, result.size());
        assertEquals(level(HEAT_BANE, 2), result.get(0));
    }

    @Test
    void entriesMissingFromTheCatalogCountAsUnlimitedBoons() {
        SeasoningEffect unknown = new SeasoningEffect(ResourceLocation.fromNamespaceAndPath("test", "gone"), 1);
        assertEquals(Optional.empty(), CATALOG.kind(unknown.id()));
        assertEquals(List.of(level(unknown, 9)),
                SeasoningEffects.gain(List.of(level(unknown, 5)), level(unknown, 4), false, 4, CATALOG));
    }

    @Test
    void levelsScaleAmplifierOrDurationAsTheEntryDeclares() {
        assertEquals(2, SeasoningEffects.amplifier(LevelScaling.AMPLIFIER, 3));
        assertEquals(100, SeasoningEffects.durationTicks(100, LevelScaling.AMPLIFIER, 3, 1.0));
        assertEquals(0, SeasoningEffects.amplifier(LevelScaling.DURATION, 3));
        assertEquals(300, SeasoningEffects.durationTicks(100, LevelScaling.DURATION, 3, 1.0));
        assertEquals(150, SeasoningEffects.durationTicks(100, LevelScaling.AMPLIFIER, 1, 1.5));
        assertEquals(1, SeasoningEffects.durationTicks(100, LevelScaling.AMPLIFIER, 1, 0.0),
                "never shorter than a tick");
    }
}
