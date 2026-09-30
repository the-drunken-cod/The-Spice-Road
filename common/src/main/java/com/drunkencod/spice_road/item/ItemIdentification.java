package com.drunkencod.spice_road.item;

import net.minecraft.world.item.ItemStack;

/**
 * Tag-based runtime checks for identifying Spice items. Deliberately has no
 * dependency on the {@code Spice} enum - see {@link SpiceItemTags} for why
 * tags are used instead.
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
     * @return {@code true} if {@code stack} is a Processed Spice item
     *         ({@link SpiceItemTags#PROCESSED_SPICES})
     */
    public static boolean isProcessedSpice(ItemStack stack) {
        return stack.is(SpiceItemTags.PROCESSED_SPICES);
    }

    /**
     * @param stack The stack to check
     * @return {@code true} if {@code stack} is a raw or Processed Spice item
     */
    public static boolean isAnySpice(ItemStack stack) {
        return isRawSpice(stack) || isProcessedSpice(stack);
    }
}
