package com.drunkencod.spice_road.spice;

import com.drunkencod.spice_road.Constants;

/**
 * One of a fixed, closed set of 8 bipolar spectrums a {@link SpiceProfile} is
 * scored on, each a single value from +1 (its positive pole) to -1 (its
 * negative pole).
 */
public enum FlavorAxis {

    /** Fiery (+1) / cooling (-1). */
    HEAT_COOLING("heat_cooling"),
    /** Sweet (+1) / bitter (-1). */
    SWEET_BITTER("sweet_bitter"),
    /** Tart (+1) / rounded (-1). */
    SOUR_MELLOW("sour_mellow"),
    /** Earthy (+1) / floral (-1). */
    EARTHY_FLORAL("earthy_floral"),
    /** Bark/wood (+1) / fresh herb (-1). */
    WOODY_GREEN("woody_green"),
    /** Sinus-sharp (+1) / soft (-1). */
    PUNGENT_SOFT("pungent_soft"),
    /** Resin/pine (+1) / clean (-1). */
    RESINOUS_CLEAN("resinous_clean"),
    /** Umami (+1) / airy (-1). */
    SAVORY_DELICATE("savory_delicate");

    protected String axisId;

    FlavorAxis(String id) {
        this.axisId = id;
    }

    public String getId() {
        return this.axisId;
    }

    /** Translation key for this axis' positive-pole (+1) display name. */
    public String positiveTranslationKey() {
        return Constants.MOD_ID + ".spice_axis.name." + this.axisId + ".positive";
    }

    /** Translation key for this axis' negative-pole (-1) display name. */
    public String negativeTranslationKey() {
        return Constants.MOD_ID + ".spice_axis.name." + this.axisId + ".negative";
    }
}
