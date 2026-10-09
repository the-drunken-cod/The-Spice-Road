package com.drunkencod.spice_road.drying;

import com.mojang.serialization.MapCodec;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.TagKey;
import net.minecraft.util.RandomSource;
import net.minecraft.world.Containers;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.material.PushReaction;
import net.minecraft.world.level.pathfinder.PathComputationType;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

import com.drunkencod.spice_road.Constants;
import com.drunkencod.spice_road.block.SpiceBlockTags;
import com.drunkencod.spice_road.platform.Services;
import com.drunkencod.spice_road.registry.ModBlockEntities;
import com.drunkencod.spice_road.rack.SpiceRackWood;
import com.drunkencod.spice_road.registry.ModSounds;

/**
 * The Drying Rack, which comes in every vanilla wood: dries up to two items at
 * once, one on each of its sides.
 * Players put items on it and take them off by hand, choosing the side by which
 * half of the rack they click; see {@link DryingRackBlockEntity} for the rest.
 * <p>
 * It is {@code heated} while a block of {@link SpiceBlockTags#HEAT_SOURCES}
 * sits next to it or while it stands in a biome of
 * {@link #ALWAYS_HEATED_BIOMES}, which makes it dry faster.
 */
public class DryingRackBlock extends BaseEntityBlock {

    public static final MapCodec<DryingRackBlock> CODEC = simpleCodec(DryingRackBlock::new);

    /** The side of the rack the viewer stands on, its thermometer facing them. */
    public static final DirectionProperty FACING = HorizontalDirectionalBlock.FACING;
    /** Whether a heat source or a hot biome speeds the rack up. */
    public static final BooleanProperty HEATED = BooleanProperty.create("heated");

    /** Biomes where every Drying Rack is heated, e.g. the nether. */
    public static final TagKey<Biome> ALWAYS_HEATED_BIOMES = TagKey.create(Registries.BIOME,
            ResourceLocation.fromNamespaceAndPath(Constants.MOD_ID, "drying_always_heated"));

    private static final VoxelShape SHAPE_ALONG_X = Block.box(1, 0, 3, 15, 13, 13);
    private static final VoxelShape SHAPE_ALONG_Z = Block.box(3, 0, 1, 13, 13, 15);

