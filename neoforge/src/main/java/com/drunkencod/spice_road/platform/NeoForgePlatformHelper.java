package com.drunkencod.spice_road.platform;

import com.drunkencod.spice_road.platform.services.IPlatformHelper;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.ModList;
import net.neoforged.fml.loading.FMLLoader;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.level.BlockEvent;

/**
 * NeoForge implementation of {@link IPlatformHelper}.
 */
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

    @Override
    public boolean canBreakBlock(ServerPlayer player, BlockPos pos) {
        BlockEvent.BreakEvent event = new BlockEvent.BreakEvent(player.level(), pos,
                player.level().getBlockState(pos), player);
        return !NeoForge.EVENT_BUS.post(event).isCanceled();
    }
}
