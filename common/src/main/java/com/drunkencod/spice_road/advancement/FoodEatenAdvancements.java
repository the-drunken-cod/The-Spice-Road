package com.drunkencod.spice_road.advancement;

import java.util.Set;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Item;

import com.drunkencod.spice_road.event.FoodEatenListeners;
import com.drunkencod.spice_road.registry.ModDataComponents;

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
            Set<Item> contributors = event.stack().getOrDefault(ModDataComponents.FLAVOR_CONTRIBUTORS.get(),
                    Set.of());
            ModCriteriaTriggers.FOOD_EATEN.get().trigger(player, event.stack(), event.profile(), contributors);
        });
    }
}
