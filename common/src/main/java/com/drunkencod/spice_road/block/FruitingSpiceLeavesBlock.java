package com.drunkencod.spice_road.block;

import org.jetbrains.annotations.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
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
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.BlockHitResult;

import com.drunkencod.spice_road.Constants;
import com.drunkencod.spice_road.platform.Services;
import com.drunkencod.spice_road.spice.HarvestAction;
import com.drunkencod.spice_road.spice.Spice;
import com.drunkencod.spice_road.stats.ModStats;
import com.drunkencod.spice_road.spice.region.SeededHash;

/**
 * Leaves of a {@link SpiceTree.HarvestPart#LEAVES} Spice Tree. Fruit-bearing
 * leaves grow through {@link #AGE} stages {@code 0} to
 * {@link Constants#SPICE_TREE_LEAF_GROWTH_STAGES} and, once ripe, are
 * right-clicked to drop the tree's Spice from the clicked face and reset to
 * stage {@code 0}.
 * <p>
 * Only some leaves bear fruit - see {@link #canBearFruit}.
 */
public class FruitingSpiceLeavesBlock extends LeavesBlock {

    /** Fruiting stage, ripe at {@link Constants#SPICE_TREE_LEAF_GROWTH_STAGES}. */
    public static final IntegerProperty AGE = IntegerProperty.create("age", 0,
            Constants.SPICE_TREE_LEAF_GROWTH_STAGES);

    /**
     * On average, one in this many random ticks advances a fruiting stage at a 1.0
     * growth speed.
     */
    private static final int BASE_GROWTH_TICKS_PER_STAGE = 5;

    private final SpiceTree tree;

    /**
     * @param properties Block properties.
     * @param tree       The tree these leaves belong to. Only stored, not read
     *                   during construction.
     */
    public FruitingSpiceLeavesBlock(BlockBehaviour.Properties properties, SpiceTree tree) {
        super(properties);
        this.tree = tree;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(AGE);
    }

    // #region growth

    @Override
    protected boolean isRandomlyTicking(BlockState state) {
        return super.isRandomlyTicking(state) || (!state.getValue(PERSISTENT) && !isRipe(state));
    }

    @Override
    protected void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        if (decaying(state)) {
            super.randomTick(state, level, pos, random);
            return;
        }

        if (isRipe(state) || !canBearFruit(level, pos, state))
            return;

