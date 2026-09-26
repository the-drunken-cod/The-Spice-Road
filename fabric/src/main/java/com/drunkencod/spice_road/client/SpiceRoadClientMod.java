package com.drunkencod.spice_road.client;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.blockrenderlayer.v1.BlockRenderLayerMap;
import net.minecraft.client.renderer.RenderType;

import com.drunkencod.spice_road.block.SpicePlants;
import com.drunkencod.spice_road.block.SpiceTrees;

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
        SpicePlants.getRegistered().values()
                .forEach(plant -> BlockRenderLayerMap.INSTANCE.putBlock(plant.block().get(), RenderType.cutout()));
        SpiceTrees.getRegistered().values().forEach(tree -> {
            BlockRenderLayerMap.INSTANCE.putBlock(tree.getSapling().get(), RenderType.cutout());
            BlockRenderLayerMap.INSTANCE.putBlock(tree.getLeaves().get(), RenderType.cutoutMipped());
        });
    }
}
