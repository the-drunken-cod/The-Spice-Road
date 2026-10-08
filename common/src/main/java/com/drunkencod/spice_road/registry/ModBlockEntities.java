package com.drunkencod.spice_road.registry;

import java.util.function.Supplier;

import net.minecraft.world.level.block.entity.BlockEntityType;

import com.drunkencod.spice_road.drying.DryingRackBlockEntity;
import com.drunkencod.spice_road.platform.Services;
import com.drunkencod.spice_road.rack.SpiceRackBlockEntity;

/**
 * Custom {@link BlockEntityType}s, via {@link Services#REGISTRY}. Register the
 * blocks they belong to first.
 */
public final class ModBlockEntities {

    /** The block entity of the Drying Rack. */
    public static final Supplier<BlockEntityType<DryingRackBlockEntity>> DRYING_RACK = Services.REGISTRY
            .registerBlockEntityType("drying_rack", DryingRackBlockEntity::new, ModBlocks.DRYING_RACK);

    /** The block entity of every Spice Rack. */
    public static final Supplier<BlockEntityType<SpiceRackBlockEntity>> SPICE_RACK = Services.REGISTRY
            .registerBlockEntityType("spice_rack", SpiceRackBlockEntity::new, ModBlocks.SPICE_RACKS.values());

    private ModBlockEntities() {
    }

    /**
     * No-op other than forcing this class (and therefore its static
     * initializers) to load.
     */
    public static void register() {
    }
}
