package com.drunkencod.spice_road.registry;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;

import java.util.function.Supplier;

import org.jetbrains.annotations.Nullable;

import com.drunkencod.spice_road.Constants;
import com.drunkencod.spice_road.block.SpiceTree;
import com.drunkencod.spice_road.block.SpiceTrees;
import com.drunkencod.spice_road.block.SpiceVines;
import com.drunkencod.spice_road.datagen.ItemModelHelper;
import com.drunkencod.spice_road.grinder.SpiceGrinderItem;
import com.drunkencod.spice_road.mix.MixPresetRegistry;
import com.drunkencod.spice_road.mix.SpiceMixItem;
import com.drunkencod.spice_road.mix.SpiceMixes;
import com.drunkencod.spice_road.platform.Services;
import com.drunkencod.spice_road.spice.ProcessedSpice;
import com.drunkencod.spice_road.spice.Spice;

/**
 * Central item registry helpers: creative tab contents and item lookup.
 * <p>
 * Items are registered using
 * {@link IRegistryHelper#registerItem(String, Supplier)};
 * items that need a flat item model are queued via
 * {@link com.drunkencod.spice_road.datagen.ItemModelHelper#addFlatItem}.
 */
public class ModItems {

    /** The Spice Grinder, which opens the seasoning GUI. */
    public static Supplier<SpiceGrinderItem> SPICE_GRINDER;

    /** The empty jar, the base of every Spice Mix. */
    public static Supplier<Item> JAR;

    /** A jar filled with spices. */
    public static Supplier<SpiceMixItem> SPICE_MIX;

    /**
     * Registers the Spice Grinder, with a flat item model. Must be called during
     * mod initialization (see {@code SpiceRoad#init()}).
     */
    public static void registerGrinder() {
        SPICE_GRINDER = Services.REGISTRY.registerItem("spice_grinder", SpiceGrinderItem::new);
        ItemModelHelper.addFlatItem("spice_grinder", ResourceLocation.fromNamespaceAndPath(Constants.MOD_ID,
                "item/spice_grinder"));
    }

    /**
     * Registers the empty jar, with a flat item model, and the Spice Mix, whose
     * model (with one override per Mix Preset) is datagenned separately. Must be
     * called during mod initialization (see {@code SpiceRoad#init()}).
     */
    public static void registerSpiceMixes() {
        JAR = Services.REGISTRY.registerItem("jar", () -> new Item(new Item.Properties()));
        ItemModelHelper.addFlatItem("jar", ResourceLocation.fromNamespaceAndPath(Constants.MOD_ID, "item/jar"));
        SPICE_MIX = Services.REGISTRY.registerItem("spice_mix",
                () -> new SpiceMixItem(new Item.Properties().craftRemainder(JAR.get())));
    }

    // #region creative tabs

    /**
     * Fills the generic creative tab.
     *
     * @param output The tab's item output.
     */
    public static void populateGenericTab(CreativeModeTab.Output output) {
        output.accept(SPICE_GRINDER.get().getDefaultInstance());
        output.accept(JAR.get().getDefaultInstance());
        output.accept(ModBlocks.DRYING_RACK_ITEM.get().getDefaultInstance());
        ModBlocks.SPICE_RACK_ITEMS.values().forEach(rack -> output.accept(rack.get().getDefaultInstance()));
        MixPresetRegistry.getAll().forEach((id, preset) -> SpiceMixes.ofPreset(id, preset)
                .ifPresent(stack -> output.accept(stack)));
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

            if (spiceRaw != null)
                output.accept(spiceRaw.getDefaultInstance());

            for (ProcessedSpice processed : ProcessedSpice.bySource(spice)) {
                Item processedItem = processed.getItem();
                if (processedItem != null)
                    output.accept(processedItem.getDefaultInstance());
            }

            if (spiceSeeds != null)
                output.accept(spiceSeeds.getDefaultInstance());

            SpiceTree tree = SpiceTrees.getRegistered().get(spice);
            if (tree != null)
                tree.getBlockItems().forEach(item -> output.accept(item.getDefaultInstance()));

            SpiceVines.RegisteredSpiceVine vine = SpiceVines.getRegistered().get(spice);
            if (vine != null)
                output.accept(vine.vineItem().get().getDefaultInstance());
        }
    }

    // #region Item byPath

    /** Returns the item with the given registry ID path */
    public static @Nullable Item byPath(String path) {
        ResourceLocation itemId = ResourceLocation.fromNamespaceAndPath(Constants.MOD_ID, path);
        return BuiltInRegistries.ITEM.getOptional(itemId).orElse(null);
    }
}
