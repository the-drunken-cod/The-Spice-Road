package com.drunkencod.spice_road;

import com.drunkencod.spice_road.Constants;
import com.drunkencod.spice_road.SpiceRoad;
import com.drunkencod.spice_road.config.NeoForgeConfigHelper;
import com.drunkencod.spice_road.datagen.NeoForgeItemModelProvider;
import com.drunkencod.spice_road.platform.Services;
import com.drunkencod.spice_road.registry.NeoForgeRegistryHelper;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.data.event.GatherDataEvent;

@Mod(Constants.MOD_ID)
public class SpiceRoadMod {

    public SpiceRoadMod(IEventBus eventBus, ModContainer modContainer) {
        // Wire DeferredRegisters
        ((NeoForgeRegistryHelper) Services.REGISTRY).initialize(eventBus);

        // Register configs
        ((NeoForgeConfigHelper) Services.CONFIG).register(modContainer);

        eventBus.addListener(this::onGatherData);

        SpiceRoad.init();
    }

    private void onGatherData(GatherDataEvent event) {
        event.getGenerator().addProvider(
                event.includeClient(),
                new NeoForgeItemModelProvider(event.getGenerator().getPackOutput(), event.getExistingFileHelper()));
    }
}
