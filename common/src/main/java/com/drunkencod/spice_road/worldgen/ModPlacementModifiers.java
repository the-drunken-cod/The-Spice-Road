package com.drunkencod.spice_road.worldgen;

import java.util.function.Supplier;

import net.minecraft.world.level.levelgen.placement.PlacementModifierType;

import com.drunkencod.spice_road.platform.Services;

/**
 * Custom {@link PlacementModifierType}s, via {@link Services#REGISTRY}.
 */
public final class ModPlacementModifiers {

    /** {@code spice_road:region_heart} - see {@link RegionHeartPlacement}. */
    public static final Supplier<PlacementModifierType<RegionHeartPlacement>> REGION_HEART = Services.REGISTRY
            .registerPlacementModifierType("region_heart", () -> () -> RegionHeartPlacement.CODEC);

    /** {@code spice_road:near_region_heart} - see {@link RegionHeartProximityPlacement}. */
    public static final Supplier<PlacementModifierType<RegionHeartProximityPlacement>> NEAR_REGION_HEART =
            Services.REGISTRY.registerPlacementModifierType("near_region_heart",
                    () -> () -> RegionHeartProximityPlacement.CODEC);

    private ModPlacementModifiers() {
    }

    /**
     * No-op other than forcing this class (and therefore its suppliers'
     * static initializers) to load.
     */
    public static void register() {
    }
}
