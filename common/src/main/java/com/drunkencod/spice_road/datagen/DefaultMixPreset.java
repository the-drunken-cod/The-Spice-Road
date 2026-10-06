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
    PUMPKIN_SPICE("pumpkin_spice", 1, Map.of(Spice.CINNAMON, 1, Spice.NUTMEG, 1, Spice.CLOVE, 1, Spice.ALLSPICE, 1));

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
