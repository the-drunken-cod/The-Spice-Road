package com.drunkencod.spice_road;

import com.drunkencod.spice_road.block.SpicePlants;
import com.drunkencod.spice_road.block.SpiceTrees;
import com.drunkencod.spice_road.platform.Services;
import com.drunkencod.spice_road.registry.ModDataComponents;
import com.drunkencod.spice_road.registry.ModLootConditions;
import com.drunkencod.spice_road.spice.SpiceProfileReloadListener;
import com.drunkencod.spice_road.tooltip.SpiceProfileTooltips;
import com.drunkencod.spice_road.worldgen.ModFeatures;

public class SpiceRoad {

    public static void init() {
        ModDataComponents.register();
        Services.REGISTRY.registerReloadListener(SpiceProfileReloadListener.ID, new SpiceProfileReloadListener());
        SpiceProfileTooltips.register();
        SpicePlants.bootstrap();
        SpiceTrees.bootstrap();
        ModFeatures.register();
        ModLootConditions.register();
    }

    /**
     * Setup that needs every block/item to already be registered. Called once
     * registries are populated (after {@link #init()} on Fabric, during
     * {@code FMLCommonSetupEvent} on NeoForge).
     */
    public static void commonSetup() {
        SpiceTrees.registerFlammability();
    }
}
