package com.drunkencod.spice_road.spice;

/**
 * A biome's climate, derived from its temperature/humidity, but overridable
 * per-biome via datapack tags. All Spices sharing a Climate value form that
 * Climate's Climate Bucket.
 */
public enum Climate {

    TROPICAL,
    TEMPERATE,
    ARID,
    COLD
}
