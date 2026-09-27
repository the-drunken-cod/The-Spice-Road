package com.drunkencod.spice_road.tooltip;

import java.util.List;

import net.minecraft.world.item.ItemStack;

import com.drunkencod.spice_road.Constants;
import com.drunkencod.spice_road.platform.Services;
import com.drunkencod.spice_road.registry.ModDataComponents;
import com.drunkencod.spice_road.spice.SpiceProfiles;

/**
 * Registers the global {@link TooltipUtil} contribution that shows a stack's
 * {@link SpiceProfiles Spice Profile} flavor axis values on Shift, for
 * any item that has one.
 * <p>
 * Actually appending the tooltip to a given stack's hover text still needs a
 * per-loader hook into a global item tooltip event (there's no vanilla/common
 * hook for "every item's tooltip"), since {@code Item#appendHoverText} is
 * only called for items that override it. See
 * {@code NeoForgeSpiceTooltipHandler}/{@code FabricSpiceTooltipHandler} in
 * each loader's client module - both just forward to
 * {@link TooltipUtil#append}, which already knows about this registration.
 */
public final class SpiceProfileTooltips {

    private SpiceProfileTooltips() {
    }

    /**
     * Registers the tooltip contribution. Must be called once during mod
     * init (safe on both client and dedicated server - {@link TooltipUtil}
     * itself is client-agnostic, only ever consulted from client-only code).
     */
    public static void register() {
        TooltipUtil.register(stack -> SpiceProfiles.get(stack).isPresent(), TooltipUtil.Visibility.SHIFT_ONLY,
                stack -> SpiceProfiles.getEffective(stack)
                        .map(profile -> SpiceFlavorTooltips.formatFlavorAxes(profile, barScale(stack)))
                        .orElse(List.of()),
                TooltipUtil.shiftHint(Constants.MOD_ID + ".tooltip.shift_hint.spice"));
    }

    /**
     * @param stack The stack whose profile is shown.
     * @return The flavor soft cap for a Profile Override (e.g. seasoned food,
     *         whose flavor adds up across many spices), otherwise {@code 1},
     *         the conventional range of a single Spice Item.
     */
    private static double barScale(ItemStack stack) {
        return stack.has(ModDataComponents.SPICE_PROFILE.get()) ? Services.CONFIG.getFlavorSoftCap() : 1D;
    }
}
