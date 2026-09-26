package com.drunkencod.spice_road.spice;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.world.level.biome.Biome;

/**
 * A biome's climate, derived from its temperature/humidity, but overridable
 * per-biome via datapack tags. All Spices sharing a Climate value form that
 * Climate's Climate Bucket.
 */
public enum Climate {

    TROPICAL,
    TEMPERATE,
    ARID,
    COLD;

    /**
     * Derives a {@link Climate} from a biome's temperature/precipitation.
     * <p>
     * Heuristic, not a precise scientific mapping - cold biomes are simply
     * ones below the snow-line temperature threshold; among the rest, "no
     * precipitation" (deserts, badlands, etc.) reads as {@link #ARID} and
     * everything else splits on temperature into {@link #TEMPERATE} /
     * {@link #TROPICAL}.
     * <p>
     * Datapack tag overrides (e.g. {@code spice_road:biome/climate/arid})
     * are not implemented here yet. Callers that need the override should check
     * those tags first and only fall back to this method if none apply.
     *
     * @param biome The biome to derive climate from.
     * @param pos   The position within that biome (precipitation can vary
     *              with height, e.g. snow at altitude).
     * @return The derived {@link Climate}.
     */
    public static Climate fromBiome(Holder<Biome> biome, BlockPos pos) {
        Biome value = biome.value();
        float temperature = value.getBaseTemperature();

        if (temperature < 0.15F) {
            return COLD;
        }
        if (value.getPrecipitationAt(pos) == Biome.Precipitation.NONE) {
            return ARID;
        }
        return temperature < 0.8F ? TEMPERATE : TROPICAL;
    }
}
