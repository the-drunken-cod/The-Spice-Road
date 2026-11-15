package com.drunkencod.spice_road.platform;

import com.drunkencod.spice_road.platform.services.IPlatformHelper;

import net.fabricmc.api.EnvType;
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;

/**
 * Fabric implementation of {@link IPlatformHelper}.
 */
public class FabricPlatformHelper implements IPlatformHelper {

    @Override
    public String getPlatformName() {
        return "Fabric";
    }

    @Override
    public boolean isModLoaded(String modId) {
        return FabricLoader.getInstance().isModLoaded(modId);
    }

    @Override
    public boolean isDevelopmentEnvironment() {
        return FabricLoader.getInstance().isDevelopmentEnvironment();
    }

    @Override
    public boolean isDedicatedServer() {
        // Checked against the physical environment type rather than a live
        // MinecraftServer instance, so this is safe to call at any time (e.g. before a
        // world is loaded) and needs no fallback for a "no server yet" case.
        return FabricLoader.getInstance().getEnvironmentType() == EnvType.SERVER;
    }

    @Override
    public boolean canBreakBlock(ServerPlayer player, BlockPos pos) {
        return PlayerBlockBreakEvents.BEFORE.invoker().beforeBlockBreak(player.level(), player, pos,
                player.level().getBlockState(pos), player.level().getBlockEntity(pos));
    }
}
