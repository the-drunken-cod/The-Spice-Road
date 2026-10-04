package com.drunkencod.spice_road.grinder;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

import com.drunkencod.spice_road.Constants;
import com.drunkencod.spice_road.grinder.GrinderView.Event;

/**
 * The Spice Grinder: a hand-held item that opens the seasoning GUI when used.
 * It never stacks, since it can hold a run in progress.
 */
public class SpiceGrinderItem extends Item {

    /** Translation key of the GUI's title. */
    public static final String TITLE_KEY = "container." + Constants.MOD_ID + ".spice_grinder";

    /** Creates the item, one per stack. */
    public SpiceGrinderItem() {
        super(new Properties().stacksTo(1));
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (player instanceof ServerPlayer serverPlayer) {
            int slot = hand == InteractionHand.MAIN_HAND ? player.getInventory().selected : 40;
            serverPlayer.openMenu(new SimpleMenuProvider(
                    (containerId, inventory, ignored) -> new SpiceGrinderMenu(containerId, inventory, slot),
                    Component.translatable(TITLE_KEY)));
            if (serverPlayer.containerMenu instanceof SpiceGrinderMenu menu)
                menu.syncView(Event.NONE);
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
    }
}
