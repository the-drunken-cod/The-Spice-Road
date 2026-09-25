package com.drunkencod.spice_road.item;

import net.minecraft.world.item.ItemStack;

/**
 * Tag-based runtime checks for identifying Spice items and cutting tools.
 * <p>
 * Used by Spice Plant harvest/interaction logic (e.g. "is the item the player
 * is holding a valid cutting tool for this harvest," "is this ItemStack a
 * Spice item at all"). Deliberately has no dependency on the {@code Spice}
 * enum - see {@link SpiceItemTags} for why tags are used instead.
 */
public class ItemIdentification {

    private ItemIdentification() {
    }

    /**
     * @param stack The stack to check
     * @return {@code true} if {@code stack} is a raw Spice item
     *         ({@link SpiceItemTags#RAW_SPICES})
     */
    public static boolean isRawSpice(ItemStack stack) {
        return stack.is(SpiceItemTags.RAW_SPICES);
    }

    /**
     * @param stack The stack to check
     * @return {@code true} if {@code stack} is a dried Spice item
     *         ({@link SpiceItemTags#DRIED_SPICES})
     */
    public static boolean isDriedSpice(ItemStack stack) {
        return stack.is(SpiceItemTags.DRIED_SPICES);
    }

    /**
     * @param stack The stack to check
     * @return {@code true} if {@code stack} is a raw or dried Spice item
     */
    public static boolean isAnySpice(ItemStack stack) {
        return isRawSpice(stack) || isDriedSpice(stack);
    }

    /**
     * @param stack The stack to check
     * @return {@code true} if {@code stack} counts as a cutting tool for
     *         harvest-tool-requirement checks
     *         ({@link SpiceItemTags#CUTTING_TOOLS})
     */
    public static boolean isCuttingTool(ItemStack stack) {
        return stack.is(SpiceItemTags.CUTTING_TOOLS);
    }
}
