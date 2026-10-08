package com.drunkencod.spice_road.grinder;

import java.util.LinkedHashMap;
import java.util.Map;

import org.jetbrains.annotations.Nullable;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.DataSlot;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;

import com.drunkencod.spice_road.platform.Services;
import com.drunkencod.spice_road.registry.ModDataComponents;
import com.drunkencod.spice_road.registry.ModMenus;
import com.drunkencod.spice_road.spice.Flavoring;

/**
 * The menu of the Spice Grinder GUI: one food slot and the player's inventory.
 * The spice list, the board and the buttons are not slots; they are drawn by
 * the screen and act through {@link GrinderIntentPayload}s. The menu is bound
 * to
 * the inventory slot the Grinder is in and closes itself when that slot stops
 * holding one.
 * <p>
 * While drafting, the draft (the chosen spices) lives only here on the server,
 * so closing the menu drops it and nothing was consumed.
 */
public class SpiceGrinderMenu extends AbstractContainerMenu {

    /** Index of the food slot among the menu's slots. */
    public static final int FOOD_SLOT = 0;

    /**
     * Ticks between looks at what spices the inventory and nearby storage hold, to
     * tell the client when it changed.
     */
    private static final int AVAILABLE_CHECK_TICKS = 20;

    /** Furthest a player may be from a placed Grinder, squared, as for vanilla block menus. */
    private static final double MAX_BLOCK_DISTANCE_SQR = 64D;

    private final Inventory inventory;
    private final int grinderSlot;
    private final @Nullable SpiceGrinderBlockEntity block;
    private final SimpleContainer foodContainer = new SimpleContainer(1);
    /**
     * The inventory slot holding the Grinder, kept in sync with the client so both
     * lock it.
     */
    private final DataSlot lockedSlot = DataSlot.standalone();
    private final Map<GrinderSpice, Integer> draft = new LinkedHashMap<>();
    private GrinderView view = GrinderView.EMPTY;
    private Map<GrinderSpice, Integer> lastAvailable = Map.of();

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
     * @param grinderSlot The inventory slot holding the Grinder, {@code -1} on the
     *                    client.
     */
    public SpiceGrinderMenu(int containerId, Inventory inventory, int grinderSlot) {
        this(containerId, inventory, grinderSlot, null);
    }

