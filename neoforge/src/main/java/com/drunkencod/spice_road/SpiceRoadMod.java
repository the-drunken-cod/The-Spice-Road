package com.drunkencod.spice_road;

import com.drunkencod.spice_road.client.NeoForgeSpiceRegionDebugOverlay;
import com.drunkencod.spice_road.client.NeoForgeSpiceTooltipHandler;
import com.drunkencod.spice_road.config.NeoForgeConfigHelper;
import com.drunkencod.spice_road.datagen.NeoForgeBlockStateProvider;
import com.drunkencod.spice_road.datagen.NeoForgeItemModelProvider;
import com.drunkencod.spice_road.datagen.NeoForgeSpiceDataMapProvider;
import com.drunkencod.spice_road.datagen.NeoForgeSpiceLootProvider;
import com.drunkencod.spice_road.datagen.SpiceTreeCompatRecipeProvider;
import com.drunkencod.spice_road.platform.Services;
import com.drunkencod.spice_road.registry.NeoForgeCreativeTabHelper;
import com.drunkencod.spice_road.registry.NeoForgeRegistryHelper;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.neoforge.data.event.GatherDataEvent;

/**
 * NeoForge mod entry point.
 */
@Mod(Constants.MOD_ID)
public class SpiceRoadMod {

    /**
     * Wires up registries, configs, and lifecycle/datagen listeners, then runs
     * the shared {@link SpiceRoad#init()}.
     *
     * @param eventBus     The mod event bus.
     * @param modContainer This mod's container, used to register configs.
     */
    public SpiceRoadMod(IEventBus eventBus, ModContainer modContainer) {
        // Wire DeferredRegisters
        ((NeoForgeRegistryHelper) Services.REGISTRY).initialize(eventBus);
        ((NeoForgeCreativeTabHelper) Services.CREATIVE_TAB).initialize(eventBus);

        // Register configs
        ((NeoForgeConfigHelper) Services.CONFIG).register(modContainer);

        eventBus.addListener(this::onGatherData);
        eventBus.addListener(this::onCommonSetup);
        eventBus.addListener(this::onClientSetup);

        SpiceRoad.init();
    }

    private void onCommonSetup(FMLCommonSetupEvent event) {
        event.enqueueWork(SpiceRoad::commonSetup);
    }

    private void onGatherData(GatherDataEvent event) {
        event.getGenerator().addProvider(
                event.includeClient(),
                new NeoForgeItemModelProvider(event.getGenerator().getPackOutput(), event.getExistingFileHelper()));
        event.getGenerator().addProvider(
                event.includeClient(),
                new NeoForgeBlockStateProvider(event.getGenerator().getPackOutput(), event.getExistingFileHelper()));
        event.getGenerator().addProvider(
                event.includeServer(),
                new NeoForgeSpiceLootProvider(event.getGenerator().getPackOutput(), event.getLookupProvider()));
        event.getGenerator().addProvider(
                event.includeServer(),
                new NeoForgeSpiceDataMapProvider(event.getGenerator().getPackOutput(), event.getLookupProvider()));
        event.getGenerator().addProvider(
                event.includeServer(),
                new SpiceTreeCompatRecipeProvider(event.getGenerator().getPackOutput()));
    }

    private void onClientSetup(FMLClientSetupEvent event) {
        // Debug-only F3 overlay; registerIfDevelopment() itself gates on
        // Services.PLATFORM.isDevelopmentEnvironment(), so this is a no-op in
        // production.
        NeoForgeSpiceRegionDebugOverlay.registerIfDevelopment();
        NeoForgeSpiceTooltipHandler.register();
    }
}
