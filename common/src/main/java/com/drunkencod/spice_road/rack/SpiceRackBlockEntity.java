package com.drunkencod.spice_road.rack;

import java.util.stream.IntStream;

import org.jetbrains.annotations.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.Container;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BaseContainerBlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import com.drunkencod.spice_road.Constants;
import com.drunkencod.spice_road.grinder.GrinderSpice;
import com.drunkencod.spice_road.item.SpiceItemTags;
import com.drunkencod.spice_road.registry.ModBlockEntities;

/**
 * The inventory of a {@link SpiceRackBlock}: two tiers of {@value #TIER_SIZE}
 * slots, the lower tier first. Every face and slot is open to hoppers and the
 * loaders' item transfer APIs, limited to what {@link #accepts} allows, and
 * each slot takes the item's full stack size. Slots fill up and empty from the
 * last to the first, whether by hand or by automation, which is why it is a
 * {@link WorldlyContainer}. Players can name it like a chest.
 * <p>
 * Its items are saved under {@code Items} like any container's, which is how
 * the Spice Grinder reads spice storage.
 */
public class SpiceRackBlockEntity extends BaseContainerBlockEntity implements WorldlyContainer {

    /** Slots per tier, which is also how many items fit in one row or ring. */
    public static final int TIER_SIZE = 4;
    /** The lower tier is slots {@code 0} to {@code 3}, the upper tier {@code 4} to {@code 7}. */
    public static final int TIERS = 2;
    /** Slots in total. */
    public static final int SLOT_COUNT = TIER_SIZE * TIERS;

    private static final String TITLE_KEY = "container." + Constants.MOD_ID + ".spice_rack";

    /** Every slot, last to first, which is the order automation fills and empties them in. */
    private static final int[] SLOTS_LAST_FIRST = IntStream.rangeClosed(1, SLOT_COUNT)
            .map(i -> SLOT_COUNT - i).toArray();

    private NonNullList<ItemStack> items = NonNullList.withSize(SLOT_COUNT, ItemStack.EMPTY);

    /**
     * @param pos   The rack's position.
     * @param state The rack's block state.
     */
    public SpiceRackBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.SPICE_RACK.get(), pos, state);
    }

    /**
     * @param stack A stack.
     * @return Whether a rack takes it: a Spice Item or a Spice Mix, or an item
     *         of {@link SpiceItemTags#SPICE_RACK_STORABLE}.
     */
    public static boolean accepts(ItemStack stack) {
        return !stack.isEmpty() && (GrinderSpice.of(stack).isPresent() || stack.is(SpiceItemTags.SPICE_RACK_STORABLE));
    }

    /**
     * @param rack A rack's contents.
     * @return Whether every slot holds a full stack.
     */
    public static boolean isFull(Container rack) {
        for (int slot = 0; slot < rack.getContainerSize(); slot++) {
            ItemStack stack = rack.getItem(slot);
            if (stack.isEmpty() || stack.getCount() < stack.getMaxStackSize())
                return false;
        }
        return true;
    }

    /**
     * Puts as much of {@code stack} as fits into the rack, topping up matching
     * stacks before using empty slots, the last slot first. Shrinks
     * {@code stack} by what was inserted.
     *
     * @param stack What to insert, if the rack {@link #accepts} it.
     * @return How many items were inserted.
     */
    public int insert(ItemStack stack) {
        if (!accepts(stack))
            return 0;
        int before = stack.getCount();
        for (int slot : SLOTS_LAST_FIRST) {
            if (stack.isEmpty())
                break;
            ItemStack present = items.get(slot);
            if (present.isEmpty() || !ItemStack.isSameItemSameComponents(present, stack))
                continue;
            int moved = Math.min(stack.getCount(), present.getMaxStackSize() - present.getCount());
            if (moved > 0) {
                present.grow(moved);
                stack.shrink(moved);
            }
        }
        for (int slot : SLOTS_LAST_FIRST) {
            if (stack.isEmpty())
                break;
            if (items.get(slot).isEmpty())
                items.set(slot, stack.split(Math.min(stack.getCount(), stack.getMaxStackSize())));
        }
        int inserted = before - stack.getCount();
        if (inserted > 0)
            setChanged();
        return inserted;
    }

    // #region container

    @Override
    protected Component getDefaultName() {
        return Component.translatable(TITLE_KEY);
    }

    @Override
    protected NonNullList<ItemStack> getItems() {
        return items;
    }

    @Override
    protected void setItems(NonNullList<ItemStack> items) {
        this.items = items;
    }

    @Override
    public int getContainerSize() {
        return SLOT_COUNT;
    }

    @Override
    public boolean canPlaceItem(int slot, ItemStack stack) {
        return accepts(stack);
    }

    @Override
    public int[] getSlotsForFace(Direction face) {
        return SLOTS_LAST_FIRST;
    }

    @Override
    public boolean canPlaceItemThroughFace(int slot, ItemStack stack, @Nullable Direction face) {
        return canPlaceItem(slot, stack);
    }

    @Override
    public boolean canTakeItemThroughFace(int slot, ItemStack stack, Direction face) {
        return true;
    }

    @Override
    public void clearContent() {
        super.clearContent();
        setChanged();
    }

    @Override
    protected AbstractContainerMenu createMenu(int containerId, Inventory inventory) {
        return new SpiceRackMenu(containerId, inventory, this);
    }

    // #region save and sync

    /** Saves, and sends the new contents to every client watching the rack. */
    @Override
    public void setChanged() {
        super.setChanged();
        if (level != null && !level.isClientSide)
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_ALL);
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        ContainerHelper.saveAllItems(tag, items, registries);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        items = NonNullList.withSize(SLOT_COUNT, ItemStack.EMPTY);
        ContainerHelper.loadAllItems(tag, items, registries);
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