        double multiplier = Services.CONFIG.getSpicePlantGrowthSpeedMultiplier(tree.getSpice().getTier());
        if (random.nextDouble() < multiplier / BASE_GROWTH_TICKS_PER_STAGE)
            level.setBlock(pos, state.setValue(AGE, state.getValue(AGE) + 1), Block.UPDATE_CLIENTS);
    }

    /**
     * Whether the leaves at {@code pos} are able to bear fruit. All of these
     * must hold:
     * <ul>
     * <li>The leaves are naturally grown (not {@link #PERSISTENT}, i.e. not
     * player-placed).</li>
     * <li>At least one side is exposed to air.</li>
     * <li>The position is one of the fruit-bearing ones - a deterministic
     * fraction of positions, set per tier by
     * {@code IConfigHelper#getSpiceTreeFruitingLeavesChance}.</li>
     * <li>The Spice may be cultivated here - see
     * {@link Spice#canBeCultivatedAt}.</li>
     * </ul>
     *
     * @param level The server level.
     * @param pos   The leaves' position.
     * @param state The leaves' state.
     * @return Whether the leaves may advance their fruiting stage.
     */
    public boolean canBearFruit(ServerLevel level, BlockPos pos, BlockState state) {
        return !state.getValue(PERSISTENT)
                && isAirExposed(level, pos)
                && isFruitBearingPosition(level.getSeed(), pos)
                && tree.getSpice().canBeCultivatedAt(level, pos);
    }

    private static boolean isAirExposed(ServerLevel level, BlockPos pos) {
        for (Direction direction : Direction.values()) {
            if (level.getBlockState(pos.relative(direction)).isAir())
                return true;
        }
        return false;
    }

    private boolean isFruitBearingPosition(long worldSeed, BlockPos pos) {
        Spice spice = tree.getSpice();
        long hash = SeededHash.hash(SeededHash.hash(worldSeed, pos.getX(), pos.getY()), pos.getZ(),
                spice.getId().hashCode());
        return SeededHash.toUnitDouble(hash) < Services.CONFIG.getSpiceTreeFruitingLeavesChance(spice.getTier());
    }

    /** @return Whether {@code state} is at its final fruiting stage. */
    public static boolean isRipe(BlockState state) {
        return state.getValue(AGE) >= Constants.SPICE_TREE_LEAF_GROWTH_STAGES;
    }

    // #region harvest

    /**
     * Spices with a Harvest Tool Requirement (which {@link HarvestAction#SHEAR}
     * always implies) are harvested with an item from their harvest tool tag,
     * which takes durability damage.
     */
    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos,
            Player player, InteractionHand hand, BlockHitResult hitResult) {
        Spice spice = tree.getSpice();
        if (!isRipe(state) || !spice.requiresHarvestTool() || !SpiceHarvesting.isHarvestTool(spice, stack)
                || !SpiceHarvesting.mayHarvest(spice, player))
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;

        harvest(state, level, pos, player, hitResult.getDirection(), SoundEvents.GROWING_PLANT_CROP);
        if (!level.isClientSide())
            stack.hurtAndBreak(1, player, LivingEntity.getSlotForHand(hand));
        return ItemInteractionResult.sidedSuccess(level.isClientSide());
    }

    /** Spices without a Harvest Tool Requirement are picked by hand. */
    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player,
            BlockHitResult hitResult) {
        if (!isRipe(state) || tree.getSpice().requiresHarvestTool()
                || !SpiceHarvesting.mayHarvest(tree.getSpice(), player))
            return InteractionResult.PASS;

        harvest(state, level, pos, player, hitResult.getDirection(), SoundEvents.SWEET_BERRY_BUSH_PICK_BERRIES);
        return InteractionResult.sidedSuccess(level.isClientSide());
    }

    /**
     * Charges the harvest tool ripe leaves were broken with, so that breaking
     * them is equivalent to harvesting them by interaction - see
     * {@link SpiceHarvesting#hurtHarvestTool} and
     * {@code SpiceBreakHarvest}.
     */
    @Override
    public void playerDestroy(Level level, Player player, BlockPos pos, BlockState state,
            @Nullable BlockEntity blockEntity, ItemStack tool) {
        super.playerDestroy(level, player, pos, state, blockEntity, tool);

        if (isRipe(state))
            SpiceHarvesting.hurtHarvestTool(tree.getSpice(), level, player, tool);
    }

    /**
     * Drops a harvest roll (see {@link SpiceTree#rollHarvest}) from the clicked
     * face and resets the leaves to stage {@code 0}.
     */
    private void harvest(BlockState state, Level level, BlockPos pos, Player player, Direction face,
            SoundEvent sound) {
        if (level.isClientSide())
            return;

        ModStats.award(player, ModStats.HAND_PICKED_HARVESTS);
        ItemStack harvest = tree.rollHarvest(level.getRandom());
        if (!harvest.isEmpty())
            popResourceFromFace(level, pos, face, harvest);

        level.playSound(null, pos, sound, SoundSource.BLOCKS, 1.0F, 0.8F + level.getRandom().nextFloat() * 0.4F);

        BlockState harvestedState = state.setValue(AGE, 0);
        level.setBlock(pos, harvestedState, Block.UPDATE_CLIENTS);
        level.gameEvent(GameEvent.BLOCK_CHANGE, pos, GameEvent.Context.of(player, harvestedState));
    }
}
