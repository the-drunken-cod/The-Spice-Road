package com.drunkencod.spice_road.datagen;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

import com.drunkencod.spice_road.spice.Spice;

/**
 * The Mix Presets the mod ships, written to
 * {@code data/spice_road/mix_preset/} and given their own item models by
 * datagen. Datapacks may add, change or remove presets; this only seeds the
 * defaults.
 */
public enum DefaultMixPreset {
    BENGALI("bengali", 1,
            Map.of(Spice.FENUGREEK, 1, Spice.NIGELLA, 1, Spice.CUMIN, 1, Spice.MUSTARD, 1, Spice.FENNEL, 1)),
    BIRYANI("biryani", 2,
            Map.of(Spice.CORIANDER, 1, Spice.CARDAMOM, 1, Spice.FENNEL, 1, Spice.CORIANDER, 1, Spice.CARAWAY, 1,
                    Spice.CLOVE, 1)),
    CAJUN("cajun", 3, Map.of(Spice.HABANERO, 1, Spice.GARLIC, 1, Spice.LONG_PEPPER, 1, Spice.THYME, 1)),
    CURRY("curry", 4,
            Map.of(Spice.CURRY, 1, Spice.CUMIN, 1, Spice.CORIANDER, 1, Spice.TURMERIC, 1, Spice.FENNEL, 1,
                    Spice.FENUGREEK, 1)),
    HOLIDAY("holiday", 5,
            Map.of(Spice.TONKA, 1, Spice.STAR_ANISE, 1, Spice.VANILLA, 1, Spice.LICORICE, 1, Spice.CINNAMON, 1)),
    MEDITERRANEAN("mediterranean", 6,
            Map.of(Spice.THYME, 1, Spice.OREGANO, 1, Spice.ROSEMARY, 1, Spice.PARSLEY, 1, Spice.BASIL, 1)),
    MEXICAN("mexican", 7,
            Map.of(Spice.OREGANO, 1, Spice.HABANERO, 1, Spice.CUMIN, 1, Spice.CORIANDER, 1)),
    PERSIAN("persian", 8, Map.of(
            Spice.SAFFRON, 1, Spice.TURMERIC, 1, Spice.CINNAMON, 1, Spice.LONG_PEPPER, 1, Spice.CARDAMOM, 1)),
    PUMPKIN_SPICE("pumpkin_spice", 9,
            Map.of(Spice.CINNAMON, 1, Spice.NUTMEG, 1, Spice.CLOVE, 1, Spice.ALLSPICE, 1, Spice.GINGER, 1)),
    RAS_EL_HANOUT("ras_el_hanout", 10, Map.of(
            Spice.CARDAMOM, 1, Spice.CUMIN, 1, Spice.CLOVE, 1, Spice.CINNAMON, 1, Spice.NUTMEG, 1, Spice.ALLSPICE, 1,
            Spice.LONG_PEPPER, 1, Spice.MASTIC, 1)),
    SALAD("salad", 11, Map.of(Spice.TURMERIC, 1, Spice.PARSLEY, 1, Spice.DILL, 1, Spice.GARLIC, 1)),
    ZAATAR("zaatar", 12, Map.of(Spice.THYME, 1, Spice.SUMAC, 1, Spice.OREGANO, 1, Spice.SESAME, 1, Spice.CLOVE, 1));

    private final String id;
    private final int customModelData;
    private final Map<Spice, Integer> spices;

    DefaultMixPreset(String id, int customModelData, Map<Spice, Integer> spices) {
        this.id = id;
        this.customModelData = customModelData;
        this.spices = Collections.unmodifiableMap(new LinkedHashMap<>(spices));
    }

    /** @return The preset's ID path, also the name of its texture and model. */
    public String getId() {
        return id;
    }

    /**
     * @return The {@code custom_model_data} its mixes are given; unique among
     *         presets, as it picks the model override.
     */
    public int getCustomModelData() {
        return customModelData;
    }

    /** @return The proportions, by the raw item of each Spice. */
    public Map<Spice, Integer> getSpices() {
        return spices;
    }
}
