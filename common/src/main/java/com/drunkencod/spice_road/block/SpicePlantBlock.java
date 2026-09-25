package com.drunkencod.spice_road.block;

import java.util.function.Supplier;

import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;

import com.drunkencod.spice_road.Constants;
import com.drunkencod.spice_road.spice.Spice;

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

    private final Supplier<? extends ItemLike> seedItem;
    private final Spice spice;

    /**
     * @param properties Block properties, typically {@link #defaultProperties()}.
     * @param seedItem   Supplies the seed item this Spice Plant is grown from
     *                   and hands back when middle-clicked/cloned.
     * @param spice      The {@link Spice} this block grows - the source of
     *                   truth for Spice Region support (see
     *                   {@link SpiceCropBlock#canSurvive}) and harvest
     *                   difficulty.
     */
    protected SpicePlantBlock(BlockBehaviour.Properties properties, Supplier<? extends ItemLike> seedItem,
            Spice spice) {

        super(properties);
        this.seedItem = seedItem;
        this.spice = spice;
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

    /**
     * Returns {@link Constants#DEFAULT_SPICE_PLANT_GROWTH_STAGES}, not the
     * live {@code IConfigHelper#getSpicePlantGrowthStages()} config value.
     * <p>
     * Vanilla's {@code CropBlock#isRandomlyTicking()} calls {@code getMaxAge()}
     * (via {@code isMaxAge()}) during {@code BlockStateBase#initCache()}, which
     * runs synchronously right after registries freeze - i.e. immediately
     * after {@code RegisterEvent}, and always before
     * {@code ModConfigEvent.Loading} fires, in every environment. There is no safe
     * point at which a {@code Block} override can read live config for a value that
     * affects block-state caching. See
     * {@code IConfigHelper#getSpicePlantGrowthStages()}'s javadoc for the
     * same constraint from the config side.
     */
    @Override
    public int getMaxAge() {

        return Constants.DEFAULT_SPICE_PLANT_GROWTH_STAGES;
    }

    @Override
    protected ItemLike getBaseSeedId() {

        return seedItem.get();
    }

    /** @return The {@link Spice} this block grows. */
    protected Spice getSpice() {

        return spice;
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
