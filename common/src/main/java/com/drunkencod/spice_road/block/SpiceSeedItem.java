package com.drunkencod.spice_road.block;

import com.drunkencod.spice_road.spice.Spice;
import com.drunkencod.spice_road.spice.Tier;

import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemNameBlockItem;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Seeds of a {@link SpicePlantBlock}. Plantable wherever the seeded block
 * itself is allowed to stand (see {@link SpicePlantBlock#mayPlaceOn}) - this
 * defers to the block instead of hardcoding a ground check, so it stays in
 * sync per Source Type without needing its own farmland/{@link
 * SpicePlantBlock#SPICE_GROWABLE} split: farmland only for {@code CROP}
 * Spices via {@link SpiceCropBlock}, or farmland plus
 * {@code SPICE_GROWABLE} ground for {@code FLOWER_PATCH} Spices, which use
 * {@link SpicePlantBlock}'s wider default unmodified.
 */
public class SpiceSeedItem extends ItemNameBlockItem {

    /**
     * @param block      The Spice Plant block these seeds plant.
     * @param properties Item properties.
     */
    public SpiceSeedItem(Block block, Properties properties) {
        super(block, properties);
    }

    /**
     * @param spice The {@link Spice} these seeds plant.
     * @return Default item properties, rarity-coded by {@code spice}'s
     *         {@link Tier}.
     */
    public static Properties defaultProperties(Spice spice) {
        return new Properties().rarity(spice.getTier().getRarity());
    }

    @Override
    protected boolean canPlace(BlockPlaceContext context, BlockState state) {
        if (!(getBlock() instanceof SpicePlantBlock plant))
            return super.canPlace(context, state);

        BlockPos belowPos = context.getClickedPos().below();
        BlockState belowState = context.getLevel().getBlockState(belowPos);
        return plant.mayPlaceOn(belowState, context.getLevel(), belowPos) && super.canPlace(context, state);
    }
}
