package com.drunkencod.spice_road;

import com.drunkencod.spice_road.block.SpicePlants;
import com.drunkencod.spice_road.worldgen.ModFeatures;

public class SpiceRoad {

    public static void init() {
        SpicePlants.bootstrap();
        ModFeatures.register();
    }
}
