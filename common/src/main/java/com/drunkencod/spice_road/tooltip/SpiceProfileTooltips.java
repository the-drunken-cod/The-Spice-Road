package com.drunkencod.spice_road.tooltip;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.StringUtil;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import com.drunkencod.spice_road.Constants;
import com.drunkencod.spice_road.item.SpiceItemTags;
import com.drunkencod.spice_road.mix.SpiceMixes;
import com.drunkencod.spice_road.platform.Services;
import com.drunkencod.spice_road.registry.ModDataComponents;
import com.drunkencod.spice_road.spice.Seasoning;
import com.drunkencod.spice_road.spice.SpiceProfile;
import com.drunkencod.spice_road.spice.SpiceProfiles;
import com.drunkencod.spice_road.spice.Tier;
import com.drunkencod.spice_road.spice.effect.SeasoningEffect;
import com.drunkencod.spice_road.spice.effect.SeasoningEffectRegistry;
import com.drunkencod.spice_road.spice.effect.SeasoningEffects;

/**
 * Registers the global {@link TooltipUtil} contributions that show a stack's
 * {@link Tier} (for items in a {@link Tier#getItemTag() tier tag}) and its
 * {@link SpiceProfiles Spice Profile} flavor axis values on Shift for Spice
 * Items, and the counted Flavor Contributors of seasoned food.
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

    /**
     * Translation key of one Flavor Contributor, taking its item name and its
     * amount as arguments (e.g. {@code "Cinnamon x2"}).
     */
    private static final String FLAVOR_CONTRIBUTOR_KEY = Constants.MOD_ID + ".tooltip.flavor_contributor";

    /**
     * Translation key of the first Flavor Contributors line, taking as many
     * comma-separated contributors as fit on it as its only argument (see
     * {@link #flavorContributorsLines}).
     */
    private static final String FLAVOR_CONTRIBUTORS_KEY = Constants.MOD_ID + ".tooltip.flavor_contributors";

    /**
     * Translation key of the usage line of planting items bound to their Spice
     * Region.
     */
    private static final String REGION_BOUND_KEY = Constants.MOD_ID + ".tooltip.region_bound";

    /** Translation key of the header above the spices a Spice Mix holds. */
    private static final String MIX_CONTENTS_KEY = Constants.MOD_ID + ".tooltip.spice_mix.contents";

    /** Translation key of the header above a seasoned food's effects. */
    private static final String SEASONING_EFFECTS_KEY = Constants.MOD_ID + ".tooltip.seasoning_effects";

    /** Tick rate effect durations are shown against. */
    private static final float TICKS_PER_SECOND = 20F;

    /** Pixel width a Flavor Contributors list wraps at. */
    private static final int CONTRIBUTORS_LINE_WIDTH = 200;

    /** Score magnitude at which a Spice Item's flavor bar is full. */
    private static final double SPICE_ITEM_BAR_SCALE = 1D;

    private SpiceProfileTooltips() {
    }

    /**
     * Registers the tooltip contribution. Must be called once during mod
     * init (safe on both client and dedicated server - {@link TooltipUtil}
     * itself is client-agnostic, only ever consulted from client-only code).
     */
    public static void register() {
        // Spice Items with a profile get their tier line merged into the Shift hint
        // instead
        TooltipUtil.register(
                stack -> tierOf(stack).isPresent()
                        && (!stack.is(SpiceItemTags.SPICES) || SpiceProfiles.get(stack).isEmpty()),
                TooltipUtil.Visibility.ALWAYS,
                stack -> tierOf(stack)
                        .map(tier -> List.of(tierLineWithUsage(stack, tier)))
                        .orElse(List.of()));
        TooltipUtil.register(stack -> stack.has(ModDataComponents.SPICE_MIX.get()), TooltipUtil.Visibility.ALWAYS,
                SpiceProfileTooltips::mixContentsLines);
        TooltipUtil.register(stack -> displayProfile(stack).isPresent() || stack.has(ModDataComponents.SEASONING.get()),
                TooltipUtil.Visibility.SHIFT_ONLY,
                SpiceProfileTooltips::shiftLines, SpiceProfileTooltips::shiftHintLine);
    }

    /**
     * @param stack The stack being hovered.
     * @param tier  Its tier.
     * @return The tier line, followed on the same line by the
     *         {@link #REGION_BOUND_KEY} usage hint in gray if
     *         {@link #isRegionBound} holds for {@code stack}.
     */
    private static Component tierLineWithUsage(ItemStack stack, Tier tier) {
        MutableComponent line = tierLine(tier, lineKeyOf(stack)).copy();
        if (isRegionBound(stack))
            line.append(" ").append(Component.translatable(REGION_BOUND_KEY).withStyle(ChatFormatting.GRAY));
        return line;
    }

    /**
     * @param stack The stack to check.
     * @return Whether {@code stack} is the planting item of a Spice that, going
     *         by the current config, can only be planted in the Spice Region
     *         that supports it (see {@code Spice#canBeCultivatedAt}), such as
     *         seeds, cuttings and saplings of a high harvest difficulty.
     */
    private static boolean isRegionBound(ItemStack stack) {
        if (stack.is(SpiceItemTags.SPICES) || !Services.CONFIG.isSpiceRegionPlantingRestricted())
            return false;
        return tierOf(stack)
                .filter(tier -> tier.getMinHarvestDifficulty() > Services.CONFIG.getSpiceHardyHarvestDifficulty())
                .isPresent();
    }

    /**
     * @param stack A filled Spice Mix.
     * @return The "Contains:" header and one line per held spice with its count.
     */
    private static List<Component> mixContentsLines(ItemStack stack) {
        List<Component> lines = new ArrayList<>();
        SpiceMixes.contentsOf(stack).ifPresent(mix -> {
            lines.add(Component.translatable(MIX_CONTENTS_KEY).withStyle(ChatFormatting.GRAY));
            mix.spices().forEach((item, count) -> lines.add(Component.literal(" ")
                    .append(Component.translatable(FLAVOR_CONTRIBUTOR_KEY, item.getDescription(), count))
                    .withStyle(ChatFormatting.DARK_GRAY)));
        });
        return lines;
    }

    /**
     * @param stack The stack to look up.
     * @return The Effective Profile of a Spice Item, or of a filled Spice Mix
     *         (see {@link SpiceProfiles#getEffectiveMix}).
     */
    private static Optional<SpiceProfile> displayProfile(ItemStack stack) {
        Optional<SpiceProfile> profile = SpiceProfiles.getEffective(stack);
        return profile.isPresent() ? profile : SpiceProfiles.getEffectiveMix(stack);
    }

    /**
     * @param stack The stack being hovered.
     * @return The lines shown while Shift is held: a Spice Item's tier line
     *         (which is merged into {@link #shiftHintLine(ItemStack)} while Shift
     *         isn't held) and flavor axes, a Spice Mix's averaged flavor axes, or
     *         a seasoned food's Flavor Contributors.
     */
    private static List<Component> shiftLines(ItemStack stack) {
        List<Component> lines = new ArrayList<>();
        if (stack.is(SpiceItemTags.SPICES))
            tierOf(stack).map(tier -> tierLine(tier, lineKeyOf(stack))).ifPresent(lines::add);
        displayProfile(stack)
                .map(profile -> SpiceFlavorTooltips.formatFlavorAxes(profile, SPICE_ITEM_BAR_SCALE))
                .ifPresent(lines::addAll);

        Seasoning seasoning = stack.get(ModDataComponents.SEASONING.get());
        if (seasoning != null && !seasoning.contributors().isEmpty())
            lines.addAll(flavorContributorsLines(seasoning));
        if (seasoning != null && !seasoning.effects().isEmpty())
            lines.addAll(seasoningEffectLines(seasoning));

        return lines;
    }

    /**
     * @param seasoning The stack's {@link ModDataComponents#SEASONING}.
     * @return The comma-separated contributors with their amounts,
     *         word-wrapped at {@link #CONTRIBUTORS_LINE_WIDTH}, with the first
     *         line prefixed via {@link #FLAVOR_CONTRIBUTORS_KEY}, e.g.
     *         {@code "Seasoned with: Cinnamon x2, Nutmeg x1.5,"} followed by
     *         {@code "Habanero x1"} on its own line.
     */
    private static List<Component> flavorContributorsLines(Seasoning seasoning) {
        Map<Item, Double> contributors = seasoning.contributors();
        Component space = Component.literal(" ");
        int spaceWidth = SpiceFlavorTooltips.measureWidth(space);

        List<Component> rawLines = new ArrayList<>();
        MutableComponent line = Component.empty();
        int lineWidth = 0;
        int index = 0;
        for (Map.Entry<Item, Double> contributor : contributors.entrySet()) {
            boolean isLast = ++index == contributors.size();
            MutableComponent token = Component.translatable(FLAVOR_CONTRIBUTOR_KEY,
                    contributor.getKey().getDefaultInstance().getHoverName(),
                    BigDecimal.valueOf(contributor.getValue()).stripTrailingZeros().toPlainString());
            if (!isLast)
                token.append(",");
            int tokenWidth = SpiceFlavorTooltips.measureWidth(token);

            if (lineWidth > 0 && lineWidth + spaceWidth + tokenWidth > CONTRIBUTORS_LINE_WIDTH) {
                rawLines.add(line);
                line = Component.empty();
                lineWidth = 0;
            }
            if (lineWidth > 0) {
                line.append(space);
                lineWidth += spaceWidth;
            }
            line.append(token);
            lineWidth += tokenWidth;
        }
        rawLines.add(line);

        List<Component> lines = new ArrayList<>(rawLines.size());
        lines.add(Component.translatable(FLAVOR_CONTRIBUTORS_KEY, rawLines.get(0)).withStyle(ChatFormatting.GRAY));
        for (Component continuation : rawLines.subList(1, rawLines.size()))
            lines.add(continuation.copy().withStyle(ChatFormatting.GRAY));
        return lines;
    }

    /**
     * @param seasoning The stack's {@link ModDataComponents#SEASONING}.
     * @return A header line followed by one line per effect that is still in
     *         the catalog, like vanilla's potion tooltips: name, level and
     *         duration, in the color of the mob effect's category.
     */
    private static List<Component> seasoningEffectLines(Seasoning seasoning) {
        double multiplier = Services.CONFIG.getSeasoningEffectDurationMultiplier();
        List<Component> lines = new ArrayList<>();
        for (SeasoningEffect effect : seasoning.effects()) {
            SeasoningEffectRegistry.get(effect.id()).ifPresent(def -> {
                MobEffect mobEffect = def.effect().value();
                MutableComponent line = mobEffect.getDisplayName().copy();
                if (effect.level() > 1)
                    line = Component.translatable("potion.withAmplifier", line,
                            Component.translatable("potion.potency." + (effect.level() - 1)));
                int duration = SeasoningEffects.durationTicks(def, effect.level(), multiplier);
                line.append(" (" + StringUtil.formatTickDuration(duration, TICKS_PER_SECOND) + ")");
                lines.add(line.withStyle(mobEffect.getCategory().getTooltipFormatting()));
            });
        }
        if (!lines.isEmpty())
            lines.add(0, Component.translatable(SEASONING_EFFECTS_KEY).withStyle(ChatFormatting.GRAY));
        return lines;
    }

    /**
     * @param stack The stack being hovered.
     * @return The line shown in place of {@link #shiftLines(ItemStack)} while
     *         Shift isn't held - for a tiered Spice Item, the tier line merged
     *         with the hint, otherwise the plain hint.
     */
    private static Component shiftHintLine(ItemStack stack) {
        boolean isSpice = stack.is(SpiceItemTags.SPICES) || stack.has(ModDataComponents.SPICE_MIX.get());
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
}
