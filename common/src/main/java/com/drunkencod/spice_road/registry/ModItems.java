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

import net.minecraft.world.item.ItemStack;

import com.drunkencod.spice_road.Constants;
import com.drunkencod.spice_road.spice.Spice;
import com.drunkencod.spice_road.spice.SpiceProfile;
import com.drunkencod.spice_road.spice.SpiceProfileRegistry;

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
                output.accept(withSpiceProfile(spiceRaw));
            if (spiceDried != null)
                output.accept(withSpiceProfile(spiceDried));
            if (spiceSeeds != null)
                output.accept(withSpiceProfile(spiceSeeds));
        }
    }

    /**
     * @param item An item, optionally registered as a spice.
     * @return A default stack of {@code item} carrying the
     *         {@code spice_road:spice_profile} data component if a
     *         datapack-registered default {@link SpiceProfile} exists for it,
     *         so the creative tab entry shows its flavor axis tooltip without
     *         relying on a live datapack reload having populated
     *         {@link SpiceProfileRegistry}.
     */
    private static ItemStack withSpiceProfile(Item item) {
        ItemStack stack = item.getDefaultInstance();
        SpiceProfileRegistry.getDefault(item)
                .ifPresent(profile -> stack.set(ModDataComponents.SPICE_PROFILE.get(), profile));
        return stack;
    }
}
