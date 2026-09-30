package com.drunkencod.spice_road.spice;

/**
 * A season a {@link Spice} grows in. Only used for compatibility with season
 * mods (e.g. Serene Seasons' {@code <season>_crops} block tags); the mod
 * itself has no seasons.
 */
public enum Season {

    SPRING("spring"),
    SUMMER("summer"),
    AUTUMN("autumn"),
    WINTER("winter");

    private final String id;

    Season(String id) {
        this.id = id;
    }

    /** @return This season's ID, e.g. {@code "autumn"}. */
    public String getId() {
        return id;
    }
}
