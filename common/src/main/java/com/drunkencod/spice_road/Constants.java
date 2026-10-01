package com.drunkencod.spice_road;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Mod-wide constants, including config defaults that code needs before the
 * config itself is loaded.
 */
public class Constants {

    /** The mod ID, also used as the registry and resource namespace. */
    public static final String MOD_ID = "spice_road";
    /** The mod's display name. */
    public static final String MOD_NAME = "The Spice Road";
    /** The mod's shared logger. */
    public static final Logger LOG = LoggerFactory.getLogger(MOD_NAME);

    /**
     * Growth stage count (highest age value) for {@code FLOWER_PATCH}/
     * {@code CROP} Spice Plants. Fixed rather than configurable: it defines the
     * blockstate property range, which is frozen at block registration, and
     * every stage needs its own model and texture to exist.
     */
    public static final int SPICE_PLANT_GROWTH_STAGES = 4;

    /**
     * Growth stage count (highest age value) for {@code BUSH} Spice Plants.
     * Fixed for the same reason as {@link #SPICE_PLANT_GROWTH_STAGES}.
     */
    public static final int SPICE_BUSH_GROWTH_STAGES = 2;

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
     * Highest ripening stage ({@code age} value, starting at 0) of Spice
     * Vines. Fixed rather than configurable for the same reason as
     * {@link #SPICE_TREE_LEAF_GROWTH_STAGES}.
     */
    public static final int SPICE_VINE_GROWTH_STAGES = 2;

    /**
     * Default harvest yield multiplier for Spice Trees, applied to
     * {@code Spice#getDropAmount()} when stripping bark or picking fruiting
     * leaves.
     */
    public static final double DEFAULT_SPICE_TREE_HARVEST_YIELD_MULTIPLIER = 1.0D;
}
