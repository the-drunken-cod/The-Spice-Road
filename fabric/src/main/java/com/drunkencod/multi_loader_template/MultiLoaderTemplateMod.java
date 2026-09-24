package com.drunkencod.multi_loader_template;

import com.drunkencod.multi_loader_template.MultiLoaderTemplate;
import com.drunkencod.multi_loader_template.config.FabricConfigHelper;
import com.drunkencod.multi_loader_template.platform.Services;

import net.fabricmc.api.ModInitializer;

public class MultiLoaderTemplateMod implements ModInitializer {

    @Override
    public void onInitialize() {
        // Register Cloth Config configs
        ((FabricConfigHelper) Services.CONFIG).register();

        MultiLoaderTemplate.init();
    }
}
