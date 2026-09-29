package com.drunkencod.spice_road.tooltip;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
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

    /** Base translation key of the Shift hint shown in place of the flavor axes. */
    private static final String SHIFT_HINT_KEY = Constants.MOD_ID + ".tooltip.shift_hint.spice";

    /**
     * Base translation key of a tier line's wording, taking the tier's name as
     * its only argument (e.g. {@code "%s spice"}).
     */
    private static final String TIER_LINE_KEY = Constants.MOD_ID + ".tooltip.spice_tier.line";

    private SpiceProfileTooltips() {
    }

    /**
     * Registers the tooltip contribution. Must be called once during mod
     * init (safe on both client and dedicated server - {@link TooltipUtil}
     * itself is client-agnostic, only ever consulted from client-only code).
     */
    public static void register() {
        TooltipUtil.register(stack -> tierOf(stack).isPresent() && !stack.is(SpiceItemTags.SPICES),
                TooltipUtil.Visibility.ALWAYS,
                stack -> tierOf(stack)
                        .map(tier -> List.of(tierLine(tier, lineKeyOf(stack))))
                        .orElse(List.of()));
        TooltipUtil.register(stack -> SpiceProfiles.get(stack).isPresent(), TooltipUtil.Visibility.SHIFT_ONLY,
                SpiceProfileTooltips::shiftLines, SpiceProfileTooltips::shiftHintLine);
    }

    /**
     * @param stack The stack being hovered.
     * @return The lines shown while Shift is held: the flavor axes, preceded by
     *         a Spice Item's tier line (which is merged into
     *         {@link #shiftHintLine(ItemStack)} while Shift isn't held).
     */
    private static List<Component> shiftLines(ItemStack stack) {
        List<Component> lines = new ArrayList<>();
        if (stack.is(SpiceItemTags.SPICES))
            tierOf(stack).map(tier -> tierLine(tier, lineKeyOf(stack))).ifPresent(lines::add);
        SpiceProfiles.getEffective(stack)
                .map(profile -> SpiceFlavorTooltips.formatFlavorAxes(profile, barScale(stack)))
                .ifPresent(lines::addAll);
        return lines;
    }

    /**
     * @param stack The stack being hovered.
     * @return The line shown in place of {@link #shiftLines(ItemStack)} while
     *         Shift isn't held - for a tiered Spice Item, the tier line merged
     *         with the hint, otherwise the plain hint.
     */
    private static Component shiftHintLine(ItemStack stack) {
        boolean isSpice = stack.is(SpiceItemTags.SPICES);
        Optional<Tier> tier = isSpice ? tierOf(stack) : Optional.empty();
        if (tier.isPresent())
            return spiceHintLine(tier.get(), lineKeyOf(stack));
        return TooltipUtil.shiftHint(SHIFT_HINT_KEY, SHIFT_HINT_KEY + (isSpice ? ".spice" : ".seasoned"), isSpice);
    }

    /**
     * @param tier    The tier of the hovered Spice Item.
     * @param lineKey Translation key of the tier line's wording.
     * @return A line like {@code "[icon] Rare spice ingredient. Hold [Shift] for
     *         information."}, with the tier line in its rarity color and the
     *         hint in dark gray.
     */
    private static Component spiceHintLine(Tier tier, String lineKey) {
        return Component.empty()
                .append(tierLine(tier, lineKey))
                .append(" ")
                .append(Component.translatable(SHIFT_HINT_KEY + ".hold", TooltipUtil.shiftKeyName())
                        .withStyle(ChatFormatting.DARK_GRAY));
    }

    /**
     * @param stack The stack being hovered.
     * @return The {@link #TIER_LINE_KEY} variant describing what kind of item
     *         {@code stack} is - a Spice Item, its seeds, or anything else in a
     *         {@link Tier#getItemTag() tier tag} (e.g. saplings and vines).
     */
    private static String lineKeyOf(ItemStack stack) {
        if (stack.is(SpiceItemTags.SPICES))
            return TIER_LINE_KEY + ".spice";
        return TIER_LINE_KEY + (stack.is(SpiceItemTags.SPICE_PLANT_SEEDS) ? ".seeds" : ".item");
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
     * @param tier    The tier to describe.
     * @param lineKey Translation key of the wording, taking the tier's name as
     *                its only argument (see {@link #lineKeyOf}).
     * @return A line like {@code "[icon] Epic spice ingredient."}, with the icon
     *         from {@link #TIER_ICON_FONT} and the text in the tier's rarity
     *         color.
     */
    private static Component tierLine(Tier tier, String lineKey) {
        return Component.empty()
                .append(tierIcon(tier))
                .append(" ")
                .append(Component.translatable(lineKey, tierName(tier)).withStyle(tier.getRarity().color()));
    }

    /**
     * @param tier The tier to describe.
     * @return The tier's icon from {@link #TIER_ICON_FONT}.
     */
    private static Component tierIcon(Tier tier) {
        return Component.literal(String.valueOf((char) (TIER_ICON_FIRST_CHAR + tier.ordinal())))
                .withStyle(style -> style.withFont(TIER_ICON_FONT).withColor(ChatFormatting.WHITE));
    }

    /**
     * @param tier The tier to describe.
     * @return The tier's name, like {@code "Rare"}, uncolored.
     */
    private static MutableComponent tierName(Tier tier) {
        return Component.translatable(Constants.MOD_ID + ".tooltip.spice_tier." + tier.getSerializedName());
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
