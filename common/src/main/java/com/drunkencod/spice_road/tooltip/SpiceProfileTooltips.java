package com.drunkencod.spice_road.tooltip;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

import com.drunkencod.spice_road.Constants;
import com.drunkencod.spice_road.item.SpiceItemTags;
import com.drunkencod.spice_road.platform.Services;
import com.drunkencod.spice_road.registry.ModDataComponents;
import com.drunkencod.spice_road.spice.SpiceProfiles;
import com.drunkencod.spice_road.spice.Tier;

/**
 * Registers the global {@link TooltipUtil} contributions that show a stack's
 * {@link Tier} (for items in a {@link Tier#getItemTag() tier tag}) and its
 * {@link SpiceProfiles Spice Profile} flavor axis values on Shift, for any
 * item that has one.
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

    /**
     * Bitmap font mapping one private use codepoint per {@link Tier} (starting
     * at {@link #TIER_ICON_FIRST_CHAR}, in enum order) to its Region Heart map
     * marker texture.
     */
    private static final ResourceLocation TIER_ICON_FONT = ResourceLocation.fromNamespaceAndPath(Constants.MOD_ID,
            "spice_tier_icons");

    /** Codepoint of the first {@link Tier}'s icon in {@link #TIER_ICON_FONT}. */
    private static final char TIER_ICON_FIRST_CHAR = '';

    private SpiceProfileTooltips() {
    }

    /**
     * Registers the tooltip contribution. Must be called once during mod
     * init (safe on both client and dedicated server - {@link TooltipUtil}
     * itself is client-agnostic, only ever consulted from client-only code).
     */
    public static void register() {
        TooltipUtil.register(stack -> tierOf(stack).isPresent(), TooltipUtil.Visibility.ALWAYS,
                stack -> tierOf(stack).map(tier -> List.of(tierLine(tier))).orElse(List.of()));
        TooltipUtil.register(stack -> SpiceProfiles.get(stack).isPresent(), TooltipUtil.Visibility.SHIFT_ONLY,
                stack -> SpiceProfiles.getEffective(stack)
                        .map(profile -> SpiceFlavorTooltips.formatFlavorAxes(profile,
                                barScale(stack)))
                        .orElse(List.of()),
                stack -> TooltipUtil.shiftHint(Constants.MOD_ID + ".tooltip.shift_hint.spice",
                        Constants.MOD_ID + ".tooltip.shift_hint.spice."
                                + (stack.is(SpiceItemTags.SPICES) ? "spice"
                                        : "seasoned"),
                        stack.is(SpiceItemTags.SPICES)));
    }

    /**
     * @param stack The stack to check.
     * @return The {@link Tier} whose {@link Tier#getItemTag() tag} contains
     *         {@code stack}, or empty if none does.
     */
    private static Optional<Tier> tierOf(ItemStack stack) {
        return Arrays.stream(Tier.values()).filter(tier -> stack.is(tier.getItemTag())).findFirst();
    }

    /**
     * @param tier The tier to describe.
     * @return A line like {@code "[icon] Epic rarity"}, with the icon
     *         from {@link #TIER_ICON_FONT} and the text in the tier's rarity
     *         color.
     */
    private static Component tierLine(Tier tier) {
        Component icon = Component.literal(String.valueOf((char) (TIER_ICON_FIRST_CHAR + tier.ordinal())))
                .withStyle(style -> style.withFont(TIER_ICON_FONT).withColor(ChatFormatting.WHITE));
        return Component.empty()
                .append(icon)
                .append(" ")
                .append(Component
                        .translatable(Constants.MOD_ID + ".tooltip.spice_tier."
                                + tier.getSerializedName())
                        .withStyle(tier.getRarity().color()));
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
