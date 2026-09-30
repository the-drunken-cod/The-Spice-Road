package com.drunkencod.spice_road.registry;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Supplier;

import org.jetbrains.annotations.Nullable;

import net.minecraft.world.item.Items;

import com.drunkencod.spice_road.Constants;
import com.drunkencod.spice_road.block.SpiceTree;
import com.drunkencod.spice_road.block.SpiceTrees;
import com.drunkencod.spice_road.block.SpiceVines;
import com.drunkencod.spice_road.spice.ProcessedSpice;
import com.drunkencod.spice_road.spice.Spice;

/**
 * Central item registry.
 * <p>
 * Add items here using {@link IRegistryHelper#registerItem(String, Supplier)},
 * then call {@link #addForBulkModel(ResourceLocation)} so the datagen helper
 * knows to generate a flat item model for them automatically.
 */
public class ModItems {

    // #region model genning

    /**
     * Resource locations of items that should receive an auto-generated flat item
     * model (i.e. {@code minecraft:item/generated} parent with a single
     * {@code layer0} texture).
     */
    public static final List<ResourceLocation> FLAT_ITEM_MODEL_IDS = new ArrayList<>();

    /**
     * Mark an item as needing a bulk-generated flat item model.
     * 
     * @param loc The full {@link ResourceLocation} of the item (e.g.
     *            {@code spice_road:my_item})
     */
    public static void addForBulkModel(ResourceLocation loc) {
        FLAT_ITEM_MODEL_IDS.add(loc);
    }

    /** @return Unmodifiable view of the items queued for flat model generation. */
    public static List<ResourceLocation> getFlatItemModelIds() {
        return Collections.unmodifiableList(FLAT_ITEM_MODEL_IDS);
    }

    // #region creative tabs

    /**
     * Fills the generic creative tab.
     *
     * @param output The tab's item output.
     */
    public static void populateGenericTab(CreativeModeTab.Output output) {
        output.accept(Items.ROTTEN_FLESH.getDefaultInstance());
    }

    /**
     * Fills the spices creative tab with every {@link Spice}'s seeds, tree
     * blocks, vine, raw item and Processed Spice items, in enum order.
     *
     * @param output The tab's item output.
     */
    public static void populateSpicesTab(CreativeModeTab.Output output) {
        for (Spice spice : Spice.values()) {
            Item spiceSeeds = Spice.getSeedsById(spice.getId());
            Item spiceRaw = Spice.getRawById(spice.getId());

            if (spiceSeeds != null)
                output.accept(spiceSeeds.getDefaultInstance());

            SpiceTree tree = SpiceTrees.getRegistered().get(spice);
            if (tree != null)
                tree.getBlockItems().forEach(item -> output.accept(item.getDefaultInstance()));

            SpiceVines.RegisteredSpiceVine vine = SpiceVines.getRegistered().get(spice);
            if (vine != null)
                output.accept(vine.vineItem().get().getDefaultInstance());

            if (spiceRaw != null)
                output.accept(spiceRaw.getDefaultInstance());
            for (ProcessedSpice processed : ProcessedSpice.bySource(spice)) {
                Item processedItem = processed.getItem();
                if (processedItem != null)
                    output.accept(processedItem.getDefaultInstance());
            }
        }
    }

    // #region Item byPath

    /** Returns the item with the given registry ID path */
    public static @Nullable Item byPath(String path) {
        ResourceLocation itemId = ResourceLocation.fromNamespaceAndPath(Constants.MOD_ID, path);
        return BuiltInRegistries.ITEM.getOptional(itemId).orElse(null);
    }
}
