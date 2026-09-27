package com.drunkencod.spice_road.mixin.compat;

import net.neoforged.fml.loading.LoadingModList;

/** NeoForge implementation of {@link CompatMixinPlugin}. */
public class NeoForgeCompatMixinPlugin extends CompatMixinPlugin {

    @Override
    protected boolean isModLoaded(String modId) {
        return LoadingModList.get().getModFileById(modId) != null;
    }
}
