package com.drunkencod.spice_road.spice;

import com.drunkencod.spice_road.Constants;

/**
 * One of a fixed, closed set of 8 bipolar spectrums a {@link SpiceProfile} is
 * scored on, each a single value from +1 (its positive pole) to -1 (its
 * negative pole).
 */
public enum FlavorAxis {

    /** Fiery (+1) / cooling (-1). */
    HEAT_COOLING("heat_cooling", 0xFF5533, 0x55CCFF),
    /** Sweet (+1) / bitter (-1). */
    SWEET_BITTER("sweet_bitter", 0xFF88CC, 0xB08850),
    /** Tart (+1) / rounded (-1). */
    SOUR_MELLOW("sour_mellow", 0xDDEE33, 0xE8C890),
    /** Earthy (+1) / floral (-1). */
    EARTHY_FLORAL("earthy_floral", 0xA0703C, 0xCC88FF),
    /** Bark/wood (+1) / fresh herb (-1). */
    WOODY_GREEN("woody_green", 0xC08A5A, 0x55DD55),
    /** Sinus-sharp (+1) / soft (-1). */
    PUNGENT_SOFT("pungent_soft", 0xFFAA00, 0xFFC8D8),
    /** Resin/pine (+1) / clean (-1). */
    RESINOUS_CLEAN("resinous_clean", 0xD4A017, 0xD8F0FF),
    /** Umami (+1) / airy (-1). */
    SAVORY_DELICATE("savory_delicate", 0xCC6644, 0xE8D8FF);

    protected String axisId;
    protected int positiveColor;
    protected int negativeColor;

    /**
     * @param id            Identifier used in translation keys
     * @param positiveColor Display color ({@code 0xRRGGBB}) of the positive pole
     * @param negativeColor Display color ({@code 0xRRGGBB}) of the negative pole
     */
    FlavorAxis(String id, int positiveColor, int negativeColor) {
        this.axisId = id;
        this.positiveColor = positiveColor;
        this.negativeColor = negativeColor;
    }

    public String getId() {
        return this.axisId;
    }

    /** @return Display color ({@code 0xRRGGBB}) of the positive pole (+1). */
    public int getPositiveColor() {
        return this.positiveColor;
    }

    /** @return Display color ({@code 0xRRGGBB}) of the negative pole (-1). */
    public int getNegativeColor() {
        return this.negativeColor;
    }

    /**
     * @param positive Whether to return the positive or negative pole's color
     * @return Display color ({@code 0xRRGGBB}) of the given pole
     */
    public int getColor(boolean positive) {
        return positive ? this.positiveColor : this.negativeColor;
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
