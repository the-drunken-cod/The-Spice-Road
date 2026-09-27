package com.drunkencod.spice_road.mixin.compat;

import net.fabricmc.loader.api.FabricLoader;

/** Fabric implementation of {@link CompatMixinPlugin}. */
public class FabricCompatMixinPlugin extends CompatMixinPlugin {

    @Override
    protected boolean isModLoaded(String modId) {
        return FabricLoader.getInstance().isModLoaded(modId);
    }
}