    /**
     * @param properties The block's properties, see
     *                   {@link #createProperties(SpiceRackWood)}.
     */
    public DryingRackBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(HEATED, false));
    }

    /**
     * @param wood The wood the rack is made of.
     * @return The properties of that wood's Drying Rack.
     */
    public static Properties createProperties(SpiceRackWood wood) {
        return Properties.of().noOcclusion().strength(0.5F).mapColor(wood.getMapColor()).sound(wood.getSoundType())
                .pushReaction(PushReaction.BLOCK);
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    // #region state

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, HEATED);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite())
                .setValue(HEATED, isHeatedAt(context.getLevel(), context.getClickedPos()));
    }

    @Override
    protected BlockState rotate(BlockState state, Rotation rotation) {
        return state.setValue(FACING, rotation.rotate(state.getValue(FACING)));
    }

    @Override
    protected BlockState mirror(BlockState state, Mirror mirror) {
        return state.setValue(FACING, mirror.mirror(state.getValue(FACING)));
    }

    /**
     * @param level The level.
     * @param pos   A position a rack would stand at.
     * @return Whether a rack there is heated: it stands in an
     *         {@link #ALWAYS_HEATED_BIOMES} biome, or a block of
     *         {@link SpiceBlockTags#HEAT_SOURCES} that isn't an unlit
     *         campfire-like block sits on one of its faces.
     */
    public static boolean isHeatedAt(LevelReader level, BlockPos pos) {
        if (level.getBiome(pos).is(ALWAYS_HEATED_BIOMES))
            return true;
        for (Direction direction : Direction.values()) {
            BlockState neighbor = level.getBlockState(pos.relative(direction));
            if (neighbor.is(SpiceBlockTags.HEAT_SOURCES) && (!neighbor.hasProperty(BlockStateProperties.LIT)
                    || neighbor.getValue(BlockStateProperties.LIT)))
                return true;
        }
        return false;
    }

    @Override
    protected void neighborChanged(BlockState state, Level level, BlockPos pos, Block block, BlockPos fromPos,
            boolean movedByPiston) {
        super.neighborChanged(state, level, pos, block, fromPos, movedByPiston);
        if (level.isClientSide)
            return;
        boolean heated = isHeatedAt(level, pos);
        if (state.getValue(HEATED) != heated)
            level.setBlock(pos, state.setValue(HEATED, heated), Block.UPDATE_ALL);
        if (level.getBlockEntity(pos) instanceof DryingRackBlockEntity rack)
            rack.markPoweredDirty();
    }

    // #region shape

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return state.getValue(FACING).getAxis() == Direction.Axis.X ? SHAPE_ALONG_Z : SHAPE_ALONG_X;
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
        return new DryingRackBlockEntity(pos, state);
    }

    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state,
            BlockEntityType<T> type) {
        if (level.isClientSide)
            return null;
        return createTickerHelper(type, ModBlockEntities.DRYING_RACK.get(),
                (tickLevel, pos, tickState, rack) -> rack.serverTick(tickLevel, pos, tickState));
    }

    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        if (!state.is(newState.getBlock()) && level.getBlockEntity(pos) instanceof DryingRackBlockEntity rack) {
            Containers.dropContents(level, pos, rack);
            level.updateNeighbourForOutputSignal(pos, this);
        }
        super.onRemove(state, level, pos, newState, movedByPiston);
    }

    // #region interaction

    /**
     * @param state The rack's state.
     * @param pos   The rack's position.
     * @param hit   Where it was clicked.
     * @return {@link DryingRackBlockEntity#LEFT} or
     *         {@link DryingRackBlockEntity#RIGHT}:
     *         the half of the rack, as seen from its front, that was clicked.
     */
    private static int sideAt(BlockState state, BlockPos pos, Vec3 hit) {
        Direction left = state.getValue(FACING).getClockWise();
        double along = (hit.x - pos.getX() - 0.5) * left.getStepX() + (hit.z - pos.getZ() - 0.5) * left.getStepZ();
        return along >= 0 ? DryingRackBlockEntity.LEFT : DryingRackBlockEntity.RIGHT;
    }

    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos,
            Player player, InteractionHand hand, BlockHitResult hit) {
        if (stack.isEmpty() || !(level.getBlockEntity(pos) instanceof DryingRackBlockEntity rack))
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        if (!DryingRackBlockEntity.canDry(level, stack))
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        if (level.isClientSide)
            return rack.hasFreeInput() ? ItemInteractionResult.SUCCESS
                    : ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        if (!rack.insertByPlayer(player, stack, sideAt(state, pos, hit.getLocation())))
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        stack.consume(1, player);
        playSound(level, pos, ModSounds.DRYING_RACK_ADD_ITEM.get());
        return ItemInteractionResult.SUCCESS;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player,
            BlockHitResult hit) {
        if (!(level.getBlockEntity(pos) instanceof DryingRackBlockEntity rack))
            return InteractionResult.PASS;
        int side = sideAt(state, pos, hit.getLocation());
        if (level.isClientSide)
            return rack.hasAnythingToTake(side, player.isSecondaryUseActive()) ? InteractionResult.SUCCESS
                    : InteractionResult.PASS;
        ItemStack taken = rack.takeByPlayer(side, player.isSecondaryUseActive());
        if (taken.isEmpty())
            return InteractionResult.PASS;
        if (!player.getInventory().add(taken))
            player.drop(taken, false);
        playSound(level, pos, ModSounds.DRYING_RACK_REMOVE_ITEM.get());
        return InteractionResult.CONSUME;
    }

    private static void playSound(Level level, BlockPos pos, SoundEvent sound) {
        if (Services.CONFIG.areDryingRackSoundsEnabled())
            level.playSound(null, pos, sound, SoundSource.BLOCKS, 0.5F, level.random.nextFloat() * 0.1F + 0.9F);
    }

    // #region redstone

    @Override
    protected boolean hasAnalogOutputSignal(BlockState state) {
        return true;
    }

    @Override
    protected int getAnalogOutputSignal(BlockState state, Level level, BlockPos pos) {
        return level.getBlockEntity(pos) instanceof DryingRackBlockEntity rack ? rack.getAnalogSignal() : 0;
    }

    // #region effects

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        if (!state.getValue(HEATED) || random.nextInt(5) != 0
                || !(level.getBlockEntity(pos) instanceof DryingRackBlockEntity rack)
                || !Services.CONFIG.areDryingRackParticlesEnabled())
            return;
        Direction left = state.getValue(FACING).getClockWise();
        for (int side = 0; side < DryingRackBlockEntity.SIDES; side++) {
            if (!rack.isDrying(side))
                continue;
            double along = (side == DryingRackBlockEntity.LEFT ? 3 : -3) / 16.0;
            level.addParticle(ParticleTypes.SMOKE,
                    pos.getX() + 0.5 + left.getStepX() * along + (random.nextDouble() - 0.5) * 0.1,
                    pos.getY() + 0.9, pos.getZ() + 0.5 + left.getStepZ() * along + (random.nextDouble() - 0.5) * 0.1,
                    0, 0.03, 0);
        }
    }
}
