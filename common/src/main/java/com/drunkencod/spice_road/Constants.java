package com.drunkencod.spice_road;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Constants {

    public static final String MOD_ID = "spice_road";
    public static final String MOD_NAME = "The Spice Road";
    public static final Logger LOG = LoggerFactory.getLogger(MOD_NAME);

    /**
     * Default growth stage count (highest age value) for
     * {@code FLOWER_PATCH}/{@code CROP} Spice Plants. Shared source of truth
     * between {@code IConfigHelper}'s config spec definitions and code that
     * needs the default without reading a possibly-unloaded config (see
     * {@link #DEFAULT_SPICE_PLANT_HARVEST_YIELD}).
     */
    public static final int DEFAULT_SPICE_PLANT_GROWTH_STAGES = 4;

    /**
     * Default flat harvest yield (item count) for {@code FLOWER_PATCH}/{@code CROP}
     * Spice Plants. Loot table datagen bakes this value in directly rather than
     * reading it from {@code IConfigHelper} at datagen time, since a pure
     * {@code runData} pass may run before the mod's config file is actually loaded.
     * Re-run datagen after changing this default to regenerate the loot tables.
     */
    public static final int DEFAULT_SPICE_PLANT_HARVEST_YIELD = 1;
}
