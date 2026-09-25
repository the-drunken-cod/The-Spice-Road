package com.drunkencod.spice_road.block;

import java.util.function.Supplier;

import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;

/**
 * Shared growth-stage/interaction template for the {@code flower_patch} and
 * {@code crop} {@link com.drunkencod.spice_road.spice.SourceType} Source
 * Types.
 * <p>
 * Both source types grow/regrow on farmland the same way, so this base class
 * holds all of that shared behaviour by extending vanilla {@link CropBlock}
 * directly: age-based growth stages gated by light/farmland fertility, bonemeal
 * support, and seed-item cloning are all inherited unchanged. A concrete
 * Spice Plant only needs to supply the configured stage count and the seed
 * item to hand back when the block is middle-clicked/cloned.
 */
public abstract class SpicePlantBlock extends CropBlock {

    /**
     * Highest age value vanilla's {@code AGE_7} blockstate property
     * (inherited unchanged from {@link CropBlock}) supports.
     */
    private static final int VANILLA_AGE_PROPERTY_LIMIT = 7;

    private final int maxAge;
    private final Supplier<? extends ItemLike> seedItem;

    /**
     * @param properties Block properties, typically {@link #defaultProperties()}.
     * @param maxAge     Configured growth stage count's highest age value (1-7
     *                   inclusive). Kept in the 1-7 range because this class
     *                   reuses vanilla {@code CropBlock}'s fixed {@code AGE_7}
     *                   blockstate property (values 0-7) rather than minting a
     *                   custom per-instance {@code IntegerProperty} sized to
     *                   the configured count: {@code CropBlock}'s own
     *                   constructor resolves {@link #getAgeProperty()}
     *                   polymorphically before this class's fields would be
     *                   assigned, so a field-backed override of that method
     *                   would read {@code null}/default at that point.
     *                   Reusing the fixed property sidesteps that
     *                   constructor-ordering hazard entirely.
     * @param seedItem   Supplies the seed item this Spice Plant is grown from
     *                   and hands back when middle-clicked/cloned.
     * @throws IllegalArgumentException If {@code maxAge} is outside 1-7.
     */
    protected SpicePlantBlock(BlockBehaviour.Properties properties, int maxAge, Supplier<? extends ItemLike> seedItem) {

        super(properties);

        if (maxAge < 1 || maxAge > VANILLA_AGE_PROPERTY_LIMIT) {
            throw new IllegalArgumentException(
                    "Spice Plant growth stage count must be in range 1-" + VANILLA_AGE_PROPERTY_LIMIT
                            + ", got " + maxAge);
        }

        this.maxAge = maxAge;
        this.seedItem = seedItem;
    }

    /**
     * Standard vanilla crop block properties (see e.g. {@code Blocks.WHEAT}):
     * plant-coloured, no collision, ticks randomly to grow, instant-break,
     * crop sound, destroyed by pistons.
     */
    public static BlockBehaviour.Properties defaultProperties() {

        return BlockBehaviour.Properties.of()
                .mapColor(MapColor.PLANT)
                .noCollission()
                .randomTicks()
                .instabreak()
                .sound(SoundType.CROP)
                .pushReaction(PushReaction.DESTROY);
    }

    @Override
    public int getMaxAge() {

        return maxAge;
    }

    @Override
    protected ItemLike getBaseSeedId() {

        return seedItem.get();
    }

    /**
     * Widens {@link CropBlock#getAgeProperty()} to public so the loot table
     * datagen providers (in a different package on both loaders) can read
     * the age blockstate property without duplicating it.
     */
    @Override
    public IntegerProperty getAgeProperty() {

        return super.getAgeProperty();
    }
}
