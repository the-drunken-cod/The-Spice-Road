package com.drunkencod.spice_road.rack;

import java.util.EnumMap;
import java.util.Map;

import com.mojang.serialization.MapCodec;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.Containers;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.AttachFace;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.material.PushReaction;
import net.minecraft.world.level.pathfinder.PathComputationType;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

import com.drunkencod.spice_road.advancement.ModCriteriaTriggers;

/**
 * The Spice Rack: stores spices and shows them off. It stands on the floor, on
 * a wall or hangs from the ceiling, and needs no supporting block, so it can
 * float. Clicking it with something it {@link SpiceRackBlockEntity#accepts
 * accepts} in hand inserts as much as fits; otherwise it opens the rack's GUI.
 * See {@link SpiceRackBlockEntity} for the rest.
 */
public class SpiceRackBlock extends BaseEntityBlock {

    public static final MapCodec<SpiceRackBlock> CODEC = simpleCodec(SpiceRackBlock::new);

    /**
     * Whether the rack stands on the floor, hangs from the ceiling or is mounted on
     * a wall.
     */
    public static final EnumProperty<AttachFace> FACE = BlockStateProperties.ATTACH_FACE;
    /**
     * The side a wall rack faces, away from the wall. Floor and ceiling racks
     * look the same from all sides, so it has no effect on them.
     */
    public static final DirectionProperty FACING = HorizontalDirectionalBlock.FACING;

    // #region shapes

    /**
     * Each shape is a few boxes following the model: for a wall rack the
     * backboard, the two shelves and their lips, for the others the two
     * platforms and the pole between them. Wall shapes are given facing north
     * and rotated for the other directions.
     */
    private static final VoxelShape FLOOR_SHAPE = Shapes.or(
            Block.box(5, 0, 2, 11, 1, 14), Block.box(3, 0, 3, 13, 1, 13), Block.box(2, 0, 5, 14, 1, 11),
            Block.box(7, 1, 7, 9, 9, 9),
            Block.box(6, 8, 4, 10, 9, 12), Block.box(4, 8, 6, 12, 9, 10), Block.box(5, 8, 5, 11, 9, 11));
    private static final VoxelShape CEILING_SHAPE = Shapes.or(
            Block.box(6, 7, 4, 10, 8, 12), Block.box(4, 7, 6, 12, 8, 10), Block.box(5, 7, 5, 11, 8, 11),
            Block.box(7, 8, 7, 9, 15, 9),
            Block.box(5, 15, 2, 11, 16, 14), Block.box(3, 15, 3, 13, 16, 13), Block.box(2, 15, 5, 14, 16, 11));
    private static final VoxelShape WALL_SHAPE_NORTH = Shapes.or(
            Block.box(0, 2, 15, 16, 14, 16),
            Block.box(0, 2, 12, 16, 3, 15), Block.box(0, 3, 13, 1, 5, 15), Block.box(15, 3, 13, 16, 5, 15),
            Block.box(0, 8, 12, 16, 9, 15), Block.box(0, 9, 13, 1, 11, 15), Block.box(15, 9, 13, 16, 11, 15));
    private static final Map<Direction, VoxelShape> WALL_SHAPES = wallShapes();

