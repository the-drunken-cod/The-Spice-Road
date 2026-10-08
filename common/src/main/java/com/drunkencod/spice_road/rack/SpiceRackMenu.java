package com.drunkencod.spice_road.rack;

import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

import com.drunkencod.spice_road.registry.ModMenus;

/**
 * The menu of a Spice Rack: its slots in two rows of four, the upper tier's
 * slots on top, then the player's inventory.
 */
public class SpiceRackMenu extends AbstractContainerMenu {

    /** Width of the GUI. */
    public static final int WIDTH = 176;
    /** Height of the GUI. */
    public static final int HEIGHT = 150;
    /** Left edge of the first slot of a row. */
    public static final int SLOT_X = 53;
    /** Top edge of the upper tier's row. */
    public static final int UPPER_ROW_Y = 18;
    /** Top edge of the lower tier's row. */
    public static final int LOWER_ROW_Y = 36;
    /** Top edge of the inventory's first row. */
    public static final int INVENTORY_Y = 67;
    /** Top edge of the hotbar. */
    public static final int HOTBAR_Y = 125;
    /** Where the inventory's label goes. */
    public static final int INVENTORY_LABEL_Y = 56;

    private final Container rack;

    /**
     * Client-side constructor, as created from the menu type.
     *
     * @param containerId The menu ID.
     * @param inventory   The player's inventory.
     */
    public SpiceRackMenu(int containerId, Inventory inventory) {
        this(containerId, inventory, new SimpleContainer(SpiceRackBlockEntity.SLOT_COUNT) {
            @Override
            public boolean canPlaceItem(int slot, ItemStack stack) {
                return SpiceRackBlockEntity.accepts(stack);
            }
        });
    }

    /**
     * @param containerId The menu ID.
     * @param inventory   The player's inventory.
     * @param rack        The rack's contents.
     */
    public SpiceRackMenu(int containerId, Inventory inventory, Container rack) {
        super(ModMenus.SPICE_RACK.get(), containerId);
        checkContainerSize(rack, SpiceRackBlockEntity.SLOT_COUNT);
        this.rack = rack;
        rack.startOpen(inventory.player);
        for (int slot = 0; slot < SpiceRackBlockEntity.SLOT_COUNT; slot++) {
            boolean upper = slot >= SpiceRackBlockEntity.TIER_SIZE;
            addSlot(new RackSlot(rack, slot, SLOT_X + (slot % SpiceRackBlockEntity.TIER_SIZE) * 18,
                    upper ? UPPER_ROW_Y : LOWER_ROW_Y));
        }
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++)
                addSlot(new Slot(inventory, col + row * 9 + 9, 8 + col * 18, INVENTORY_Y + row * 18));
        }
        for (int col = 0; col < 9; col++)
            addSlot(new Slot(inventory, col, 8 + col * 18, HOTBAR_Y));
    }

    @Override
    public boolean stillValid(Player player) {
        return rack.stillValid(player);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        Slot slot = slots.get(index);
        if (!slot.hasItem())
            return ItemStack.EMPTY;
        ItemStack stack = slot.getItem();
        ItemStack original = stack.copy();
        boolean moved = index < SpiceRackBlockEntity.SLOT_COUNT
                ? moveItemStackTo(stack, SpiceRackBlockEntity.SLOT_COUNT, slots.size(), true)
                : moveItemStackTo(stack, 0, SpiceRackBlockEntity.SLOT_COUNT, false);
        if (!moved)
            return ItemStack.EMPTY;
        if (stack.isEmpty())
            slot.setByPlayer(ItemStack.EMPTY);
        else
            slot.setChanged();
        return original;
    }

    @Override
    public void removed(Player player) {
        super.removed(player);
        rack.stopOpen(player);
    }

    /** A slot of the rack, which only takes what the rack accepts. */
    private static final class RackSlot extends Slot {

        RackSlot(Container container, int slot, int x, int y) {
            super(container, slot, x, y);
        }

        @Override
        public boolean mayPlace(ItemStack stack) {
            return container.canPlaceItem(getContainerSlot(), stack);
        }
    }
}
