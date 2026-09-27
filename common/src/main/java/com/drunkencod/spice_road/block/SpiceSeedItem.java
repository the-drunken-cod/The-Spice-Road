package com.drunkencod.spice_road.block;

import net.minecraft.world.item.ItemNameBlockItem;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.FarmBlock;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Seeds of a {@link SpicePlantBlock}. Can only be planted on farmland, even
 * though the planted Spice Plant also survives on
 * {@link SpicePlantBlock#SPICE_GROWABLE} ground, where only wild ones
 * generate.
 */
public class SpiceSeedItem extends ItemNameBlockItem {

    /**
     * @param block      The Spice Plant block these seeds plant.
     * @param properties Item properties.
     */
    public SpiceSeedItem(Block block, Properties properties) {
        super(block, properties);
    }

    @Override
    protected boolean canPlace(BlockPlaceContext context, BlockState state) {
        return context.getLevel().getBlockState(context.getClickedPos().below()).getBlock() instanceof FarmBlock
                && super.canPlace(context, state);
    }
}
