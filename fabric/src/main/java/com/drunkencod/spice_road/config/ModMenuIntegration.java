package com.drunkencod.spice_road.config;

import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;

import me.shedaniel.autoconfig.AutoConfig;

/**
 * Exposes the config screen to ModMenu, which is the only way to open a Cloth
 * Config screen on Fabric - Cloth registers a screen for itself, not for
 * dependent mods.
 * <p>
 * ModMenu is a compile-only dependency and this entrypoint is only loaded when
 * it is actually installed, so the mod runs unchanged without it.
 */
public class ModMenuIntegration implements ModMenuApi {

    @Override
    public ConfigScreenFactory<?> getModConfigScreenFactory() {
        return parent -> AutoConfig.getConfigScreen(FabricConfigHelper.getConfigClass(), parent).get();
    }
}
