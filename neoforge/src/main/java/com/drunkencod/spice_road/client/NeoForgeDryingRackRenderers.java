package com.drunkencod.spice_road.client;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;

import com.drunkencod.spice_road.Constants;
import com.drunkencod.spice_road.client.drying.DryingRackRenderer;
import com.drunkencod.spice_road.registry.ModBlockEntities;

/**
 * Registers the renderer drawing the items on a Drying Rack. Fabric does the
 * same in {@code SpiceRoadClientMod}.
 */
@EventBusSubscriber(modid = Constants.MOD_ID, value = Dist.CLIENT)
public final class NeoForgeDryingRackRenderers {

    private NeoForgeDryingRackRenderers() {
    }

    @SubscribeEvent
    private static void onRegisterRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerBlockEntityRenderer(ModBlockEntities.DRYING_RACK.get(), DryingRackRenderer::new);
    }
}
