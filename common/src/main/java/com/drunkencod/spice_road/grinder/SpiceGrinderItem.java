package com.drunkencod.spice_road.grinder;

import java.util.List;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import com.drunkencod.spice_road.Constants;
import com.drunkencod.spice_road.grinder.GrinderView.Event;
import com.drunkencod.spice_road.platform.Services;
import com.drunkencod.spice_road.registry.ModDataComponents;

/**
 * The Spice Grinder: a hand-held item that opens the seasoning GUI when used.
 * It never stacks, since it can hold a run in progress.
 */
public class SpiceGrinderItem extends Item {

    /** Translation key of the GUI's title. */
    public static final String TITLE_KEY = "container." + Constants.MOD_ID + ".spice_grinder";

    /** Translation key of the tooltip line telling what the item is for. */
    private static final String USAGE_TOOLTIP_KEY = Constants.MOD_ID + ".tooltip.grinder_usage";

    /** Translation key of the tooltip line naming the food of a run in progress. */
    private static final String SESSION_TOOLTIP_KEY = Constants.MOD_ID + ".tooltip.grinder_session";

    /** Creates the item, one per stack. */
    public SpiceGrinderItem() {
        super(new Properties().stacksTo(1));
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip,
            TooltipFlag flag) {
        super.appendHoverText(stack, context, tooltip, flag);
        tooltip.add(Component.translatable(USAGE_TOOLTIP_KEY).withStyle(ChatFormatting.GRAY));
        SeasoningSession session = stack.get(ModDataComponents.GRINDER_SESSION.get());
        if (session != null)
            tooltip.add(Component.translatable(SESSION_TOOLTIP_KEY, session.food().getCount(),
                    session.food().getHoverName()).withStyle(ChatFormatting.GRAY));
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (player instanceof ServerPlayer serverPlayer) {
            int slot = hand == InteractionHand.MAIN_HAND ? player.getInventory().selected : 40;
            serverPlayer.openMenu(new SimpleMenuProvider(
                    (containerId, inventory, ignored) -> new SpiceGrinderMenu(containerId, inventory, slot),
                    Component.translatable(TITLE_KEY).withStyle(
                            Services.CONFIG.isDarkMode() ? ChatFormatting.WHITE : ChatFormatting.DARK_GRAY)));
            if (serverPlayer.containerMenu instanceof SpiceGrinderMenu menu)
                menu.syncView(Event.NONE);
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
    }
}
