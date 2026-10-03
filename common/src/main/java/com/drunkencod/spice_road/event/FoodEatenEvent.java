package com.drunkencod.spice_road.event;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/**
 * Fired whenever a {@link LivingEntity} finishes eating a food item, on both
 * logical sides. See {@link FoodEatenListeners} for how to listen.
 *
 * @param entity         The entity that finished eating.
 * @param level          {@code entity}'s level. {@code level.isClientSide()}
 *                       tells apart the two firings of this event.
 * @param stack          The stack that was eaten, as it was just before being
 *                       consumed.
 * @param foodProperties {@code stack}'s {@code FoodProperties}.
 */
public record FoodEatenEvent(
        LivingEntity entity,
        Level level,
        ItemStack stack,
        FoodProperties foodProperties) {
}
