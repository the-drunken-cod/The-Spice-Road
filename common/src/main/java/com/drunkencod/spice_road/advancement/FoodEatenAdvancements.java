package com.drunkencod.spice_road.advancement;

import net.minecraft.server.level.ServerPlayer;

import com.drunkencod.spice_road.event.FoodEatenListeners;

/**
 * Bridges {@link FoodEatenListeners} to {@link ModCriteriaTriggers#FOOD_EATEN}.
 */
public final class FoodEatenAdvancements {

    private FoodEatenAdvancements() {
    }

    /** Registers this bridge with {@link FoodEatenListeners}. */
    public static void register() {
        FoodEatenListeners.register(event -> {
            if (event.level().isClientSide() || !(event.entity() instanceof ServerPlayer player))
                return;
            ModCriteriaTriggers.FOOD_EATEN.get().trigger(player, event.stack(), event.profile());
        });
    }
}
