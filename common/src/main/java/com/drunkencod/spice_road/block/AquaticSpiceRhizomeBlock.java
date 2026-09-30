package com.drunkencod.spice_road.block;

import java.util.function.Supplier;

import org.jetbrains.annotations.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.FluidTags;
import net.minecraft.tags.TagKey;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SimpleWaterloggedBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.material.FlowingFluid;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;

import com.drunkencod.spice_road.Constants;
import com.drunkencod.spice_road.platform.Services;
import com.drunkencod.spice_road.spice.Spice;

/**
 * {@code RHIZOME} template for Aquatic Spices (see {@link Spice#isAquatic()}).
 * Grows like a {@link SpiceCropBlock}, but on {@link #AQUATIC_SPICE_GROWABLE}
 * ground instead of farmland, in exactly one block of water: it can't be
 * placed, and breaks, while more {@link #AQUATIC_SPICE_WATER} sits above it.
 * <p>
 * It may also stand dry - e.g. after its water is bucketed out - but is then
 * stunted unless {@code IConfigHelper#isAquaticSpiceWaterRequired()} is off.
 * Used both for planting and for worldgen, since unlike farmland its ground
 * can't be trampled out from under it.
 * <p>
 * Source water waterlogs it as with any vanilla waterloggable block. Flowing
 * water instead passes through it as if it weren't there (see
 * {@link FluidPassthroughBlock}), held in {@link #FLOW}/{@link #FALLING} -
 * unless {@code IConfigHelper#isAquaticSpiceFlowThroughEnabled()} is off,
 * in which case flowing water can't enter it, and planting it into flowing
 * water waterlogs it with a source, as with any vanilla waterloggable
 * block. Whichever fluid it holds is
 * always vanilla water; other {@link #AQUATIC_SPICE_WATER} fluids only count
 * as water when planting it.
 */
