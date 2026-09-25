package com.drunkencod.multi_loader_template.registry;

import net.minecraft.resources.ResourceLocation;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Supplier;

/**
 * Central item registry.
 * <p>
 * Add items here using {@link IRegistryHelper#registerItem(String, Supplier)},
 * then call
 * {@link #addForBulkModel(ResourceLocation)} so the datagen helper knows to
 * generate a
 * flat item model for them automatically.
 */
public class ModItems {

    /**
     * Resource locations of items that should receive an auto-generated flat item
     * model
     * (i.e. {@code minecraft:item/generated} parent with a single {@code layer0}
     * texture).
     * Populated by the loader-specific entry point during mod initialisation.
     */
    public static final List<ResourceLocation> FLAT_ITEM_MODEL_IDS = new ArrayList<>();

    /**
     * Mark an item as needing a bulk-generated flat item model.
     *
     * @param loc The full {@link ResourceLocation} of the item (e.g.
     *            {@code multi_loader_template:my_item})
     */
    public static void addForBulkModel(ResourceLocation loc) {
        FLAT_ITEM_MODEL_IDS.add(loc);
    }

    public static List<ResourceLocation> getFlatItemModelIds() {
        return Collections.unmodifiableList(FLAT_ITEM_MODEL_IDS);
    }
}
