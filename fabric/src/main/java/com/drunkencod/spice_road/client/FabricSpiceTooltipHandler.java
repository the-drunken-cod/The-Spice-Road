package com.drunkencod.spice_road.client;

import net.fabricmc.fabric.api.client.item.v1.ItemTooltipCallback;
import net.minecraft.client.gui.screens.Screen;

import com.drunkencod.spice_road.tooltip.SpiceProfileTooltips;
import com.drunkencod.spice_road.tooltip.TooltipUtil;

/**
 * Fabric-side hook that appends {@link SpiceProfileTooltips}' Spice Profile
 * lines to <i>any</i> item's tooltip - see
 * {@code NeoForgeSpiceTooltipHandler}'s javadoc for why this is needed
 * (there's no vanilla per-item override for datapack-registered spices).
 */
public final class FabricSpiceTooltipHandler {

    private FabricSpiceTooltipHandler() {
    }

    /** Registers the tooltip callback. Must be called once during client init. */
    public static void register() {
        ItemTooltipCallback.EVENT
                .register((stack, context, flag, lines) -> TooltipUtil.append(stack, lines, Screen.hasShiftDown()));
    }
}
