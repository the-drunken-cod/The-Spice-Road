package com.drunkencod.spice_road.platform;

import com.drunkencod.spice_road.platform.services.IPlatformHelper;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.ModList;
import net.neoforged.fml.loading.FMLLoader;

public class NeoForgePlatformHelper implements IPlatformHelper {

    @Override
    public String getPlatformName() {

        return "NeoForge";
    }

    @Override
    public boolean isModLoaded(String modId) {

        return ModList.get().isLoaded(modId);
    }

    @Override
    public boolean isDevelopmentEnvironment() {

        return !FMLLoader.isProduction();
    }

    @Override
    public boolean isDedicatedServer() {

        // Checked against the physical distribution rather than a live MinecraftServer
        // instance, so this is safe to call at any time (e.g. before a world is loaded)
        // and needs no fallback for a "no server yet" case.
        return FMLLoader.getDist() == Dist.DEDICATED_SERVER;
    }
}
