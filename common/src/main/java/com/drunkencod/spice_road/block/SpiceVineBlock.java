package com.drunkencod.spice_road.block;

import java.util.function.Supplier;

import org.jetbrains.annotations.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.BonemealableBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.VineBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.BlockHitResult;

import com.drunkencod.spice_road.Constants;
import com.drunkencod.spice_road.platform.Services;
import com.drunkencod.spice_road.spice.Spice;

/**
 * A {@code VINE} Source Type Spice Plant. Climbs and spreads exactly like a
 * vanilla vine, regardless of climate, and is propagated by shearing off a
 * segment. On top of that, each segment ripens through {@link #AGE} stages
 * {@code 0} to {@link Constants#SPICE_VINE_GROWTH_STAGES}, but only where
 * its Spice may be cultivated (see {@link Spice#canBeCultivatedAt}). Ripe
 * segments are right-clicked to drop the Spice and reset to stage {@code 0}.
 * <p>
 * Bonemeal only speeds up ripening, never the spreading.
 */
public class SpiceVineBlock extends VineBlock implements BonemealableBlock {

    /** Ripening stage, ripe at {@link Constants#SPICE_VINE_GROWTH_STAGES}. */
    public static final IntegerProperty AGE = IntegerProperty.create("age", 0, Constants.SPICE_VINE_GROWTH_STAGES);

    /**
     * On average, one in this many random ticks advances a ripening stage at a
     * 1.0 growth speed.
     */
    private static final int BASE_GROWTH_TICKS_PER_STAGE = 5;

    private final Spice spice;
    private final Supplier<? extends Item> productItem;

    /**
     * @param properties  Block properties, typically copied from
     *                    {@code Blocks.VINE}.
     * @param spice       The {@link Spice} this vine grows.
     * @param productItem Supplies the raw Spice item dropped when harvested.
     */
    public SpiceVineBlock(BlockBehaviour.Properties properties, Spice spice, Supplier<? extends Item> productItem) {
        super(properties);
        this.spice = spice;
        this.productItem = productItem;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(AGE);
    }

    // #region growth

    /**
     * Spreads like a vanilla vine, then advances the ripening stage of this
     * segment. Vanilla copies the whole state, including {@link #AGE}, into a
     * segment spreading upwards, so a newly grown segment above is reset to
     * stage {@code 0}.
     */
    @Override
    protected void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        BlockPos above = pos.above();
        boolean vineAbove = level.getBlockState(above).is(this);

        super.randomTick(state, level, pos, random);

        if (!vineAbove)
            resetAge(level, above);

        BlockState current = level.getBlockState(pos);
        if (!current.is(this) || isRipe(current) || !spice.canBeCultivatedAt(level, pos))
            return;

        double multiplier = Services.CONFIG.getSpicePlantGrowthSpeedMultiplier(spice.getTier());
        if (random.nextDouble() < multiplier / BASE_GROWTH_TICKS_PER_STAGE)
            level.setBlock(pos, current.setValue(AGE, current.getValue(AGE) + 1), Block.UPDATE_CLIENTS);
    }

    private void resetAge(ServerLevel level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        if (state.is(this) && state.getValue(AGE) > 0)
            level.setBlock(pos, state.setValue(AGE, 0), Block.UPDATE_CLIENTS);
    }

    /** @return Whether {@code state} is at its final ripening stage. */
    public static boolean isRipe(BlockState state) {
        return state.getValue(AGE) >= Constants.SPICE_VINE_GROWTH_STAGES;
    }

    @Override
    public boolean isValidBonemealTarget(LevelReader level, BlockPos pos, BlockState state) {
        return !isRipe(state);
    }

    @Override
    public boolean isBonemealSuccess(Level level, RandomSource random, BlockPos pos, BlockState state) {
        return true;
    }

    /**
     * Advances the ripening stage by one (epic Spices only half of the time),
     * if the Spice may be cultivated here.
     */
    @Override
    public void performBonemeal(ServerLevel level, RandomSource random, BlockPos pos, BlockState state) {
        if (!spice.canBeCultivatedAt(level, pos))
            return;

        int increase = spice.getTier().getRarity() == Rarity.EPIC ? random.nextInt(2) : 1;
        int age = Math.min(state.getValue(AGE) + increase, Constants.SPICE_VINE_GROWTH_STAGES);
        level.setBlock(pos, state.setValue(AGE, age), Block.UPDATE_CLIENTS);
    }

    // #region harvest

    /**
     * Spices with a Harvest Tool Requirement are harvested with an item from
     * their harvest tool tag, which takes durability damage.
     */
    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos,
            Player player, InteractionHand hand, BlockHitResult hitResult) {
        if (!isRipe(state) || !spice.requiresHarvestTool() || !SpiceHarvesting.isHarvestTool(spice, stack)
                || !SpiceHarvesting.mayHarvest(spice, player))
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;

        harvest(state, level, pos, player, hitResult, SoundEvents.GROWING_PLANT_CROP);
        if (!level.isClientSide())
            stack.hurtAndBreak(1, player, LivingEntity.getSlotForHand(hand));
        return ItemInteractionResult.sidedSuccess(level.isClientSide());
    }

    /** Spices without a Harvest Tool Requirement are picked by hand. */
    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player,
            BlockHitResult hitResult) {
        if (!isRipe(state) || spice.requiresHarvestTool() || !SpiceHarvesting.mayHarvest(spice, player))
            return InteractionResult.PASS;

        harvest(state, level, pos, player, hitResult, SoundEvents.SWEET_BERRY_BUSH_PICK_BERRIES);
        return InteractionResult.sidedSuccess(level.isClientSide());
    }

    /**
     * Charges the harvest tool a ripe segment was broken with, so that breaking
     * it is equivalent to harvesting it by interaction - see
     * {@link SpiceHarvesting#hurtHarvestTool} and {@code SpiceBreakHarvest}.
     */
    @Override
    public void playerDestroy(Level level, Player player, BlockPos pos, BlockState state,
            @Nullable BlockEntity blockEntity, ItemStack tool) {
        super.playerDestroy(level, player, pos, state, blockEntity, tool);

        if (isRipe(state))
            SpiceHarvesting.hurtHarvestTool(spice, level, player, tool);
    }

    /** Drops the Spice from the clicked face and resets the segment to stage {@code 0}. */
    private void harvest(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult,
            SoundEvent sound) {
        if (level.isClientSide())
            return;

        int yield = SpiceHarvesting.getPlantYield(spice);
        if (yield > 0)
            popResourceFromFace(level, pos, hitResult.getDirection(), new ItemStack(productItem.get(), yield));

        level.playSound(null, pos, sound, SoundSource.BLOCKS, 1.0F, 0.8F + level.getRandom().nextFloat() * 0.4F);

        BlockState harvestedState = state.setValue(AGE, 0);
        level.setBlock(pos, harvestedState, Block.UPDATE_CLIENTS);
        level.gameEvent(GameEvent.BLOCK_CHANGE, pos, GameEvent.Context.of(player, harvestedState));
    }

    /** @return The {@link Spice} this vine grows. */
    public Spice getSpice() {
        return spice;
    }
}
