package com.drunkencod.spice_road.mixin.bee;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.llamalad7.mixinextras.sugar.Local;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.animal.Bee;

import com.drunkencod.spice_road.block.SpicePollination;

/**
 * Keeps a bee from returning to a remembered Spice plant once it no longer
 * may be pollinated (see {@link SpicePollination}), e.g. after being
 * harvested. Finding new flowers is gated by {@link BeePollinateGoalMixin}.
 */
@Mixin(Bee.class)
public abstract class BeeMixin {

    @ModifyReturnValue(method = "isFlowerValid", at = @At("RETURN"))
    private boolean spice_road$requireRipeSpice(boolean valid, @Local(argsOnly = true) BlockPos pos) {
        return valid && SpicePollination.mayPollinate(((Bee) (Object) this).level().getBlockState(pos));
    }
}
