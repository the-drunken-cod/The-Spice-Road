package com.drunkencod.spice_road.spice;

/**
 * One of a fixed, closed set of 8 bipolar spectrums a {@link SpiceProfile} is
 * scored on, each a single value from +1 (its positive pole) to -1 (its
 * negative pole).
 */
public enum FlavorAxis {

    /** Fiery (+1) / cooling (-1). */
    HEAT_COOLING,
    /** Sweet (+1) / bitter (-1). */
    SWEET_BITTER,
    /** Tart (+1) / rounded (-1). */
    SOUR_MELLOW,
    /** Earthy (+1) / floral (-1). */
    EARTHY_FLORAL,
    /** Bark/wood (+1) / fresh herb (-1). */
    WOODY_GREEN,
    /** Sinus-sharp (+1) / soft (-1). */
    PUNGENT_SOFT,
    /** Resin/pine (+1) / clean (-1). */
    RESINOUS_CLEAN,
    /** Umami (+1) / airy (-1). */
    SAVORY_DELICATE
}
