package com.drunkencod.spice_road.datagen;

import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.drunkencod.spice_road.Constants;
import com.drunkencod.spice_road.registry.ModItems;

/**
 * Helper for bulk item model generation.
 * <p>
 * Items added via {@link #addFlatItem(String)} automatically get a
 * {@code minecraft:item/generated} model with a single {@code layer0} texture
 * sourced from
 * {@code spice_road:item/<id>}.
 *
 * <p>
 * Usage in mod init:
 * 
 * <pre>{@code
 * ItemModelHelper.addFlatItem("my_item");
 * }</pre>
 */
public class ItemModelHelper {

    /** Item path IDs (no namespace) queued for flat model generation. */
    private static final List<String> FLAT_ITEM_IDS = new ArrayList<>();

    /**
     * Register an item id for flat ({@code item/generated}) model generation.
     * Also registers the item in {@link ModItems#FLAT_ITEM_MODEL_IDS} for
     * cross-loader use.
     *
     * @param id Registry path of the item (e.g. {@code "my_item"})
     */
    public static void addFlatItem(String id) {
        FLAT_ITEM_IDS.add(id);
        ModItems.addForBulkModel(ResourceLocation.fromNamespaceAndPath(Constants.MOD_ID, id));
    }

    /** @return Unmodifiable view of the flat item path IDs. */
    public static List<String> getFlatItemIds() {
        return Collections.unmodifiableList(FLAT_ITEM_IDS);
    }
}