    /**
     * @param containerId The menu ID.
     * @param inventory   The player's inventory.
     * @param grinderSlot The inventory slot holding the Grinder, {@code -1} if
     *                    there is none.
     * @param block       The placed Grinder the menu is for, {@code null} if it
     *                    is for the item.
     */
    private SpiceGrinderMenu(int containerId, Inventory inventory, int grinderSlot,
            @Nullable SpiceGrinderBlockEntity block) {
        super(ModMenus.SPICE_GRINDER.get(), containerId);
        this.inventory = inventory;
        this.grinderSlot = grinderSlot;
        this.block = block;
        lockedSlot.set(grinderSlot);
        addDataSlot(lockedSlot);

        addSlot(new FoodSlot(foodContainer, 0, GrinderLayout.FOOD_SLOT_X, GrinderLayout.FOOD_SLOT_Y));
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++)
                addSlot(new InventorySlot(inventory, col + row * 9 + 9, GrinderLayout.INVENTORY_X + col * 18,
                        GrinderLayout.INVENTORY_Y + row * 18));
        }
        for (int col = 0; col < 9; col++)
            addSlot(new InventorySlot(inventory, col, GrinderLayout.INVENTORY_X + col * 18, GrinderLayout.HOTBAR_Y));
        foodContainer.addListener(container -> slotsChanged(container));
    }

    /**
     * Opens the GUI of a Grinder for a player and sends them its first view.
     *
     * @param player      The player.
     * @param grinderSlot The inventory slot holding the Grinder item, ignored if
     *                    {@code block} is given.
     * @param block       The placed Grinder to open, {@code null} to open the
     *                    item in {@code grinderSlot}.
     */
    public static void open(ServerPlayer player, int grinderSlot, @Nullable SpiceGrinderBlockEntity block) {
        player.openMenu(new SimpleMenuProvider((containerId, inventory, ignored) -> {
            SpiceGrinderMenu menu = new SpiceGrinderMenu(containerId, inventory, block == null ? grinderSlot : -1,
                    block);
            if (block != null)
                block.setUser(player);
            return menu;
        }, Component.translatable(SpiceGrinderItem.TITLE_KEY).withStyle(
                Services.CONFIG.isDarkMode() ? ChatFormatting.WHITE : ChatFormatting.DARK_GRAY)));
        if (player.containerMenu instanceof SpiceGrinderMenu menu)
            menu.syncView(GrinderView.Event.NONE);
    }

    /** @return The placed Grinder this menu is for; {@code null} for the item and on the client. */
    public @Nullable SpiceGrinderBlockEntity block() {
        return block;
    }

    /**
     * @return The run in progress on the Grinder, {@code null} while drafting.
     *         A placeholder (what a client's copy of the Grinder carries, see
     *         {@link SeasoningSession#PLACEHOLDER}) is not a run.
     */
    public SeasoningSession session() {
        if (block != null)
            return block.session();
        SeasoningSession session = grinderSlot < 0 ? null
                : inventory.getItem(grinderSlot).get(ModDataComponents.GRINDER_SESSION.get());
        return SeasoningSession.isPlaceholder(session) ? null : session;
    }

    /**
     * Server-side. Saves the run on the Grinder.
     *
     * @param session The new state of the run, {@code null} to end it.
     */
    void setSession(@Nullable SeasoningSession session) {
        if (block != null)
            block.setSession(session);
        else if (grinderSlot >= 0 && session != null)
            inventory.getItem(grinderSlot).set(ModDataComponents.GRINDER_SESSION.get(), session);
        else if (grinderSlot >= 0)
            inventory.getItem(grinderSlot).remove(ModDataComponents.GRINDER_SESSION.get());
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

    /**
     * @return The draft: how many of each loose spice and kind of Spice Mix per
     *         food were chosen. Server-side only.
     */
    Map<GrinderSpice, Integer> draft() {
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
            Services.NETWORK.sendToPlayer(player,
                    new GrinderViewPayload(containerId, GrinderActions.viewOf(this, event)));
    }

    @Override
    public void broadcastChanges() {
        super.broadcastChanges();
        if (inventory.player instanceof ServerPlayer player && player.tickCount % AVAILABLE_CHECK_TICKS == 0) {
            Map<GrinderSpice, Integer> now = SpiceSources.of(player).available();
            if (!now.equals(lastAvailable)) {
                lastAvailable = now;
                syncView(GrinderView.Event.NONE);
            }
        }
    }

    @Override
    public void slotsChanged(Container container) {
        super.slotsChanged(container);
        if (container == foodContainer && !inventory.player.level().isClientSide())
            syncView(GrinderView.Event.NONE);
    }

    /**
     * @param slot One of the menu's slots.
     * @return Whether it is the inventory slot the Grinder is in, which can't
     *         be touched while the GUI is open so the Grinder can't be moved,
     *         dropped or swapped away from under its own run.
     */
    public boolean isLocked(Slot slot) {
        return slot.container == inventory && slot.getContainerSlot() == lockedSlot.get();
    }

    /**
     * Besides the locked slot itself, a hotbar or off hand key press would swap
     * the Grinder out of a slot without ever touching that slot, so those are
     * refused too.
     */
    @Override
    public void clicked(int slotId, int button, ClickType clickType, Player player) {
        if (clickType == ClickType.SWAP && lockedSlot.get() >= 0 && button == lockedSlot.get())
            return;
        super.clicked(slotId, button, clickType, player);
    }

    @Override
    public boolean stillValid(Player player) {
        if (block != null)
            return !block.isRemoved() && player.level().getBlockEntity(block.getBlockPos()) == block
                    && player.distanceToSqr(Vec3.atCenterOf(block.getBlockPos())) <= MAX_BLOCK_DISTANCE_SQR;
        if (grinderSlot < 0)
            return true;
        return inventory.getItem(grinderSlot).getItem() instanceof SpiceGrinderItem;
    }

    @Override
    public void removed(Player player) {
        super.removed(player);
        if (!player.level().isClientSide())
            clearContainer(player, foodContainer);
        if (block != null)
            block.setUser(null);
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
        } else if (!slots.get(FOOD_SLOT).mayPlace(stack) || !moveItemStackTo(stack, FOOD_SLOT, FOOD_SLOT + 1, false))
            return ItemStack.EMPTY;
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

    /**
     * A slot of the player's inventory, locked while it is the one holding the
     * Grinder.
     */
    private final class InventorySlot extends Slot {

        private InventorySlot(Container container, int index, int x, int y) {
            super(container, index, x, y);
        }

        @Override
        public boolean mayPickup(Player player) {
            return !isLocked(this);
        }

        @Override
        public boolean mayPlace(ItemStack stack) {
            return !isLocked(this);
        }
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
