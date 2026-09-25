package com.drunkencod.spice_road;

import com.drunkencod.spice_road.block.SpicePlants;
import com.drunkencod.spice_road.platform.Services;
import com.drunkencod.spice_road.registry.ModDataComponents;
import com.drunkencod.spice_road.spice.SpiceProfileReloadListener;
import com.drunkencod.spice_road.tooltip.SpiceProfileTooltips;
import com.drunkencod.spice_road.worldgen.ModFeatures;

public class SpiceRoad {

    public static void init() {
        ModDataComponents.register();
        Services.REGISTRY.registerReloadListener(SpiceProfileReloadListener.ID, new SpiceProfileReloadListener());
        SpiceProfileTooltips.register();
        SpicePlants.bootstrap();
        ModFeatures.register();
    }
}
