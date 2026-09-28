package com.drunkencod.spice_road.block;

import java.util.function.Supplier;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;

import com.drunkencod.spice_road.spice.Spice;

/**
 * Worldgen-only counterpart of {@link SpiceCropBlock} - the block
 * {@code SpicePlantFeature} actually places, surviving on natural
 * {@link SpicePlantBlock#SPICE_GROWABLE} ground instead of requiring
 * farmland. Kept as a separate block so the farmed crop (planted from seeds)
 * stays farmland-only and correctly breaks when its farmland is trampled,
 * while this variant keeps growing on the dirt/sand/terracotta it generated
 * on.
 * <p>
 * Shares the farmed crop's loot table and stage models 1:1 (see
 * {@code SpicePlants}) - only its ground check differs.
 */
public class WildSpiceCropBlock extends SpiceCropBlock {

    public WildSpiceCropBlock(BlockBehaviour.Properties properties, Supplier<? extends ItemLike> seedItem,
            Spice spice) {
        super(properties, seedItem, spice);
    }

    @Override
    protected boolean mayPlaceOn(BlockState state, BlockGetter level, BlockPos pos) {
        return super.mayPlaceOn(state, level, pos) || state.is(SPICE_GROWABLE);
    }
}
