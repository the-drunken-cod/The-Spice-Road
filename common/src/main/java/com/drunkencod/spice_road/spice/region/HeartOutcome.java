package com.drunkencod.spice_road.spice.region;

/**
 * What a Spice Region cell amounts to, for telling apart the reasons it has no
 * Region Heart a player can be sent to.
 */
public enum HeartOutcome {

    /** A non-barren Region Heart with a Heart Grove site. */
    HEART("heart"),
    /** The cell is a Spiceless Region. */
    SPICELESS("spiceless"),
    /** The cell isn't spiceless, but no Spice belongs to the Climate at its Heart. */
    NO_SPICE_IN_CLIMATE("no_spice_in_climate"),
    /** The Heart is in a heartless biome, and isn't an Aquatic Spice in an aquatic-friendly one. */
    BARREN_BIOME("barren_biome"),
    /** The Heart Spice has no plant, tree or Host Tree worldgen can place. */
    NO_WORLDGEN_PLANT("no_worldgen_plant"),
    /** The Heart has no ground for a Heart Grove within reach. */
    NO_GROVE_SITE("no_grove_site");

    private final String id;

    HeartOutcome(String id) {
        this.id = id;
    }

    /** @return This outcome's ID, e.g. {@code "barren_biome"}. */
    public String getId() {
        return id;
    }

    /** @return The translation key of this outcome's plain name. */
    public String getTranslationKey() {
        return "commands.spice_road.sample_spices.outcome." + id;
    }
}
