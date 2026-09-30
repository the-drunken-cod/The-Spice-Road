package com.drunkencod.spice_road.datagen;

import net.minecraft.resources.ResourceLocation;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

import com.drunkencod.spice_road.Constants;
import com.drunkencod.spice_road.registry.ModItems;
import com.drunkencod.spice_road.spice.Spice;

/**
 * Helper for bulk item model generation.
 * <p>
 * Items added via {@link #addFlatItem(String, Spice)} automatically get a
 * {@code minecraft:item/generated} model with a single {@code layer0} texture
 * sourced from their Spice's item texture folder (see
 * {@link SpiceAssetPaths#item}).
 *
 * <p>
 * Usage in mod init:
 *
 * <pre>{@code
 * ItemModelHelper.addFlatItem("cinnamon_sapling", Spice.CINNAMON);
 * }</pre>
 */
public class ItemModelHelper {

    /** Item path IDs (no namespace) queued for flat model generation, mapped to their texture. */
    private static final Map<String, ResourceLocation> FLAT_ITEMS = new LinkedHashMap<>();

    /**
     * Register an item id for flat ({@code item/generated}) model generation.
     * Also registers the item in {@link ModItems#FLAT_ITEM_MODEL_IDS} for
     * cross-loader use.
     *
     * @param id    Registry path of the item (e.g. {@code "cinnamon_sapling"})
     * @param spice The Spice whose texture folder holds the item's texture.
     */
    public static void addFlatItem(String id, Spice spice) {
        FLAT_ITEMS.put(id, SpiceAssetPaths.item(spice, id));
        ModItems.addForBulkModel(ResourceLocation.fromNamespaceAndPath(Constants.MOD_ID, id));
    }

    /** @return Unmodifiable view of the flat item path IDs, mapped to their {@code layer0} texture. */
    public static Map<String, ResourceLocation> getFlatItems() {
        return Collections.unmodifiableMap(FLAT_ITEMS);
    }
}
