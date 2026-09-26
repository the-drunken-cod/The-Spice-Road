package com.drunkencod.spice_road.block;

import java.util.function.Supplier;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;

import com.drunkencod.spice_road.spice.Spice;

/**
 * {@code CROP} template for spices.
 */
public class SpiceCropBlock extends SpicePlantBlock {

    public SpiceCropBlock(BlockBehaviour.Properties properties, Supplier<? extends ItemLike> seedItem, Spice spice) {
        super(properties, seedItem, spice);
    }

    /**
     * Ground-type check only - see {@link #canGrow} for Spice Region gating.
     */
    @Override
    public boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        return super.canSurvive(state, level, pos);
    }

    /**
     * Gates growth by Spice Region support - see
     * {@link Spice#canBeCultivatedAt(ServerLevel, BlockPos)}. A Spice that
     * fails this check isn't destroyed (see {@link #canSurvive}) - it's simply
     * never allowed to advance past stage 0.
     */
    @Override
    public boolean canGrow(ServerLevel level, BlockPos pos) {
        return getSpice().canBeCultivatedAt(level, pos);
    }
}
