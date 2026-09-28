package com.drunkencod.spice_road.datagen;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;

/**
 * Every vanilla planks {@link Item}, for Spice Tree logs to datagen a
 * conversion recipe into (see {@link SpiceTreePlanksRecipeProvider}).
 */
public enum VanillaPlanksType {
    OAK(Items.OAK_PLANKS),
    SPRUCE(Items.SPRUCE_PLANKS),
    BIRCH(Items.BIRCH_PLANKS),
    JUNGLE(Items.JUNGLE_PLANKS),
    ACACIA(Items.ACACIA_PLANKS),
    DARK_OAK(Items.DARK_OAK_PLANKS),
    MANGROVE(Items.MANGROVE_PLANKS),
    CHERRY(Items.CHERRY_PLANKS),
    BAMBOO(Items.BAMBOO_PLANKS),
    CRIMSON(Items.CRIMSON_PLANKS),
    WARPED(Items.WARPED_PLANKS);

    private final Item planks;

    VanillaPlanksType(Item planks) {
        this.planks = planks;
    }

    /** @return This type's vanilla planks item. */
    public Item getPlanks() {
        return planks;
    }
}
