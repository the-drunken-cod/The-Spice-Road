package com.drunkencod.spice_road.compat.appleskin;

import net.neoforged.neoforge.common.NeoForge;

import squeek.appleskin.api.event.FoodValuesEvent;

import com.drunkencod.spice_road.spice.effect.SeasoningSaturation;

/**
 * Optional AppleSkin integration. Adds a seasoned food's bonus saturation, see
 * {@link SeasoningSaturation}, to the food values AppleSkin displays. Only
 * touch this class if AppleSkin is installed.
 */
public final class AppleSkinCompat {

    private AppleSkinCompat() {
    }

    /** Registers the food values listener. Must be called once during client setup. */
    public static void register() {
        NeoForge.EVENT_BUS.addListener(AppleSkinCompat::onFoodValues);
    }

    private static void onFoodValues(FoodValuesEvent event) {
        event.modifiedFoodProperties = SeasoningSaturation.withBonus(event.itemStack, event.modifiedFoodProperties);
    }
}
