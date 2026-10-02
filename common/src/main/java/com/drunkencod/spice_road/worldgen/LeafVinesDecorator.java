package com.drunkencod.spice_road.worldgen;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.VineBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.treedecorators.TreeDecorator;
import net.minecraft.world.level.levelgen.feature.treedecorators.TreeDecoratorType;

/**
 * Hangs vines off the sides of a generated tree's leaves, like vanilla's
 * {@code leave_vine} decorator does for jungle trees, but with a configurable
 * vine block, e.g. a Host Tree's Spice Vine.
 * <p>
 * Each leaf rolls once against {@link #probability} per horizontal side. If the
 * block on that side is air, a vine attached to the leaf is placed there and
 * continues downwards for up to {@link #maxLength} blocks total while the space
 * below is air.
 *
 * @see ModTreeDecorators#LEAF_VINES
 */
public class LeafVinesDecorator extends TreeDecorator {

    /** Codec of this decorator's JSON fields. */
    public static final MapCodec<LeafVinesDecorator> CODEC = RecordCodecBuilder.mapCodec(instance -> instance
            .group(
                    Codec.floatRange(0.0F, 1.0F).fieldOf("probability")
                            .forGetter(decorator -> decorator.probability),
                    BuiltInRegistries.BLOCK.byNameCodec().fieldOf("block")
                            .forGetter(decorator -> decorator.block),
                    Codec.intRange(1, 32).optionalFieldOf("max_length", 5)
                            .forGetter(decorator -> decorator.maxLength))
            .apply(instance, LeafVinesDecorator::new));

    private final float probability;
    private final Block block;
    private final int maxLength;

    /**
     * @param probability Chance per leaf side that a vine hangs off it.
     * @param block       The vine block to place, a {@link VineBlock}.
     * @param maxLength   Maximum vine length, including the segment attached to
     *                    the leaf.
     */
    public LeafVinesDecorator(float probability, Block block, int maxLength) {
        this.probability = probability;
        this.block = block;
        this.maxLength = maxLength;
    }

    @Override
    public void place(TreeDecorator.Context context) {
        RandomSource random = context.random();
        for (BlockPos leafPos : context.leaves()) {
            for (Direction side : Direction.Plane.HORIZONTAL) {
                BlockPos vinePos = leafPos.relative(side);
                if (random.nextFloat() < this.probability && context.isAir(vinePos))
                    hangVine(context, vinePos, side.getOpposite());
            }
        }
    }

    /**
     * Places a vine at {@code pos} attached to {@code face}, then keeps copying it
     * downwards while the space below is air.
     */
    private void hangVine(TreeDecorator.Context context, BlockPos pos, Direction face) {
        BlockState state = this.block.defaultBlockState().setValue(VineBlock.getPropertyForFace(face), true);
        BlockPos.MutableBlockPos current = pos.mutable();
        for (int length = 0; length < this.maxLength && context.isAir(current); length++) {
            context.setBlock(current, state);
            current.move(Direction.DOWN);
        }
    }

    @Override
    protected TreeDecoratorType<?> type() {
        return ModTreeDecorators.LEAF_VINES.get();
    }
}
