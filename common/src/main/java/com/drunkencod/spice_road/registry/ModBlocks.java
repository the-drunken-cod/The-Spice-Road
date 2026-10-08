package com.drunkencod.spice_road.registry;

import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;
import java.util.function.Supplier;

import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;

import com.drunkencod.spice_road.drying.DryingRackBlock;
import com.drunkencod.spice_road.grinder.SpiceGrinderBlock;
import com.drunkencod.spice_road.platform.Services;
import com.drunkencod.spice_road.rack.SpiceRackBlock;
import com.drunkencod.spice_road.rack.SpiceRackWood;

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

    /** The Spice Grinder as placed in the world. Its item is registered by {@link ModItems#registerGrinder()}. */
    public static final Supplier<SpiceGrinderBlock> SPICE_GRINDER = Services.REGISTRY.registerBlock("spice_grinder",
            () -> new SpiceGrinderBlock(SpiceGrinderBlock.createProperties()));

    /** The Spice Rack of every wood, in enum order. */
    public static final Map<SpiceRackWood, Supplier<SpiceRackBlock>> SPICE_RACKS;

    /** The item of every Spice Rack. Their models are datagenned. */
    public static final Map<SpiceRackWood, Supplier<Item>> SPICE_RACK_ITEMS;

    static {
        Map<SpiceRackWood, Supplier<SpiceRackBlock>> racks = new EnumMap<>(SpiceRackWood.class);
        Map<SpiceRackWood, Supplier<Item>> items = new EnumMap<>(SpiceRackWood.class);
        for (SpiceRackWood wood : SpiceRackWood.values()) {
            Supplier<SpiceRackBlock> rack = Services.REGISTRY.registerBlock(wood.getRackId(),
                    () -> new SpiceRackBlock(SpiceRackBlock.createProperties(wood)));
            racks.put(wood, rack);
            items.put(wood, Services.REGISTRY.registerItem(wood.getRackId(),
                    () -> new BlockItem(rack.get(), new Item.Properties())));
        }
        SPICE_RACKS = Collections.unmodifiableMap(racks);
        SPICE_RACK_ITEMS = Collections.unmodifiableMap(items);
    }

    private ModBlocks() {
    }

    /**
     * No-op other than forcing this class (and therefore its static
     * initializers) to load.
     */
    public static void register() {
    }
}
