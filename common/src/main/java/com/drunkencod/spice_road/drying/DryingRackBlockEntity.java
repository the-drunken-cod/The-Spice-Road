package com.drunkencod.spice_road.drying;

import java.util.Collections;
import java.util.Optional;
import java.util.UUID;

import org.jetbrains.annotations.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.Container;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import com.drunkencod.spice_road.advancement.ModCriteriaTriggers;
import com.drunkencod.spice_road.platform.Services;
import com.drunkencod.spice_road.registry.ModBlockEntities;
import com.drunkencod.spice_road.registry.ModRecipeTypes;
import com.drunkencod.spice_road.stats.ModStats;

/**
 * The inventory and drying progress of a {@link DryingRackBlock}.
 * <p>
 * A rack has two <b>sides</b>, {@link #LEFT} and {@link #RIGHT} as seen from
 * its front, each with an input, a primary output and a secondary output. A
 * side dries while its input holds an item with a {@link DryingRecipe}, even
 * while its outputs are occupied; a finished item waits at full progress and
 * delivers the moment both outputs are empty, so one cycle's yield always
 * fits. The outputs may hold several items because a recipe can yield many.
 * <p>
 * The rack is a {@link WorldlyContainer}, so hoppers, pipes and the loaders'
 * item transfer APIs reach it without any loader-specific code: the left and
 * right faces feed their own side's input, the top, front and back share the
 * load between both sides, and the bottom takes outputs. Its container stack
 * limit is 1, which is what keeps wrappers from filling an input with a whole
 * stack; outputs are only ever filled by the rack itself.
 */
public class DryingRackBlockEntity extends BlockEntity implements WorldlyContainer {

    // #region slots

    /** The two sides of a rack. */
    public static final int SIDES = 2;
    /** The side on the viewer's left when standing in front of the rack. */
    public static final int LEFT = 0;
    /** The side on the viewer's right when standing in front of the rack. */
    public static final int RIGHT = 1;

    private static final int SLOT_COUNT = SIDES * 3;
    private static final int[] BOTH_SIDES_LEFT_FIRST = { input(LEFT), input(RIGHT) };
    private static final int[] BOTH_SIDES_RIGHT_FIRST = { input(RIGHT), input(LEFT) };
    private static final int[] LEFT_SIDE = { input(LEFT) };
    private static final int[] RIGHT_SIDE = { input(RIGHT) };
    /** Secondary outputs first: they rest on the primary ones, so they have to leave first. */
    private static final int[] ALL_OUTPUTS = { secondary(LEFT), secondary(RIGHT), output(LEFT), output(RIGHT) };

    private final NonNullList<ItemStack> items = NonNullList.withSize(SLOT_COUNT, ItemStack.EMPTY);
    private final RecipeManager.CachedCheck<SingleRecipeInput, DryingRecipe> recipeCheck = RecipeManager
            .createCheck(ModRecipeTypes.DRYING.get());

    /** Per side, how much of the current cycle is done, from 0 to 1. */
    private final float[] progress = new float[SIDES];
    /** Per side, who put the current input there by hand, for the advancement and stat. */
    private final UUID[] inserters = new UUID[SIDES];
    /** Which side the next automated input goes to when both are equally free. */
    private int nextAutoSide = LEFT;
    private boolean poweredDirty = true;
    private boolean powered;
    private int lastSignal;