    /**
     * @param properties The block's properties, see
     *                   {@link #createProperties(SpiceRackWood)}.
     */
    public SpiceRackBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(FACE, AttachFace.FLOOR).setValue(FACING, Direction.NORTH));
    }

    /**
     * @param wood The wood the rack is made of.
     * @return The properties of that wood's Spice Rack.
     */
    public static Properties createProperties(SpiceRackWood wood) {
        return Properties.of().noOcclusion().strength(1.0F).mapColor(wood.getMapColor()).sound(wood.getSoundType())
                .pushReaction(PushReaction.BLOCK);
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    // #region state

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACE, FACING);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        Direction clicked = context.getClickedFace();
        if (clicked.getAxis() == Direction.Axis.Y)
            return defaultBlockState().setValue(FACE, clicked == Direction.UP ? AttachFace.FLOOR : AttachFace.CEILING)
                    .setValue(FACING, context.getHorizontalDirection().getOpposite());
        return defaultBlockState().setValue(FACE, AttachFace.WALL).setValue(FACING, clicked);
    }

    @Override
    protected BlockState rotate(BlockState state, Rotation rotation) {
        return state.setValue(FACING, rotation.rotate(state.getValue(FACING)));
    }

    @Override
    protected BlockState mirror(BlockState state, Mirror mirror) {
        return state.setValue(FACING, mirror.mirror(state.getValue(FACING)));
    }

    // #region shape

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return switch (state.getValue(FACE)) {
            case FLOOR -> FLOOR_SHAPE;
            case CEILING -> CEILING_SHAPE;
            case WALL -> WALL_SHAPES.get(state.getValue(FACING));
        };
    }

    @Override
    protected boolean isPathfindable(BlockState state, PathComputationType type) {
        return false;
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    private static Map<Direction, VoxelShape> wallShapes() {
        Map<Direction, VoxelShape> shapes = new EnumMap<>(Direction.class);
        VoxelShape shape = WALL_SHAPE_NORTH;
        // Turning clockwise from above takes north to east, south and then west.
        for (Direction direction : new Direction[] { Direction.NORTH, Direction.EAST, Direction.SOUTH,
                Direction.WEST }) {
            shapes.put(direction, shape);
            shape = turnClockwise(shape);
        }
        return shapes;
    }

    /**
     * @param shape A shape within a block.
     * @return The shape turned a quarter clockwise about the block's vertical axis.
     */
    private static VoxelShape turnClockwise(VoxelShape shape) {
        VoxelShape[] turned = { Shapes.empty() };
        shape.forAllBoxes((minX, minY, minZ, maxX, maxY, maxZ) -> turned[0] = Shapes.or(turned[0],
                Shapes.box(1 - maxZ, minY, minX, 1 - minZ, maxY, maxX)));
        return turned[0];
    }

    // #region block entity

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new SpiceRackBlockEntity(pos, state);
    }

    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        if (!state.is(newState.getBlock()) && level.getBlockEntity(pos) instanceof SpiceRackBlockEntity rack) {
            Containers.dropContents(level, pos, rack);
            level.updateNeighbourForOutputSignal(pos, this);
        }
        super.onRemove(state, level, pos, newState, movedByPiston);
    }

    // #region interaction

    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos,
            Player player, InteractionHand hand, BlockHitResult hit) {
        if (!SpiceRackBlockEntity.accepts(stack) || !(level.getBlockEntity(pos) instanceof SpiceRackBlockEntity rack))
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        if (level.isClientSide)
            return ItemInteractionResult.SUCCESS;
        // Creative players keep what they hold.
        if (rack.insert(player.hasInfiniteMaterials() ? stack.copy() : stack) == 0)
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        level.playSound(null, pos, SoundEvents.BUNDLE_INSERT, SoundSource.BLOCKS, 0.8F,
                level.random.nextFloat() * 0.1F + 0.9F);
        if (player instanceof ServerPlayer serverPlayer && SpiceRackBlockEntity.isFull(rack))
            ModCriteriaTriggers.SPICE_RACK_FILLED.get().trigger(serverPlayer);
        return ItemInteractionResult.CONSUME;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player,
            BlockHitResult hit) {
        if (level.isClientSide)
            return InteractionResult.SUCCESS;
        if (level.getBlockEntity(pos) instanceof SpiceRackBlockEntity rack)
            player.openMenu(rack);
        return InteractionResult.CONSUME;
    }

    // #region redstone

    @Override
    protected boolean hasAnalogOutputSignal(BlockState state) {
        return true;
    }

    @Override
    protected int getAnalogOutputSignal(BlockState state, Level level, BlockPos pos) {
        return level.getBlockEntity(pos) instanceof SpiceRackBlockEntity rack
                ? AbstractContainerMenu.getRedstoneSignalFromContainer(rack)
                : 0;
    }
}
