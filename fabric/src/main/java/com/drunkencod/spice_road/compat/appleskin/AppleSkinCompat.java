package com.drunkencod.spice_road.compat.appleskin;

import squeek.appleskin.api.AppleSkinApi;
import squeek.appleskin.api.event.FoodValuesEvent;

import com.drunkencod.spice_road.spice.effect.SeasoningSaturation;

/**
 * Optional AppleSkin integration, loaded through its {@code appleskin}
 * entrypoint, so it is never touched without AppleSkin installed. Adds a
 * seasoned food's bonus saturation, see {@link SeasoningSaturation}, to the
 * food values AppleSkin displays.
 */
public class AppleSkinCompat implements AppleSkinApi {

    @Override
    public void registerEvents() {
        FoodValuesEvent.EVENT.register(event -> event.modifiedFoodComponent = SeasoningSaturation
                .withBonus(event.itemStack, event.modifiedFoodComponent));
    }
}
