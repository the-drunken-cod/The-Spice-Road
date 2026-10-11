package com.drunkencod.spice_road.spice.effect;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.food.FoodData;

import com.drunkencod.spice_road.event.FoodEatenListeners;
import com.drunkencod.spice_road.registry.ModDataComponents;
import com.drunkencod.spice_road.spice.Seasoning;

/**
 * Applies a seasoned food's Seasoning Effects to whoever eats it, through
 * {@link FoodEatenListeners}. A player also gets the food's saturation
 * modifier, see {@link SeasoningSaturation}.
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
            if (seasoning != null && !seasoning.effects().isEmpty())
                SeasoningEffects.apply(event.entity(), seasoning);
            if (event.entity() instanceof Player player)
                addSaturation(player, SeasoningSaturation.modifier(event.stack(), event.foodProperties()));
        });
    }

    private static void addSaturation(Player player, double modifier) {
        if (modifier == 0D)
            return;
        FoodData foodData = player.getFoodData();
        float saturation = foodData.getSaturationLevel() + (float) modifier;
        if (modifier > 0D)
            saturation = Math.min(SaturationCap.limit(foodData.getFoodLevel()), saturation);
        foodData.setSaturation(Math.max(0F, saturation));
    }
}