    /**
     * @param pos   The rack's position.
     * @param state The rack's block state.
     */
    public DryingRackBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.DRYING_RACK.get(), pos, state);
    }

    /**
     * @param side {@link #LEFT} or {@link #RIGHT}.
     * @return That side's input slot.
     */
    public static int input(int side) {
        return side;
    }

    /**
     * @param side {@link #LEFT} or {@link #RIGHT}.
     * @return That side's primary output slot.
     */
    public static int output(int side) {
        return SIDES + side;
    }

    /**
     * @param side {@link #LEFT} or {@link #RIGHT}.
     * @return That side's secondary output slot.
     */
    public static int secondary(int side) {
        return 2 * SIDES + side;
    }

    private static boolean isInput(int slot) {
        return slot >= 0 && slot < SIDES;
    }

    // #region drying

    /**
     * Advances every side by one tick. Called by the block's ticker on the
     * server.
     *
     * @param level The level.
     * @param pos   The rack's position.
     * @param state The rack's block state.
     */
    public void serverTick(Level level, BlockPos pos, BlockState state) {
        if (poweredDirty) {
            powered = level.hasNeighborSignal(pos);
            poweredDirty = false;
        }
        double speed = Services.CONFIG.getDryingRackSpeedMultiplier()
                * (state.getValue(DryingRackBlock.HEATED) ? Services.CONFIG.getDryingRackHeatedSpeedMultiplier() : 1.0);
        boolean progressed = false;
        for (int side = 0; side < SIDES; side++) {
            Optional<RecipeHolder<DryingRecipe>> recipe = items.get(input(side)).isEmpty() ? Optional.empty()
                    : recipeCheck.getRecipeFor(new SingleRecipeInput(items.get(input(side))), level);
            if (recipe.isEmpty()) {
                if (progress[side] != 0) {
                    progress[side] = 0;
                    progressed = true;
                }
                continue;
            }
            if (powered)
                continue;
            if (progress[side] < 1) {
                progress[side] = Math.min(1f,
                        progress[side] + (float) (speed / recipe.get().value().getDryingTime()));
                progressed = true;
                // Tells clients the item is done and only waiting for room, which stops its smoke.
                if (progress[side] >= 1 && !outputsEmpty(side))
                    itemsChanged();
            }
            if (progress[side] >= 1 && outputsEmpty(side))
                finish(level, side, recipe.get().value());
        }
        if (progressed) {
            // Progress is saved with the chunk, but never worth a block update.
            level.blockEntityChanged(pos);
            updateSignal(level, pos, state);
        }
    }

    /**
     * Turns one side's input into its results, rolling each result's chance,
     * and credits the player who put the item there.
     */
    private void finish(Level level, int side, DryingRecipe recipe) {
        ItemStack dried = items.get(input(side));
        ItemStack primary = roll(recipe.getResult(), level.random);
        ItemStack secondary = recipe.getSecondaryResult().map(output -> roll(output, level.random))
                .orElse(ItemStack.EMPTY);
        items.set(input(side), ItemStack.EMPTY);
        items.set(output(side), primary);
        items.set(secondary(side), secondary);
        progress[side] = 0;
        UUID inserter = inserters[side];
        inserters[side] = null;
        if (inserter != null && level.getServer() != null && (!primary.isEmpty() || !secondary.isEmpty())) {
            ServerPlayer player = level.getServer().getPlayerList().getPlayer(inserter);
            if (player != null) {
                ModCriteriaTriggers.ITEM_DRIED.get().trigger(player, dried, primary);
                ModStats.award(player, ModStats.ITEMS_DRIED);
            }
        }
        itemsChanged();
    }

    private static ItemStack roll(DryingRecipe.Output output, RandomSource random) {
        return random.nextDouble() < output.chance() ? output.stack().copy() : ItemStack.EMPTY;
    }

    /**
     * @param side A side.
     * @return Whether that side holds an item that isn't done drying yet,
     *         whether or not it is being paused. A finished item whose side
     *         still has output waits without counting.
     */
    public boolean isDrying(int side) {
        return !items.get(input(side)).isEmpty() && progress[side] < 1;
    }

    private boolean outputsEmpty(int side) {
        return items.get(output(side)).isEmpty() && items.get(secondary(side)).isEmpty();
    }

    /**
     * Makes the next tick re-read whether the rack is powered. Called when a
     * neighbor changes.
     */
    public void markPoweredDirty() {
        poweredDirty = true;
    }

    /**
     * @return The comparator signal: 1 to 14 for the least advanced side that
     *         is drying, 15 when nothing is drying but an output waits to be
     *         taken, 0 when the rack is idle.
     */
    public int getAnalogSignal() {
        float least = Float.MAX_VALUE;
        for (int side = 0; side < SIDES; side++)
            if (isDrying(side))
                least = Math.min(least, progress[side]);
        if (least != Float.MAX_VALUE)
            return 1 + (int) (Math.min(least, 0.999f) * 14);
        for (int slot = SIDES; slot < SLOT_COUNT; slot++)
            if (!items.get(slot).isEmpty())
                return 15;
        return 0;
    }

    private void updateSignal(Level level, BlockPos pos, BlockState state) {
        int signal = getAnalogSignal();
        if (signal != lastSignal) {
            lastSignal = signal;
            level.updateNeighbourForOutputSignal(pos, state.getBlock());
        }
    }

    // #region hand interaction

    /**
     * @param level The level.
     * @param stack A stack.
     * @return Whether {@code stack} can be dried.
     */
    public static boolean canDry(Level level, ItemStack stack) {
        return !stack.isEmpty() && level.getRecipeManager()
                .getRecipeFor(ModRecipeTypes.DRYING.get(), new SingleRecipeInput(stack), level).isPresent();
    }

    /** @return Whether either side has an empty input. */
    public boolean hasFreeInput() {
        return items.get(input(LEFT)).isEmpty() || items.get(input(RIGHT)).isEmpty();
    }

    /**
     * Hangs one item of {@code stack} on the preferred side, or on the other if
     * that one is taken. Doesn't shrink {@code stack}.
     *
     * @param player        Who inserts it, credited when it finishes.
     * @param stack         The item to dry.
     * @param preferredSide The side to try first.
     * @return Whether an item was hung up.
     */
    public boolean insertByPlayer(Player player, ItemStack stack, int preferredSide) {
        if (level == null || !canDry(level, stack))
            return false;
        int side = items.get(input(preferredSide)).isEmpty() ? preferredSide : 1 - preferredSide;
        if (!items.get(input(side)).isEmpty())
            return false;
        setInput(side, stack.copyWithCount(1));
        inserters[side] = player.getUUID();
        itemsChanged();
        return true;
    }

    /**
     * Takes the first of the clicked side's outputs that holds something:
     * its secondary output, else its primary output (the secondary one rests
     * on it, so it leaves first), and if that side has none, the other
     * side's the same way. With {@code input} set it takes only the clicked
     * side's input instead.
     *
     * @param side  The side that was clicked.
     * @param input Whether to take the input instead of the outputs.
     * @return What was taken, or {@link ItemStack#EMPTY}.
     */
    public ItemStack takeByPlayer(int side, boolean input) {
        int[] slots = input ? new int[] { input(side) }
                : new int[] { secondary(side), output(side), secondary(1 - side), output(1 - side) };
        for (int slot : slots) {
            ItemStack stack = items.get(slot);
            if (stack.isEmpty())
                continue;
            if (isInput(slot))
                setInput(slot, ItemStack.EMPTY);
            else
                items.set(slot, ItemStack.EMPTY);
            itemsChanged();
            return stack;
        }
        return ItemStack.EMPTY;
    }

    /**
     * @param side  A side.
     * @param input Whether to look at the input instead of the outputs.
     * @return Whether {@link #takeByPlayer} would take something.
     */
    public boolean hasAnythingToTake(int side, boolean input) {
        if (input)
            return !items.get(input(side)).isEmpty();
        for (int slot = SIDES; slot < SLOT_COUNT; slot++)
            if (!items.get(slot).isEmpty())
                return true;
        return false;
    }

    private void setInput(int side, ItemStack stack) {
        items.set(input(side), stack);
        progress[side] = 0;
        inserters[side] = null;
        if (!stack.isEmpty())
            nextAutoSide = 1 - side;
    }

    /** Saves, and sends the new contents to every client watching the rack. */
    private void itemsChanged() {
        setChanged();
        if (level != null && !level.isClientSide)
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_ALL);
    }

    // #region container

    @Override
    public int getContainerSize() {
        return SLOT_COUNT;
    }

    @Override
    public boolean isEmpty() {
        return items.stream().allMatch(ItemStack::isEmpty);
    }

    @Override
    public ItemStack getItem(int slot) {
        return items.get(slot);
    }

    @Override
    public ItemStack removeItem(int slot, int amount) {
        ItemStack removed = ContainerHelper.removeItem(items, slot, amount);
        if (!removed.isEmpty()) {
            if (isInput(slot))
                setInput(slot, items.get(slot));
            itemsChanged();
        }
        return removed;
    }

    @Override
    public ItemStack removeItemNoUpdate(int slot) {
        ItemStack removed = ContainerHelper.takeItem(items, slot);
        if (isInput(slot))
            setInput(slot, ItemStack.EMPTY);
        return removed;
    }

    @Override
    public void setItem(int slot, ItemStack stack) {
        if (isInput(slot)) {
            setInput(slot, stack.isEmpty() ? ItemStack.EMPTY : stack.copyWithCount(1));
        } else {
            items.set(slot, stack);
        }
        itemsChanged();
    }

    /**
     * @return {@code 1}: an input holds a single item, and outputs are never
     *         filled from outside, so this only keeps automation from stacking
     *         into inputs. Outputs may still hold a recipe's whole yield.
     */
    @Override
    public int getMaxStackSize() {
        return 1;
    }

    @Override
    public boolean canPlaceItem(int slot, ItemStack stack) {
        return isInput(slot) && items.get(slot).isEmpty() && level != null && canDry(level, stack);
    }

    @Override
    public boolean stillValid(Player player) {
        return Container.stillValidBlockEntity(this, player);
    }

    @Override
    public void clearContent() {
        Collections.fill(items, ItemStack.EMPTY);
        progress[LEFT] = 0;
        progress[RIGHT] = 0;
        itemsChanged();
    }

    // #region automation

    /**
     * @param face A face of the rack.
     * @return {@link #LEFT} or {@link #RIGHT} for the faces that feed one side
     *         only, {@code -1} for the shared top, front and back, and
     *         {@code -2} for the bottom.
     */
    private int sideOfFace(Direction face) {
        if (face == Direction.DOWN)
            return -2;
        Direction facing = getBlockState().getValue(DryingRackBlock.FACING);
        if (face == facing.getClockWise())
            return LEFT;
        if (face == facing.getCounterClockWise())
            return RIGHT;
        return -1;
    }

    @Override
    public int[] getSlotsForFace(Direction face) {
        return switch (sideOfFace(face)) {
            case LEFT -> LEFT_SIDE;
            case RIGHT -> RIGHT_SIDE;
            case -2 -> ALL_OUTPUTS;
            default -> sharedSideFirst() == LEFT ? BOTH_SIDES_LEFT_FIRST : BOTH_SIDES_RIGHT_FIRST;
        };
    }

    /**
     * @return The side automation from the top, front or back fills first: one
     *         that is entirely free if only one is, else the one that didn't
     *         get the last item.
     */
    private int sharedSideFirst() {
        boolean leftFree = isFree(LEFT);
        boolean rightFree = isFree(RIGHT);
        if (leftFree != rightFree)
            return leftFree ? LEFT : RIGHT;
        return nextAutoSide;
    }

    private boolean isFree(int side) {
        return items.get(input(side)).isEmpty() && items.get(output(side)).isEmpty()
                && items.get(secondary(side)).isEmpty();
    }

    @Override
    public boolean canPlaceItemThroughFace(int slot, ItemStack stack, @Nullable Direction face) {
        if (!canPlaceItem(slot, stack))
            return false;
        if (face == null)
            return true;
        int side = sideOfFace(face);
        return side == -1 || side == slot;
    }

    @Override
    public boolean canTakeItemThroughFace(int slot, ItemStack stack, Direction face) {
        if (face != Direction.DOWN || isInput(slot))
            return false;
        // A primary output only leaves once the secondary one resting on it has.
        boolean primary = slot < secondary(LEFT);
        return !primary || items.get(secondary(slot - SIDES)).isEmpty();
    }

    // #region client

    /**
     * Wisps and puffs only the client sees. Called as the rack's block entity
     * data arrives.
     *
     * @param before The input slots before the update.
     */
    private void showFinishedParticles(ItemStack[] before) {
        if (level == null || !level.isClientSide || !Services.CONFIG.areDryingRackParticlesEnabled())
            return;
        for (int side = 0; side < SIDES; side++) {
            boolean finished = !before[side].isEmpty() && items.get(input(side)).isEmpty()
                    && (!items.get(output(side)).isEmpty() || !items.get(secondary(side)).isEmpty());
            if (!finished)
                continue;
            Direction facing = getBlockState().getValue(DryingRackBlock.FACING);
            Direction left = facing.getClockWise();
            double along = (side == LEFT ? 3 : -3) / 16.0;
            double x = worldPosition.getX() + 0.5 + left.getStepX() * along;
            double z = worldPosition.getZ() + 0.5 + left.getStepZ() * along;
            for (int i = 0; i < 6; i++)
                level.addParticle(ParticleTypes.POOF, x + (level.random.nextDouble() - 0.5) * 0.2,
                        worldPosition.getY() + 0.4, z + (level.random.nextDouble() - 0.5) * 0.2, 0, 0.02, 0);
        }
    }

    // #region save and sync

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        ContainerHelper.saveAllItems(tag, items, registries);
        for (int side = 0; side < SIDES; side++) {
            tag.putFloat("progress_" + side, progress[side]);
            if (inserters[side] != null)
                tag.putUUID("inserter_" + side, inserters[side]);
        }
        tag.putInt("next_auto_side", nextAutoSide);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        ItemStack[] before = new ItemStack[SIDES];
        for (int side = 0; side < SIDES; side++)
            before[side] = items.get(input(side));
        Collections.fill(items, ItemStack.EMPTY);
        ContainerHelper.loadAllItems(tag, items, registries);
        for (int side = 0; side < SIDES; side++) {
            progress[side] = tag.getFloat("progress_" + side);
            inserters[side] = tag.hasUUID("inserter_" + side) ? tag.getUUID("inserter_" + side) : null;
        }
        nextAutoSide = tag.getInt("next_auto_side") == RIGHT ? RIGHT : LEFT;
        showFinishedParticles(before);
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        return saveWithoutMetadata(registries);
    }

    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }
}