public class AquaticSpiceRhizomeBlock extends SpicePlantBlock
        implements SimpleWaterloggedBlock, FluidPassthroughBlock {

    /** Ground an Aquatic Spice may be planted on and survive on, wet or dry. */
    public static final TagKey<Block> AQUATIC_SPICE_GROWABLE = TagKey.create(Registries.BLOCK,
            ResourceLocation.fromNamespaceAndPath(Constants.MOD_ID, "aquatic_spice_growable"));

    /**
     * Fluids an Aquatic Spice counts as water: it's waterlogged when placed
     * into one, and can't live with one above it.
     */
    public static final TagKey<Fluid> AQUATIC_SPICE_WATER = TagKey.create(Registries.FLUID,
            ResourceLocation.fromNamespaceAndPath(Constants.MOD_ID, "aquatic_spice_water"));

    /** Whether this plant holds a water source. Never true together with {@link #FLOW}. */
    public static final BooleanProperty WATERLOGGED = BlockStateProperties.WATERLOGGED;

    /** Amount of flowing water passing through this plant, {@code 0} for none. */
    public static final IntegerProperty FLOW = IntegerProperty.create("flow", 0, 8);

    /** Whether the flowing water passing through this plant is falling. */
    public static final BooleanProperty FALLING = BlockStateProperties.FALLING;

    public AquaticSpiceRhizomeBlock(BlockBehaviour.Properties properties, Supplier<? extends ItemLike> seedItem,
            Spice spice) {
        super(properties, seedItem, spice);
        registerDefaultState(defaultBlockState().setValue(WATERLOGGED, false).setValue(FLOW, 0)
                .setValue(FALLING, false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(WATERLOGGED, FLOW, FALLING);
    }

    // #region placement

    /**
     * Holds whatever {@link #AQUATIC_SPICE_WATER} fluid it's placed into: a
     * source waterlogs it, flowing fluid passes through it as flowing water
     * (or waterlogs it too, if flow-through is disabled).
     */
    @Override
    public @Nullable BlockState getStateForPlacement(BlockPlaceContext context) {
        FluidState fluid = context.getLevel().getFluidState(context.getClickedPos());
        if (!fluid.is(AQUATIC_SPICE_WATER))
            return defaultBlockState();
        if (fluid.isSource() || !Services.CONFIG.isAquaticSpiceFlowThroughEnabled())
            return defaultBlockState().setValue(WATERLOGGED, true);
        return holding(defaultBlockState(), fluid);
    }

    @Override
    protected void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean movedByPiston) {
        super.onPlace(state, level, pos, oldState, movedByPiston);
        scheduleFluidTick(state, level, pos);
    }

    @Override
    protected boolean mayPlaceOn(BlockState state, BlockGetter level, BlockPos pos) {
        return state.is(AQUATIC_SPICE_GROWABLE);
    }

    /** Also requires no {@link #AQUATIC_SPICE_WATER} directly above. */
    @Override
    public boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        return super.canSurvive(state, level, pos) && !level.getFluidState(pos.above()).is(AQUATIC_SPICE_WATER);
    }

    @Override
    protected BlockState updateShape(BlockState state, Direction direction, BlockState neighborState,
            LevelAccessor level, BlockPos pos, BlockPos neighborPos) {
        scheduleFluidTick(state, level, pos);
        return super.updateShape(state, direction, neighborState, level, pos, neighborPos);
    }

    // #region fluid

    @Override
    protected FluidState getFluidState(BlockState state) {
        if (state.getValue(WATERLOGGED))
            return Fluids.WATER.getSource(false);
        int flow = state.getValue(FLOW);
        return flow > 0 ? Fluids.FLOWING_WATER.getFlowing(flow, state.getValue(FALLING)) : super.getFluidState(state);
    }

    /** Lets in source water, and flowing water too if flow-through is enabled, unless already holding water. */
    @Override
    public boolean canPlaceLiquid(@Nullable Player player, BlockGetter level, BlockPos pos, BlockState state,
            Fluid fluid) {
        if (state.getValue(WATERLOGGED) || state.getValue(FLOW) > 0)
            return false;
        return fluid == Fluids.WATER
                || (fluid == Fluids.FLOWING_WATER && Services.CONFIG.isAquaticSpiceFlowThroughEnabled());
    }

    @Override
    public boolean placeLiquid(LevelAccessor level, BlockPos pos, BlockState state, FluidState fluid) {
        if (!canPlaceLiquid(null, level, pos, state, fluid.getType()))
            return false;

        if (!level.isClientSide()) {
            BlockState held = holding(state, fluid);
            level.setBlock(pos, held, Block.UPDATE_ALL);
            scheduleFluidTick(held, level, pos);
        }
        return true;
    }

    /** @return {@code null} if flow-through is disabled, or {@code fluid} isn't water. */
    @Override
    public @Nullable BlockState withPassingFluid(BlockState state, FluidState fluid) {
        if (!Services.CONFIG.isAquaticSpiceFlowThroughEnabled())
            return null;
        if (!fluid.isEmpty() && !fluid.is(FluidTags.WATER))
            return null;
        return holding(state, fluid);
    }

    /**
     * @return {@code state} holding {@code fluid} as water - a source
     *         waterlogs it, anything else passes through as flowing water, and
     *         an empty fluid leaves it dry.
     */
    private static BlockState holding(BlockState state, FluidState fluid) {
        boolean flowing = !fluid.isEmpty() && !fluid.isSource();
        return state.setValue(WATERLOGGED, fluid.isSource())
                .setValue(FLOW, flowing ? Mth.clamp(fluid.getAmount(), 1, 8) : 0)
                .setValue(FALLING, flowing && fluid.hasProperty(FlowingFluid.FALLING)
                        && fluid.getValue(FlowingFluid.FALLING));
    }

    /** Schedules a tick of the water {@code state} holds, if any, so it keeps flowing. */
    private static void scheduleFluidTick(BlockState state, LevelAccessor level, BlockPos pos) {
        FluidState fluid = state.getFluidState();
        if (!fluid.isEmpty())
            level.scheduleTick(pos, fluid.getType(), fluid.getType().getTickDelay(level));
    }

    // #region growth

    /**
     * Gates growth by Spice Region support (see
     * {@link Spice#canBeCultivatedAt(ServerLevel, BlockPos)}) and, if
     * configured, by holding water - source or flowing. A plant failing either
     * isn't destroyed, it just stops growing.
     */
    @Override
    public boolean canGrow(ServerLevel level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        boolean watered = state.is(this) && !state.getFluidState().isEmpty();
        if (!watered && Services.CONFIG.isAquaticSpiceWaterRequired())
            return false;
        return getSpice().canBeCultivatedAt(level, pos);
    }

    /**
     * Rates submerged {@link #AQUATIC_SPICE_GROWABLE} ground like moist
     * farmland, so a flooded bed grows as fast as a watered field.
     */
    @Override
    protected float getGroundFertility(BlockGetter level, BlockPos groundPos) {
        if (!level.getBlockState(groundPos).is(AQUATIC_SPICE_GROWABLE))
            return super.getGroundFertility(level, groundPos);
        return level.getFluidState(groundPos.above()).is(AQUATIC_SPICE_WATER) ? 3.0F : 0.0F;
    }
}
