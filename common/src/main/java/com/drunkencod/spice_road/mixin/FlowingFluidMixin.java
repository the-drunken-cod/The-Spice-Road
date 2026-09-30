package com.drunkencod.spice_road.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FlowingFluid;

import com.drunkencod.spice_road.block.FluidPassthroughBlock;

/**
 * Lets flowing fluid pass through a {@link FluidPassthroughBlock}: where
 * vanilla's fluid ticking would replace the block holding a non-source fluid
 * with that fluid's own block, or with air once it dries up, the block is
 * kept and only the fluid it holds is updated. Everything else about the
 * tick - rescheduling, neighbor updates, spreading onward - stays vanilla's.
 * <p>
 * The block may decline (e.g. when disabled by config), in which case the
 * original replacement happens unchanged.
 */
@Mixin(FlowingFluid.class)
public abstract class FlowingFluidMixin {

    @WrapOperation(method = "tick", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/level/Level;setBlock(Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;I)Z"))
    private boolean spice_road$keepFluidPassthroughBlock(Level level, BlockPos pos, BlockState newState, int flags,
            Operation<Boolean> original) {
        BlockState current = level.getBlockState(pos);
        if (current.getBlock() instanceof FluidPassthroughBlock block) {
            BlockState held = block.withPassingFluid(current, newState.getFluidState());
            if (held != null)
                return original.call(level, pos, held, flags);
        }
        return original.call(level, pos, newState, flags);
    }
}
