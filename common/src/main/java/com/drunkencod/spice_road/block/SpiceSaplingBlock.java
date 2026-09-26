package com.drunkencod.spice_road.block;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.SaplingBlock;
import net.minecraft.world.level.block.grower.TreeGrower;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;

import com.drunkencod.spice_road.platform.Services;
import com.drunkencod.spice_road.spice.Spice;

/**
 * Sapling of a {@link SpiceTree}. Grows into the tree's datapack-defined
 * configured feature (see {@link SpiceTree#getTreeFeature()}), gated by Spice
 * Region support and scaled by the Spice's tier growth speed, just like
 * {@link SpiceCropBlock}.
 */
public class SpiceSaplingBlock extends SaplingBlock {

    private final Spice spice;

    /**
     * @param treeGrower Grows the tree's configured feature.
     * @param properties Block properties.
     * @param spice      The {@link Spice} this sapling grows into.
     */
    public SpiceSaplingBlock(TreeGrower treeGrower, BlockBehaviour.Properties properties, Spice spice) {
        super(treeGrower, properties);
        this.spice = spice;
    }

    /**
     * Rolls vanilla sapling growth via {@link SpiceGrowth#rollScaled}. Each roll
     * re-reads the current state and stops once the sapling has become a tree,
     * so a multiplier above {@code 1.0} can't try to grow a second tree in its
     * place.
     */
    @Override
    protected void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        if (!spice.canBeCultivatedAt(level, pos))
            return;

        SpiceGrowth.rollScaled(Services.CONFIG.getSpicePlantGrowthSpeedMultiplier(spice.getTier()), random, () -> {
            BlockState current = level.getBlockState(pos);
            if (current.is(this))
                super.randomTick(current, level, pos, random);
        });
    }

    @Override
    public void performBonemeal(ServerLevel level, RandomSource random, BlockPos pos, BlockState state) {
        if (!spice.canBeCultivatedAt(level, pos))
            return;

        super.performBonemeal(level, random, pos, state);
    }
}
