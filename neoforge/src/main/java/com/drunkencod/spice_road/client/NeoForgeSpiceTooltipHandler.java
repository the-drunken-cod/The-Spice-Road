package com.drunkencod.spice_road.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.player.ItemTooltipEvent;

import com.drunkencod.spice_road.tooltip.SpiceFlavorTooltips;
import com.drunkencod.spice_road.tooltip.SpiceProfileTooltips;
import com.drunkencod.spice_road.tooltip.TooltipUtil;

/**
 * NeoForge-side hook that appends {@link SpiceProfileTooltips}' Spice
 * Profile lines to <i>any</i> item's tooltip, not just items this mod's own
 * classes override {@code appendHoverText} on.
 */
public final class NeoForgeSpiceTooltipHandler {

    private NeoForgeSpiceTooltipHandler() {
    }

    /** Registers the tooltip listener. Must be called once during client setup. */
    public static void register() {
        SpiceFlavorTooltips.setTextWidthMeasurer(text -> Minecraft.getInstance().font.width(text));
        NeoForge.EVENT_BUS.addListener(NeoForgeSpiceTooltipHandler::onItemTooltip);
    }

    private static void onItemTooltip(ItemTooltipEvent event) {
        TooltipUtil.append(event.getItemStack(), event.getToolTip(), Screen.hasShiftDown());
    }
}
