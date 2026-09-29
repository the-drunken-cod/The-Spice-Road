package com.drunkencod.spice_road.event;

/**
 * A listener registered with {@link FoodEatenListeners}, notified every time
 * a {@link FoodEatenEvent} fires.
 */
@FunctionalInterface
public interface FoodEatenListener {

    /**
     * @param event The event to handle.
     */
    void onFoodEaten(FoodEatenEvent event);
}
