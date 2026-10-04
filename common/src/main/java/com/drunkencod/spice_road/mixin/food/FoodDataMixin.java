package com.drunkencod.spice_road.mixin.food;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.minecraft.world.food.FoodData;

import com.drunkencod.spice_road.spice.effect.SaturationCap;

/**
 * Keeps saturation above the food level that bonus saturation of seasoned food
 * granted, see {@link SaturationCap}. Vanilla clamps saturation to the food
 * level whenever food is eaten, which would otherwise wipe that bonus with the
 * next meal. Eating still can't push saturation past the food level itself.
 */
@Mixin(FoodData.class)
public abstract class FoodDataMixin {

    @Shadow
    private int foodLevel;

    @Shadow
    private float saturationLevel;

    @Unique
    private float spice_road$saturationBefore;

    @Inject(method = "add(IF)V", at = @At("HEAD"))
    private void spice_road$rememberSaturation(int nutrition, float saturation, CallbackInfo ci) {
        spice_road$saturationBefore = saturationLevel;
    }

    @Inject(method = "add(IF)V", at = @At("RETURN"))
    private void spice_road$restoreOvercap(int nutrition, float saturation, CallbackInfo ci) {
        float kept = Math.min(spice_road$saturationBefore, SaturationCap.limit(foodLevel));
        if (kept > saturationLevel)
            saturationLevel = kept;
    }
}
