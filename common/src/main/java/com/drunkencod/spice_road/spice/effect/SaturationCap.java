package com.drunkencod.spice_road.spice.effect;

import com.drunkencod.spice_road.platform.Services;

/**
 * The most saturation a player can hold. Vanilla caps it at the food level; if
 * the config allows, the bonus saturation of seasoned food may exceed that by a
 * configured amount.
 */
public final class SaturationCap {

    private SaturationCap() {
    }

    /**
     * @param foodLevel The player's food level.
     * @return The most saturation the player can hold at that food level:
     *         {@code foodLevel}, plus the configured overcap if the cap bypass
     *         is enabled.
     */
    public static float limit(int foodLevel) {
        if (!Services.CONFIG.getSeasoningBypassSaturationCap())
            return foodLevel;
        return (float) (foodLevel + Services.CONFIG.getSeasoningSaturationOvercap());
    }
}
