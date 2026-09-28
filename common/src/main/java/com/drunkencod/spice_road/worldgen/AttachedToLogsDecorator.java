package com.drunkencod.spice_road.worldgen;

import java.util.List;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.Util;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.levelgen.feature.stateproviders.BlockStateProvider;
import net.minecraft.world.level.levelgen.feature.treedecorators.TreeDecorator;
import net.minecraft.world.level.levelgen.feature.treedecorators.TreeDecoratorType;

/**
 * Attaches blocks to the sides of a generated tree's logs, e.g. a Host Tree's
 * Spice Vine. A backport of vanilla's own {@code attached_to_logs} decorator,
 * which only exists in later versions.
 * <p>
 * Each log rolls once against {@link #probability} for one of
 * {@link #directions}, and the block is placed in the air block on that side of
 * the log. Note that a block whose blockstate points back at what it hangs off
 * of (a vine's face properties, a wall-mounted block's {@code facing}) needs
 * that property set to match: a {@code north} direction places the block north
 * of the log, so a vine there attaches to its {@code south} face.
 *
 * @see ModTreeDecorators#ATTACHED_TO_LOGS
 */
public class AttachedToLogsDecorator extends TreeDecorator {

    /** Codec of this decorator's JSON fields. */
    public static final MapCodec<AttachedToLogsDecorator> CODEC = RecordCodecBuilder.mapCodec(instance -> instance
            .group(
                    Codec.floatRange(0.0F, 1.0F).fieldOf("probability")
                            .forGetter(decorator -> decorator.probability),
                    BlockStateProvider.CODEC.fieldOf("block_provider")
                            .forGetter(decorator -> decorator.blockProvider),
                    ExtraCodecs.nonEmptyList(Direction.CODEC.listOf()).fieldOf("directions")
                            .forGetter(decorator -> decorator.directions))
            .apply(instance, AttachedToLogsDecorator::new));

    private final float probability;
    private final BlockStateProvider blockProvider;
    private final List<Direction> directions;

    /**
     * @param probability   Chance per log that a block is attached to it.
     * @param blockProvider Supplies the attached block's state.
     * @param directions    Sides of a log a block may attach to, one picked at
     *                      random per log.
     */
    public AttachedToLogsDecorator(float probability, BlockStateProvider blockProvider, List<Direction> directions) {
        this.probability = probability;
        this.blockProvider = blockProvider;
        this.directions = directions;
    }

    @Override
    public void place(TreeDecorator.Context context) {
        RandomSource random = context.random();
        for (BlockPos logPos : Util.shuffledCopy(context.logs(), random)) {
            BlockPos attachPos = logPos.relative(Util.getRandom(this.directions, random));
            if (random.nextFloat() <= this.probability && context.isAir(attachPos))
                context.setBlock(attachPos, this.blockProvider.getState(random, attachPos));
        }
    }

    @Override
    protected TreeDecoratorType<?> type() {
        return ModTreeDecorators.ATTACHED_TO_LOGS.get();
    }
}
