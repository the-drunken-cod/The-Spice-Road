package com.drunkencod.spice_road.event;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

import com.drunkencod.spice_road.spice.SpiceProfile;

/**
 * Fired whenever a {@link LivingEntity} finishes eating a food item, on both
 * logical sides. See {@link FoodEatenListeners} for how to listen.
 * <p>
 * {@code profile} and {@code effectiveProfile} are {@link SpiceProfile#ZERO}
 * when {@code stack} isn't registered as a spice at all - listeners that
 * don't care about unseasoned food should bail out on {@link SpiceProfile#isZero()}.
 *
 * @param entity           The entity that finished eating.
 * @param level            {@code entity}'s level. {@code level.isClientSide()}
 *                         tells apart the two firings of this event.
 * @param stack            The stack that was eaten, as it was just before
 *                         being consumed.
 * @param foodProperties   {@code stack}'s {@code FoodProperties}.
 * @param profile          {@code stack}'s raw, uncapped {@link SpiceProfile}.
 * @param effectiveProfile {@code profile}'s Effective Profile (soft-capped).
 */
public record FoodEatenEvent(
        LivingEntity entity,
        Level level,
        ItemStack stack,
        FoodProperties foodProperties,
        SpiceProfile profile,
        SpiceProfile effectiveProfile) {
}
