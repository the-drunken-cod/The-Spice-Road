package com.drunkencod.spice_road;

import com.drunkencod.spice_road.SpiceRoad;
import com.drunkencod.spice_road.config.FabricConfigHelper;
import com.drunkencod.spice_road.platform.Services;

import net.fabricmc.api.ModInitializer;

public class SpiceRoadMod implements ModInitializer {

    @Override
    public void onInitialize() {
        // Register Cloth Config configs
        ((FabricConfigHelper) Services.CONFIG).register();

        SpiceRoad.init();
    }
}
