package com.drunkencod.spice_road.spice;

import org.jetbrains.annotations.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.biome.Biome;

/**
 * A biome's climate, derived from its temperature/humidity, but overridable
 * per-biome via datapack tags. All Spices sharing a Climate value form that
 * Climate's Climate Bucket.
 */
public enum Climate {

    // temp > 0.8, precip > 0:
    TROPICAL("tropical"),
    // temp <= 0.7, precip > 0:
    TEMPERATE("temperate"),
    // temp >= 0.15, precip = 0:
    ARID("arid"),
    // temp < 0.15, precip >= 0:
    COLD("cold");

    private final String id;

    Climate(String id) {
        this.id = id;
    }

    /** @return This Climate's ID, e.g. {@code "tropical"}. */
    public String getId() {
        return id;
    }

    /**
     * @return The translation key of this Climate's plain name (e.g.
     *         {@code "Tropical"}).
     */
    public String getTranslationKey() {
        return "climate.spice_road." + id;
    }

    /**
     * @return This Climate's translatable plain name. See
     *         {@link #getTranslationKey()}.
     */
    public Component getDisplayName() {
        return Component.translatable(getTranslationKey());
    }

    // #region static

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

        if (temperature < 0.15F)
            return COLD;
        if (value.getPrecipitationAt(pos) == Biome.Precipitation.NONE)
            return ARID;
        return temperature <= 0.8F ? TEMPERATE : TROPICAL;
    }

    /**
     * Looks up a Climate by its {@link #getId() ID}.
     *
     * @param id The Climate's ID, e.g. {@code "tropical"}.
     * @return The matching Climate, or {@code null} if there is none.
     */
    public static @Nullable Climate byId(String id) {
        for (Climate climate : values()) {
            if (climate.id.equals(id))
                return climate;
        }
        return null;
    }
}
