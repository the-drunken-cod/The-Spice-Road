package com.drunkencod.spice_road.block;

import java.util.function.Supplier;

import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.block.state.BlockBehaviour;

import com.drunkencod.spice_road.spice.Spice;

/**
 * {@code FLOWER_PATCH} x {@code PICK} template. Works exactly like a
 * vanilla flower, but grows through stages, and breaking at
 * full growth drops the flower item (spice-use or decorative) plus seeds
 * (see {@code SpicePlantLootTables}, used identically for both Source
 * Types).
 * <p>
 * All growth-stage mechanics are inherited unchanged from
 * {@link SpicePlantBlock}; this subclass exists as the concrete "shape"
 * that registration/datagen instantiate for every {@code FLOWER_PATCH}
 * Spice, and as an anchor for any future flower-specific behaviour.
 */
public class FlowerPatchBlock extends SpicePlantBlock {

    public FlowerPatchBlock(BlockBehaviour.Properties properties, Supplier<? extends ItemLike> seedItem,
            Spice spice) {
        super(properties, seedItem, spice);
    }
}
