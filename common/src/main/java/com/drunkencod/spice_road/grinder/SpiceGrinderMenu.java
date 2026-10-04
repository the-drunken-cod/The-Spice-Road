package com.drunkencod.spice_road.grinder;

import java.util.LinkedHashMap;
import java.util.Map;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import com.drunkencod.spice_road.platform.Services;
import com.drunkencod.spice_road.registry.ModDataComponents;
import com.drunkencod.spice_road.registry.ModMenus;
import com.drunkencod.spice_road.spice.Flavoring;

/**
 * The menu of the Spice Grinder GUI: one food slot and the player's inventory.
 * The spice list, the board and the buttons are not slots; they are drawn by
 * the screen and act through {@link GrinderIntentPayload}s. The menu is bound to
 * the inventory slot the Grinder is in and closes itself when that slot stops
 * holding one.
 * <p>
 * While drafting, the draft (the chosen spices) lives only here on the server,
 * so closing the menu drops it and nothing was consumed.
 */
public class SpiceGrinderMenu extends AbstractContainerMenu {

    /** Index of the food slot among the menu's slots. */
    public static final int FOOD_SLOT = 0;

    private final Inventory inventory;
    private final int grinderSlot;
    private final SimpleContainer foodContainer = new SimpleContainer(1);
    private final Map<Item, Integer> draft = new LinkedHashMap<>();
    private GrinderView view = GrinderView.EMPTY;

    /**
     * Client-side constructor, as created from the menu type.
     *
     * @param containerId The menu ID.
     * @param inventory   The player's inventory.
     */
    public SpiceGrinderMenu(int containerId, Inventory inventory) {
        this(containerId, inventory, -1);
    }

    /**
     * @param containerId The menu ID.
     * @param inventory   The player's inventory.
     * @param grinderSlot The inventory slot holding the Grinder, {@code -1} on the client.
     */
    public SpiceGrinderMenu(int containerId, Inventory inventory, int grinderSlot) {
        super(ModMenus.SPICE_GRINDER.get(), containerId);
        this.inventory = inventory;
        this.grinderSlot = grinderSlot;

        addSlot(new FoodSlot(foodContainer, 0, GrinderLayout.FOOD_SLOT_X, GrinderLayout.FOOD_SLOT_Y));
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++)
                addSlot(new Slot(inventory, col + row * 9 + 9, GrinderLayout.INVENTORY_X + col * 18,
                        GrinderLayout.INVENTORY_Y + row * 18));
        }
        for (int col = 0; col < 9; col++)
            addSlot(new Slot(inventory, col, GrinderLayout.INVENTORY_X + col * 18, GrinderLayout.HOTBAR_Y));
        foodContainer.addListener(container -> slotsChanged(container));
    }

    /** @return The Grinder item stack this menu belongs to; empty on the client. */
    public ItemStack grinder() {
        return grinderSlot < 0 ? ItemStack.EMPTY : inventory.getItem(grinderSlot);
    }

    /**
     * @return The run in progress on the Grinder, {@code null} while drafting.
     *         A placeholder (what a client's copy of the Grinder carries, see
     *         {@link SeasoningSession#PLACEHOLDER}) is not a run.
     */
    public SeasoningSession session() {
        SeasoningSession session = grinder().get(ModDataComponents.GRINDER_SESSION.get());
        return SeasoningSession.isPlaceholder(session) ? null : session;
    }

    /** @return The inventory the menu was opened for. */
    public Inventory inventory() {
        return inventory;
    }

    /** @return The food in the food slot while drafting. */
    public ItemStack food() {
        return foodContainer.getItem(0);
    }

    /** Takes the food out of the slot. */
    void clearFood() {
        foodContainer.setItem(0, ItemStack.EMPTY);
    }

    /** @return The draft: how many of each spice item per food were chosen. Server-side only. */
    Map<Item, Integer> draft() {
        return draft;
    }

    /** @return The latest view the server sent; only the client keeps it. */
    public GrinderView view() {
        return view;
    }

    /**
     * Client-side. Keeps what the server just sent.
     *
     * @param newView The view to show.
     */
    public void setView(GrinderView newView) {
        view = newView;
    }

    /**
     * Server-side. Sends the player the current view.
     *
     * @param event What the last action did.
     */
    public void syncView(GrinderView.Event event) {
        if (inventory.player instanceof ServerPlayer player)
            Services.NETWORK.sendToPlayer(player, new GrinderViewPayload(containerId, GrinderActions.viewOf(this, event)));
    }

    @Override
    public void slotsChanged(Container container) {
        super.slotsChanged(container);
        if (container == foodContainer && !inventory.player.level().isClientSide())
            syncView(GrinderView.Event.NONE);
    }

    @Override
    public boolean stillValid(Player player) {
        if (grinderSlot < 0)
            return true;
        return inventory.getItem(grinderSlot).getItem() instanceof SpiceGrinderItem;
    }

    @Override
    public void removed(Player player) {
        super.removed(player);
        if (!player.level().isClientSide())
            clearContainer(player, foodContainer);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        Slot slot = slots.get(index);
        if (!slot.hasItem())
            return ItemStack.EMPTY;
        ItemStack stack = slot.getItem();
        ItemStack original = stack.copy();
        if (index == FOOD_SLOT) {
            if (!moveItemStackTo(stack, 1, slots.size(), true))
                return ItemStack.EMPTY;
        } else if (!slots.get(FOOD_SLOT).mayPlace(stack) || !moveItemStackTo(stack, FOOD_SLOT, FOOD_SLOT + 1, false)) {
            return ItemStack.EMPTY;
        }
        if (stack.isEmpty())
            slot.setByPlayer(ItemStack.EMPTY);
        else
            slot.setChanged();
        return original;
    }

    /**
     * @param stack A stack.
     * @return Whether it can be seasoned: a Flavor Carrier that isn't seasoned yet.
     */
    public static boolean isSeasonable(ItemStack stack) {
        return Flavoring.isFlavorCarrier(stack) && !stack.has(ModDataComponents.SEASONING.get());
    }

    /** The slot the food goes in; only usable while drafting. */
    private final class FoodSlot extends Slot {

        private FoodSlot(Container container, int index, int x, int y) {
            super(container, index, x, y);
        }

        @Override
        public boolean mayPlace(ItemStack stack) {
            return isDrafting() && isSeasonable(stack);
        }

        @Override
        public boolean mayPickup(Player player) {
            return isDrafting();
        }

        @Override
        public boolean isActive() {
            return isDrafting();
        }

        private boolean isDrafting() {
            if (inventory.player.level().isClientSide())
                return view.phase() == GrinderView.Phase.DRAFT;
            return session() == null;
        }
    }
}
