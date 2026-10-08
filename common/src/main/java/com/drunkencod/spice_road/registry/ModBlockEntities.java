package com.drunkencod.spice_road.registry;

import java.util.function.Supplier;

import net.minecraft.world.level.block.entity.BlockEntityType;

import com.drunkencod.spice_road.drying.DryingRackBlockEntity;
import com.drunkencod.spice_road.platform.Services;

/**
 * Custom {@link BlockEntityType}s, via {@link Services#REGISTRY}. Register the
 * blocks they belong to first.
 */
public final class ModBlockEntities {

    /** The block entity of the Drying Rack. */
    public static final Supplier<BlockEntityType<DryingRackBlockEntity>> DRYING_RACK = Services.REGISTRY
            .registerBlockEntityType("drying_rack", DryingRackBlockEntity::new, ModBlocks.DRYING_RACK);

    private ModBlockEntities() {
    }

    /**
     * No-op other than forcing this class (and therefore its static
     * initializers) to load.
     */
    public static void register() {
    }
}
