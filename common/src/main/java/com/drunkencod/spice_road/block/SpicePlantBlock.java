package com.drunkencod.spice_road.block;

import java.util.function.Supplier;

import org.jetbrains.annotations.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.TagKey;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.FarmBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

import com.drunkencod.spice_road.Constants;
import com.drunkencod.spice_road.platform.Services;
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

    /**
     * Natural ground Spice Plants survive on besides farmland, like mushrooms
     * on mycelium. {@link SpiceSeedItem} defers to {@link #mayPlaceOn} for
     * its own ground check, so this also governs where seeds may be planted.
     * Used directly by {@link FlowerPatchBlock}; for {@code CROP} Spices,
     * only {@link WildSpiceCropBlock} accepts this ground - see
     * {@link SpiceCropBlock#mayPlaceOn}.
     */
    public static final TagKey<Block> SPICE_GROWABLE = TagKey.create(Registries.BLOCK,
            ResourceLocation.fromNamespaceAndPath(Constants.MOD_ID, "spice_growable"));

    /**
     * Per-age outline shapes, indexed by {@code age} (0-7, matching vanilla's
     * fixed {@code CropBlock.AGE} property range - see {@link #getAgeProperty()}).
     */
    private static final VoxelShape[] SHAPE_BY_AGE = new VoxelShape[8];

    static {
        for (int age = 0; age < SHAPE_BY_AGE.length; age++) {
            double height = Math.min(16.0D, (age + 1) * 3.0D);
            SHAPE_BY_AGE[age] = Block.box(0.0D, 0.0D, 0.0D, 16.0D, height, 16.0D);
        }
    }

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
     * Widened from vanilla's farmland-only check to also accept
     * {@link #SPICE_GROWABLE} ground, so wild Spice Plants can generate and
     * survive outside of farms.
     * <p>
     * {@link SpiceCropBlock} overrides this back to farmland-only for its
     * farmed variant - see {@link WildSpiceCropBlock} for the worldgen-only
     * counterpart that restores this wider check.
     */
    @Override
    protected boolean mayPlaceOn(BlockState state, BlockGetter level, BlockPos pos) {
        return state.getBlock() instanceof FarmBlock || state.is(SPICE_GROWABLE);
    }

    /**
     * Returns {@link Constants#DEFAULT_SPICE_PLANT_GROWTH_STAGES}, not the
     * live {@code IConfigHelper#getSpicePlantGrowthStages()} config value.
     */
    @Override
    public int getMaxAge() {
        return Constants.DEFAULT_SPICE_PLANT_GROWTH_STAGES;
    }

    /**
     * Reimplements {@code CropBlock#getShape} against {@link #SHAPE_BY_AGE}
     * instead of vanilla's {@code SHAPE_BY_AGE}, since Spice Plants use a
     * steeper per-age height formula.
     */
    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE_BY_AGE[getAge(state)];
    }

    /**
     * Scales vanilla's per-tick growth odds by
     * {@code IConfigHelper#getSpicePlantGrowthSpeedMultiplier(Tier)} for this
     * block's {@link Spice}'s {@link com.drunkencod.spice_road.spice.Tier}.
     * <p>
     * Unlike {@link #getMaxAge()}, this runs at genuine tick-time (not during
     * block-state-freeze), so it's safe to read live config here.
     */
    @Override
    protected void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        if (!canGrow(level, pos))
            return;

        SpiceGrowth.rollScaled(Services.CONFIG.getSpicePlantGrowthSpeedMultiplier(spice.getTier()), random,
                () -> super.randomTick(state, level, pos, random));
    }

    /**
     * Growth gate checked before every {@link #randomTick}, on top of
     * vanilla's own light/farmland-fertility odds.
     * <p>
     * Defaults to always-growable; {@link SpiceCropBlock} overrides this to
     * permanently stunt a planted Spice at stage 0 when Spice Region support
     * (see {@code SpiceRegionResolver}) doesn't back it at this position.
     *
     * @param level The server level - growth gating only ever runs at
     *              tick-time, so this is always authoritative, unlike
     *              {@code canSurvive} which also runs client-side.
     * @param pos   The position of this Spice Plant.
     * @return Whether this Spice Plant is allowed to advance its growth stage.
     */
    public boolean canGrow(ServerLevel level, BlockPos pos) {
        return true;
    }

    /**
     * Gates vanilla bonemeal the same way {@link #randomTick} gates natural
     * growth.
     */
    @Override
    public void performBonemeal(ServerLevel level, RandomSource random, BlockPos pos, BlockState state) {
        if (!canGrow(level, pos))
            return;

        super.performBonemeal(level, random, pos, state);
    }

    @Override
    protected int getBonemealAgeIncrease(Level level) {
        if (getSpice().getTier().getRarity() == Rarity.EPIC)
            return Mth.nextInt(level.random, 0, 2);
        return Mth.nextInt(level.random, 1, 2);
    }

    // #region hand-pick

    /**
     * Mature Spices with a Hand-Pick and a Harvest Tool Requirement are
     * harvested by right-clicking with an item from their harvest tool tag,
     * which takes durability damage.
     */
    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos,
            Player player, InteractionHand hand, BlockHitResult hitResult) {
        if (!isHandPickable(state) || !spice.requiresHarvestTool() || !SpiceHarvesting.isHarvestTool(spice, stack)
                || !SpiceHarvesting.mayHarvest(spice, player))
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;

        handPick(state, level, pos, player, hitResult, SoundEvents.SHEEP_SHEAR);
        if (!level.isClientSide())
            stack.hurtAndBreak(1, player, LivingEntity.getSlotForHand(hand));
        return ItemInteractionResult.sidedSuccess(level.isClientSide());
    }

    /**
     * Mature Spices with a Hand-Pick but no Harvest Tool Requirement are
     * harvested by right-clicking with an empty hand.
     */
    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player,
            BlockHitResult hitResult) {
        if (!isHandPickable(state) || spice.requiresHarvestTool() || !SpiceHarvesting.mayHarvest(spice, player))
            return InteractionResult.PASS;

        handPick(state, level, pos, player, hitResult, SoundEvents.SWEET_BERRY_BUSH_PICK_BERRIES);
        return InteractionResult.sidedSuccess(level.isClientSide());
    }

    private boolean isHandPickable(BlockState state) {
        return spice.requiresHandPick() && isMaxAge(state);
    }

    /**
     * Drops the plant's Spice from the clicked face if the Spice Region
     * supports it here (see {@link #canGrow}), then resets the plant to age
     * {@code 0} so it regrows instead of breaking. The break loot table of a
     * Hand-Pick Spice only drops the seed, so this is the only way to obtain
     * its Spice.
     *
     * @param sound The harvest sound, played at the plant's position.
     */
    private void handPick(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult,
            SoundEvent sound) {
        if (!(level instanceof ServerLevel serverLevel))
            return;

        int yield = SpiceHarvesting.getPlantYield(spice);
        if (yield > 0 && canGrow(serverLevel, pos))
            popResourceFromFace(level, pos, hitResult.getDirection(),
                    new ItemStack(Spice.getRawById(spice.getId()), yield));

        level.playSound(null, pos, sound, SoundSource.BLOCKS, 1.0F, 0.8F + level.getRandom().nextFloat() * 0.4F);

        BlockState harvestedState = state.setValue(getAgeProperty(), 0);
        level.setBlock(pos, harvestedState, Block.UPDATE_CLIENTS);
        level.gameEvent(GameEvent.BLOCK_CHANGE, pos, GameEvent.Context.of(player, harvestedState));
    }

    // #region break-harvest

    /**
     * Charges the harvest tool a mature Spice Plant was broken with, so that
     * breaking it is equivalent to harvesting it by interaction (see
     * {@link SpiceHarvesting#hurtHarvestTool}). Which of the two the break
     * itself drops is decided by the loot table, see
     * {@code SpiceBreakHarvest}.
     * <p>
     * Deliberately not {@code playerWillDestroy}: that runs before the tool
     * the loot table is rolled against is captured, so breaking the tool on
     * its last durability point would lose the Spice.
     */
    @Override
    public void playerDestroy(Level level, Player player, BlockPos pos, BlockState state,
            @Nullable BlockEntity blockEntity, ItemStack tool) {
        super.playerDestroy(level, player, pos, state, blockEntity, tool);

        if (isMaxAge(state))
            SpiceHarvesting.hurtHarvestTool(spice, level, player, tool);
    }

    // #region misc

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
