package com.drunkencod.spice_road.spice.board;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.mojang.serialization.JsonOps;

class BoardLayoutTest {

    @Test
    void theDefaultLayoutSurvivesAnEncodeAndParseRoundTrip() {
        JsonElement json = BoardLayout.CODEC.encodeStart(JsonOps.INSTANCE, BoardLayout.DEFAULT).getOrThrow();
        assertEquals(BoardLayout.DEFAULT, BoardLayout.CODEC.parse(JsonOps.INSTANCE, json).getOrThrow());
        assertTrue(json.getAsJsonObject().has("walls"), "the shipped file spells out every quota");
    }

    @Test
    void listsMustHaveOneEntryPerAxisRing() {
        JsonElement json = BoardLayout.CODEC.encodeStart(JsonOps.INSTANCE, BoardLayout.DEFAULT).getOrThrow();
        json.getAsJsonObject().add("effect_cells", JsonParser.parseString("[1, 1]"));
        assertTrue(BoardLayout.CODEC.parse(JsonOps.INSTANCE, json).error().isPresent());
    }

    @Test
    void minesNeedAtLeastOneRingWithAWeight() {
        JsonElement json = BoardLayout.CODEC.encodeStart(JsonOps.INSTANCE, BoardLayout.DEFAULT).getOrThrow();
        json.getAsJsonObject().add("mine_weights", JsonParser.parseString("[0, 0, 0]"));
        assertTrue(BoardLayout.CODEC.parse(JsonOps.INSTANCE, json).error().isPresent());
    }

    @Test
    void aBundleCannotBeEmptyOrHaveMoreMinimumThanMaximumBoons() {
        for (String bad : List.of(
                "{\"min_boons\": 0, \"max_boons\": 0, \"banes\": 0, \"random\": 0, \"level\": 1}",
                "{\"min_boons\": 2, \"max_boons\": 1, \"banes\": 1, \"random\": 0, \"level\": 1}")) {
            assertTrue(BoardLayout.BundleSpec.CODEC.parse(JsonOps.INSTANCE, JsonParser.parseString(bad)).error()
                    .isPresent(), bad);
        }
    }
}
