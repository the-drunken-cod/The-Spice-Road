package com.drunkencod.spice_road.block;

import net.minecraft.world.item.ItemNameBlockItem;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.FarmBlock;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Seeds of a {@link SpicePlantBlock}. Can only be planted on farmland.
 * <p>
 * For {@code CROP} Spices, the planted {@link SpiceCropBlock} only survives
 * on farmland - {@link WildSpiceCropBlock} is the separate, worldgen-only
 * variant that survives on {@link SpicePlantBlock#SPICE_GROWABLE} ground.
 * {@code FLOWER_PATCH} Spices don't have that split yet: the same
 * {@link FlowerPatchBlock} instance is both planted from seeds and generated
 * by worldgen, so it still survives on farmland and
 * {@link SpicePlantBlock#SPICE_GROWABLE} ground alike.
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
