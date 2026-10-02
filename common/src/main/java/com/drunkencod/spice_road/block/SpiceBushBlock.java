package com.drunkencod.spice_road.block;

import java.util.function.Supplier;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

import com.drunkencod.spice_road.Constants;
import com.drunkencod.spice_road.spice.Spice;

/**
 * {@code BUSH} template. Grows, regrows and is harvested exactly like a
 * {@link FlowerPatchBlock}, but through fewer growth stages.
 */
public class SpiceBushBlock extends FlowerPatchBlock {

    /** Per-age outline shapes, indexed by {@code age}: 9, 12 and 15 pixels tall. */
    private static final VoxelShape[] SHAPE_BY_AGE = new VoxelShape[Constants.SPICE_BUSH_GROWTH_STAGES + 1];

    static {
        for (int age = 0; age < SHAPE_BY_AGE.length; age++)
            SHAPE_BY_AGE[age] = shapeOfHeight(9.0D + age * 3.0D);
    }

    public SpiceBushBlock(BlockBehaviour.Properties properties, Supplier<? extends ItemLike> seedItem,
            Spice spice) {
        super(properties, seedItem, spice);
    }

    /** @return {@link Constants#SPICE_BUSH_GROWTH_STAGES}. */
    @Override
    public int getMaxAge() {
        return Constants.SPICE_BUSH_GROWTH_STAGES;
    }

    /** Bushes start taller than other Spice Plants, see {@link #SHAPE_BY_AGE}. */
    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE_BY_AGE[getAge(state)];
    }
}
