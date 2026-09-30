package com.drunkencod.spice_road.mixin.bee;

import java.util.function.Predicate;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.minecraft.world.level.block.state.BlockState;

import com.drunkencod.spice_road.block.SpicePollination;

/**
 * Narrows the flowers a bee searches for to ripe Spice plants (see
 * {@link SpicePollination}), by wrapping the goal's flower predicate once it's
 * built rather than targeting the synthetic lambda it's built from.
 */
@Mixin(targets = "net.minecraft.world.entity.animal.Bee$BeePollinateGoal")
public abstract class BeePollinateGoalMixin {

    @Shadow
    @Final
    @Mutable
    private Predicate<BlockState> VALID_POLLINATION_BLOCKS;

    @Inject(method = "<init>", at = @At("TAIL"))
    private void spice_road$requireRipeSpice(CallbackInfo ci) {
        VALID_POLLINATION_BLOCKS = VALID_POLLINATION_BLOCKS.and(SpicePollination::mayPollinate);
    }
}
