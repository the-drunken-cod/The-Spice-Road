package com.drunkencod.multi_loader_template;

import com.drunkencod.multi_loader_template.Constants;
import com.drunkencod.multi_loader_template.MultiLoaderTemplate;
import com.drunkencod.multi_loader_template.config.NeoForgeConfigHelper;
import com.drunkencod.multi_loader_template.datagen.NeoForgeItemModelProvider;
import com.drunkencod.multi_loader_template.platform.Services;
import com.drunkencod.multi_loader_template.registry.NeoForgeRegistryHelper;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.data.event.GatherDataEvent;

@Mod(Constants.MOD_ID)
public class MultiLoaderTemplateMod {

    public MultiLoaderTemplateMod(IEventBus eventBus, ModContainer modContainer) {
        // Wire DeferredRegisters
        ((NeoForgeRegistryHelper) Services.REGISTRY).initialize(eventBus);

        // Register configs
        ((NeoForgeConfigHelper) Services.CONFIG).register(modContainer);

        eventBus.addListener(this::onGatherData);

        MultiLoaderTemplate.init();
    }

    private void onGatherData(GatherDataEvent event) {
        event.getGenerator().addProvider(
                event.includeClient(),
                new NeoForgeItemModelProvider(event.getGenerator().getPackOutput(), event.getExistingFileHelper()));
    }
}
