package com.drunkencod.spice_road.spice.effect;

import com.drunkencod.spice_road.event.FoodEatenListeners;
import com.drunkencod.spice_road.registry.ModDataComponents;
import com.drunkencod.spice_road.spice.Seasoning;

/**
 * Applies a seasoned food's Seasoning Effects to whoever eats it, through
 * {@link FoodEatenListeners}.
 */
public final class SeasoningEffectApplier {

    private SeasoningEffectApplier() {
    }

    /** Registers this listener with {@link FoodEatenListeners}. */
    public static void register() {
        FoodEatenListeners.register(event -> {
            if (event.level().isClientSide())
                return;
            Seasoning seasoning = event.stack().get(ModDataComponents.SEASONING.get());
            if (seasoning != null)
                SeasoningEffects.apply(event.entity(), seasoning);
        });
    }
}
