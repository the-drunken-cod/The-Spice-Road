package com.drunkencod.spice_road.tooltip;

import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import java.util.function.Predicate;

import com.drunkencod.spice_road.platform.Services;

/**
 * Generic registry for item tooltip content that should render either always
 * or only while the player holds Shift.
 * <p>
 * This class is deliberately client-agnostic, so it takes
 * an explicit {@code shiftDown} boolean supplied by the caller. That keeps
 * this utility itself free of any client-only dependency and usable from
 * common code.
 * <p>
 * Usage from an item's own override:
 *
 * <pre>{@code
 * @Override
 * public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltipComponents,
 *         TooltipFlag tooltipFlag) {
 *     super.appendHoverText(stack, context, tooltipComponents, tooltipFlag);
 *     TooltipUtil.append(stack, tooltipComponents, Screen.hasShiftDown());
 * }
 * }</pre>
 */
public class TooltipUtil {

    /** When a registered tooltip contribution should be shown. */
    public enum Visibility {
        /** Always appended, regardless of Shift state. */
        ALWAYS,
        /**
         * Only appended while Shift is held, unless
         * {@code IConfigHelper#isTooltipShiftBypassed()} is enabled.
         */
        SHIFT_ONLY
    }

    /** One registered tooltip contribution. */
    private record Entry(Predicate<ItemStack> matcher, Visibility visibility,
            Function<ItemStack, List<Component>> content) {
    }

    private static final List<Entry> ENTRIES = new ArrayList<>();

    private TooltipUtil() {
    }

    /**
     * Register tooltip content for every {@link ItemStack} of a specific item.
     *
     * @param item       The item to attach the tooltip content to
     * @param visibility Whether the content should always render or only on
     *                   Shift
     * @param content    Produces the tooltip lines to append for a given stack
     */
    public static void register(Item item, Visibility visibility, Function<ItemStack, List<Component>> content) {
        register(stack -> stack.is(item), visibility, content);
    }

    /**
     * Register tooltip content for any {@link ItemStack} matching an arbitrary
     * predicate (e.g. a tag check, or an {@code instanceof} check on a custom
     * item class), for cases where a single item instance isn't a fine enough
     * filter.
     *
     * @param matcher    Decides which stacks the content applies to
     * @param visibility Whether the content should always render or only on
     *                   Shift
     * @param content    Produces the tooltip lines to append for a given stack
     */
    public static void register(Predicate<ItemStack> matcher, Visibility visibility,
            Function<ItemStack, List<Component>> content) {
        ENTRIES.add(new Entry(matcher, visibility, content));
    }

    /**
     * Appends all registered tooltip content applicable to {@code stack} into
     * {@code tooltip}. Call this from {@code Item#appendHoverText}.
     *
     * @param stack     The stack currently being hovered
     * @param tooltip   The mutable tooltip list to append lines to
     * @param shiftDown Whether Shift is currently held. The caller must
     *                  determine this itself (typically via
     *                  {@code Screen.hasShiftDown()} on the client), since this
     *                  utility must not depend on client-only classes
     */
    public static void append(ItemStack stack, List<Component> tooltip, boolean shiftDown) {
        boolean showShiftContent = shiftDown || Services.CONFIG.isTooltipShiftBypassed();
        for (Entry entry : ENTRIES) {
            if (entry.visibility() == Visibility.SHIFT_ONLY && !showShiftContent) {
                continue;
            }
            if (!entry.matcher().test(stack)) {
                continue;
            }
            tooltip.addAll(entry.content().apply(stack));
        }
    }
}
