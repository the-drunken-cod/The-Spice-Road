package com.drunkencod.spice_road.registry;

import java.util.function.Supplier;

import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;

import com.drunkencod.spice_road.drying.DryingRackBlock;
import com.drunkencod.spice_road.platform.Services;

/**
 * Central registry of the mod's standalone blocks. Spice Plants, Trees and
 * Vines register their own blocks.
 * Register blocks here via
 * {@link IRegistryHelper#registerBlock(String, Supplier)}.
 */
public final class ModBlocks {

    /** The Drying Rack, which dries items into Processed Spices and more. */
    public static final Supplier<DryingRackBlock> DRYING_RACK = Services.REGISTRY.registerBlock("drying_rack",
            () -> new DryingRackBlock(DryingRackBlock.createProperties()));

    /** The Drying Rack's item. Its model is hand-written, not generated. */
    public static final Supplier<Item> DRYING_RACK_ITEM = Services.REGISTRY.registerItem("drying_rack",
            () -> new BlockItem(DRYING_RACK.get(), new Item.Properties()));

    private ModBlocks() {
    }

    /**
     * No-op other than forcing this class (and therefore its static
     * initializers) to load.
     */
    public static void register() {
    }
}
