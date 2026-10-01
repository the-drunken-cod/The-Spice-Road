package com.drunkencod.spice_road.block;

import java.util.function.Supplier;

import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.block.state.BlockBehaviour;

import com.drunkencod.spice_road.Constants;
import com.drunkencod.spice_road.spice.Spice;

/**
 * {@code BUSH} template. Grows, regrows and is harvested exactly like a
 * {@link FlowerPatchBlock}, but through fewer growth stages.
 */
public class SpiceBushBlock extends FlowerPatchBlock {

    public SpiceBushBlock(BlockBehaviour.Properties properties, Supplier<? extends ItemLike> seedItem,
            Spice spice) {
        super(properties, seedItem, spice);
    }

    /** @return {@link Constants#SPICE_BUSH_GROWTH_STAGES}. */
    @Override
    public int getMaxAge() {
        return Constants.SPICE_BUSH_GROWTH_STAGES;
    }
}
