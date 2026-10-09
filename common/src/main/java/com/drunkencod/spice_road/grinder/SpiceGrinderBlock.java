package com.drunkencod.spice_road.grinder;

import org.jetbrains.annotations.Nullable;

import com.mojang.serialization.MapCodec;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;
import net.minecraft.world.level.pathfinder.PathComputationType;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

import com.drunkencod.spice_road.Constants;

/**
 * A Spice Grinder set down in the world. It can be placed against any face, but
 * always stands on the floor of its own block, so the block below has to be able
 * to support its center. It pops off, dropping the Grinder with its run, when
 * that block goes. Right-clicking it opens the GUI of the Grinder item;
 * shift-right-clicking with both hands empty picks it up into the main hand.
 * See {@link SpiceGrinderBlockEntity} for what it holds.
 */
public class SpiceGrinderBlock extends BaseEntityBlock {

    public static final MapCodec<SpiceGrinderBlock> CODEC = simpleCodec(SpiceGrinderBlock::new);

    /** The side the Grinder faces, which is towards the player who placed it. */
    public static final DirectionProperty FACING = HorizontalDirectionalBlock.FACING;

    /** Translation key of the message telling the player another one is using it. */
    private static final String IN_USE_KEY = Constants.MOD_ID + ".message.grinder_in_use";

    /** The mortar is 6 wide and 5 tall, centered on the floor of the block. */
    private static final VoxelShape SHAPE = Block.box(5, 0, 5, 11, 5, 11);

    /**
     * @param properties The block's properties, see {@link #createProperties()}.
     */
    public SpiceGrinderBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH));
    }

    /** @return The properties of the Spice Grinder block. */
    public static Properties createProperties() {
        return Properties.of().mapColor(MapColor.STONE).strength(0.5F).sound(SoundType.STONE).noOcclusion()
                .pushReaction(PushReaction.BLOCK);
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    // #region state

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }

    @Override
    public @Nullable BlockState getStateForPlacement(BlockPlaceContext context) {
        return defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
    }

    @Override
    protected BlockState rotate(BlockState state, Rotation rotation) {
        return state.setValue(FACING, rotation.rotate(state.getValue(FACING)));
    }

    @Override
    protected BlockState mirror(BlockState state, Mirror mirror) {
        return state.rotate(mirror.getRotation(state.getValue(FACING)));
    }

    // #region support

    @Override
    protected boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        return canSupportCenter(level, pos.below(), Direction.UP);
    }

    @Override
    protected BlockState updateShape(BlockState state, Direction direction, BlockState neighborState,
            LevelAccessor level, BlockPos pos, BlockPos neighborPos) {
        return direction == Direction.DOWN && !canSurvive(state, level, pos) ? Blocks.AIR.defaultBlockState()
                : super.updateShape(state, direction, neighborState, level, pos, neighborPos);
    }

    // #region shape

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Override
    protected boolean isPathfindable(BlockState state, PathComputationType type) {
        return false;
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    // #region block entity

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new SpiceGrinderBlockEntity(pos, state);
    }

    // #region interaction

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player,
            BlockHitResult hit) {
        if (level.isClientSide)
            return InteractionResult.SUCCESS;
        if (!(player instanceof ServerPlayer serverPlayer)
                || !(level.getBlockEntity(pos) instanceof SpiceGrinderBlockEntity grinder))
            return InteractionResult.PASS;
        if (grinder.isInUse())
            serverPlayer.displayClientMessage(Component.translatable(IN_USE_KEY), true);
        else if (player.isSecondaryUseActive())
            pickUp(level, pos, serverPlayer, grinder);
        else
            SpiceGrinderMenu.open(serverPlayer, -1, grinder);
        return InteractionResult.CONSUME;
    }

    /**
     * Takes the Grinder, with its run, back into the player's main hand. Only
     * reached with both hands empty, which vanilla requires for a shift-click.
     */
    private static void pickUp(Level level, BlockPos pos, ServerPlayer player, SpiceGrinderBlockEntity grinder) {
        player.setItemInHand(InteractionHand.MAIN_HAND, grinder.toStack());
        level.removeBlock(pos, false);
        level.playSound(null, pos, SoundEvents.ITEM_PICKUP, SoundSource.BLOCKS, 0.4F, 0.8F);
        level.gameEvent(player, GameEvent.BLOCK_DESTROY, pos);
    }
}
