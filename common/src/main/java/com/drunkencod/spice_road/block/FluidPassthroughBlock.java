package com.drunkencod.spice_road.block;

import org.jetbrains.annotations.Nullable;

import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;

/**
 * A block flowing fluid passes through as if it weren't there, holding the
 * passing fluid in its own blockstate instead of being replaced by it.
 * <p>
 * Vanilla's fluid ticking replaces any block holding a non-source fluid with
 * that fluid's own block, or with air once the flow dries up. A mixin on
 * {@code FlowingFluid#tick} asks this block for its updated state instead.
 */
public interface FluidPassthroughBlock {

    /**
     * @param state The block's current state.
     * @param fluid The fluid vanilla's fluid ticking wants at this position -
     *              empty once the flow has dried up.
     * @return {@code state} holding {@code fluid}, or {@code null} to let
     *         vanilla replace the block with {@code fluid}'s own block as usual.
     */
    @Nullable
    BlockState withPassingFluid(BlockState state, FluidState fluid);
}
