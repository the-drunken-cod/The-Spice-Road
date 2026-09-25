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

import com.drunkencod.spice_road.Constants;
import com.drunkencod.spice_road.spice.Spice;

/**
 * Central item registry.
 * <p>
 * Add items here using {@link IRegistryHelper#registerItem(String, Supplier)},
 * then call {@link #addForBulkModel(ResourceLocation)} so the datagen helper
 * knows to generate a flat item model for them automatically.
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
     *            {@code spice_road:my_item})
     */
    public static void addForBulkModel(ResourceLocation loc) {
        FLAT_ITEM_MODEL_IDS.add(loc);
    }

    public static List<ResourceLocation> getFlatItemModelIds() {
        return Collections.unmodifiableList(FLAT_ITEM_MODEL_IDS);
    }

    /** Returns the item with the given registry ID path */
    public static @Nullable Item byPath(String path) {
        ResourceLocation itemId = ResourceLocation.fromNamespaceAndPath(Constants.MOD_ID, path);
        return BuiltInRegistries.ITEM.getOptional(itemId).orElse(null);
    }

    // #region creative tabs

    public static void populateGenericTab(CreativeModeTab.Output output) {
        // output.accept();
    }

    public static void populateSpicesTab(CreativeModeTab.Output output) {
        for (Spice spice : Spice.values()) {
            Item spiceRaw = Spice.getRawById(spice.getId());
            Item spiceDried = Spice.getDriedById(spice.getId());
            Item spiceSeeds = Spice.getSeedsById(spice.getId());

            if (spiceRaw != null)
                output.accept(spiceRaw.getDefaultInstance());
            if (spiceDried != null)
                output.accept(spiceDried.getDefaultInstance());
            if (spiceSeeds != null)
                output.accept(spiceSeeds.getDefaultInstance());
        }
    }
}
