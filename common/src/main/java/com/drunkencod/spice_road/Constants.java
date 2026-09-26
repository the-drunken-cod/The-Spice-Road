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
     * Default harvest yield multiplier for {@code FLOWER_PATCH}/{@code CROP}
     * Spice Plants. Loot table datagen bakes this value in directly rather than
     * reading it from {@code IConfigHelper} at datagen time, since a pure
     * {@code runData} pass may run before the mod's config file is actually loaded.
     * Re-run datagen after changing this default to regenerate the loot tables.
     */
    public static final double DEFAULT_SPICE_PLANT_HARVEST_YIELD_MULTIPLIER = 1.0D;

    /**
     * Highest growth stage (age value, starting at 0) of fruiting Spice Tree
     * leaves. Fixed rather than configurable, since it defines the leaves'
     * blockstate property range, which is frozen at block registration.
     */
    public static final int SPICE_TREE_LEAF_GROWTH_STAGES = 2;

    /**
     * Default harvest yield multiplier for Spice Trees, applied to
     * {@code Spice#getDropAmount()} when stripping bark or picking fruiting
     * leaves.
     */
    public static final double DEFAULT_SPICE_TREE_HARVEST_YIELD_MULTIPLIER = 1.0D;
}
