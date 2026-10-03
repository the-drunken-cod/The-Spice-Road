package com.drunkencod.spice_road.client;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.blockrenderlayer.v1.BlockRenderLayerMap;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.renderer.RenderType;

import com.drunkencod.spice_road.block.SpicePlants;
import com.drunkencod.spice_road.block.SpiceTrees;
import com.drunkencod.spice_road.block.SpiceVines;
import com.drunkencod.spice_road.config.ConfigSync;
import com.drunkencod.spice_road.config.ConfigSyncOverride;
import com.drunkencod.spice_road.spice.SpiceProfileSync;
import com.drunkencod.spice_road.spice.effect.SeasoningEffectSync;

/**
 * Fabric client-only entry point. Keep this limited to client-only setup that
 * must not run on a dedicated server.
 */
public class SpiceRoadClientMod implements ClientModInitializer {

    @Override
    public void onInitializeClient() {
        // Debug-only F3 overlay; registerIfDevelopment() itself gates on
        // Services.PLATFORM.isDevelopmentEnvironment(), so this is a no-op in
        // production.
        FabricSpiceRegionDebugOverlay.registerIfDevelopment();
        FabricSpiceTooltipHandler.register();
        ClientPlayNetworking.registerGlobalReceiver(SpiceProfileSync.TYPE, (payload, context) -> payload.handle());
        ClientPlayNetworking.registerGlobalReceiver(SeasoningEffectSync.TYPE, (payload, context) -> payload.handle());
        ClientPlayNetworking.registerGlobalReceiver(ConfigSync.TYPE, (payload, context) -> payload.handle());
        // The override must not outlive the connection that sent it - a later
        // singleplayer world or a different server needs its own values.
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> ConfigSyncOverride.clear());

        registerSpicePlantRenderLayers();
    }

    /**
     * Fabric has no per-model render type like NeoForge's model-JSON
     * {@code "render_type"} key (see {@code NeoForgeBlockStateProvider}); it's
     * registered per-block here instead. Without this, Spice Plant blocks fall
     * back to {@code RenderType.solid()} and render transparent texture pixels
     * as solid/black.
     */
    private static void registerSpicePlantRenderLayers() {
        SpicePlants.getAllBlocks().forEach(block -> BlockRenderLayerMap.INSTANCE.putBlock(block, RenderType.cutout()));
        SpiceTrees.getRegistered().values().forEach(tree -> {
            BlockRenderLayerMap.INSTANCE.putBlock(tree.getSapling().get(), RenderType.cutout());
            BlockRenderLayerMap.INSTANCE.putBlock(tree.getLeaves().get(), RenderType.cutoutMipped());
        });
        SpiceVines.getRegistered().values()
                .forEach(vine -> BlockRenderLayerMap.INSTANCE.putBlock(vine.block().get(), RenderType.cutout()));
    }
}
