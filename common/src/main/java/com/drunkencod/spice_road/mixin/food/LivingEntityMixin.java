package com.drunkencod.spice_road.mixin.food;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

import com.drunkencod.spice_road.event.FoodEatenEvent;
import com.drunkencod.spice_road.event.FoodEatenListeners;

/**
 * Fires {@link FoodEatenListeners} from the single vanilla method both
 * {@code Player} and non-player food consumption funnel through, mirroring
 * how {@link com.drunkencod.spice_road.mixin.flavor.AbstractCookingRecipeMixin}
 * hooks recipe assembly instead of per-loader events.
 */
@Mixin(LivingEntity.class)
public abstract class LivingEntityMixin {

    @Inject(method = "eat(Lnet/minecraft/world/level/Level;Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/food/FoodProperties;)Lnet/minecraft/world/item/ItemStack;", at = @At("HEAD"))
    private void spice_road$fireFoodEaten(Level level, ItemStack itemStack, FoodProperties foodProperties,
            CallbackInfoReturnable<ItemStack> cir) {
        FoodEatenListeners.fire(new FoodEatenEvent((LivingEntity) (Object) this, level, itemStack, foodProperties));
    }
}
