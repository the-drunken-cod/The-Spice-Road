package com.drunkencod.spice_road;

import com.drunkencod.spice_road.block.SpicePlants;
import com.drunkencod.spice_road.block.SpiceTrees;
import com.drunkencod.spice_road.block.SpiceVines;
import com.drunkencod.spice_road.platform.Services;
import com.drunkencod.spice_road.registry.ModDataComponents;
import com.drunkencod.spice_road.registry.ModLootConditions;
import com.drunkencod.spice_road.registry.ModLootFunctions;
import com.drunkencod.spice_road.registry.ModMapDecorations;
import com.drunkencod.spice_road.spice.SpiceProfileReloadListener;
import com.drunkencod.spice_road.tooltip.SpiceProfileTooltips;
import com.drunkencod.spice_road.worldgen.ModFeatures;
import com.drunkencod.spice_road.worldgen.ModPlacementModifiers;
import com.drunkencod.spice_road.worldgen.ModTreeDecorators;

/**
 * Loader-independent mod initialization, called from each loader's entry
 * point.
 */
public class SpiceRoad {

    /**
     * Registers all blocks, items, and other registry entries, plus the
     * datapack reload listeners and tooltips. Called from each loader's mod
     * constructor/initializer.
     */
    public static void init() {
        ModDataComponents.register();
        Services.REGISTRY.registerReloadListener(SpiceProfileReloadListener.ID, new SpiceProfileReloadListener());
        SpiceProfileTooltips.register();
        SpicePlants.bootstrap();
        SpiceTrees.bootstrap();
        SpiceVines.bootstrap();
        ModFeatures.register();
        ModTreeDecorators.register();
        ModPlacementModifiers.register();
        ModLootConditions.register();
        ModLootFunctions.register();
        ModMapDecorations.register();
    }

    /**
     * Setup that needs every block/item to already be registered. Called once
     * registries are populated (after {@link #init()} on Fabric, during
     * {@code FMLCommonSetupEvent} on NeoForge).
     */
    public static void commonSetup() {
        SpiceTrees.registerFlammability();
        SpiceVines.registerFlammability();
    }
}
