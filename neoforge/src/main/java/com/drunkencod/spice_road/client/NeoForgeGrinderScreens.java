package com.drunkencod.spice_road.client;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;

import com.drunkencod.spice_road.Constants;
import com.drunkencod.spice_road.client.grinder.SpiceGrinderScreen;
import com.drunkencod.spice_road.client.rack.SpiceRackScreen;
import com.drunkencod.spice_road.registry.ModMenus;

/**
 * Registers the screens of the Spice Grinder and Spice Rack GUIs. Fabric does the same in
 * {@code SpiceRoadClientMod}.
 */
@EventBusSubscriber(modid = Constants.MOD_ID, value = Dist.CLIENT)
public final class NeoForgeGrinderScreens {

    private NeoForgeGrinderScreens() {
    }

    @SubscribeEvent
    private static void onRegisterMenuScreens(RegisterMenuScreensEvent event) {
        event.register(ModMenus.SPICE_GRINDER.get(), SpiceGrinderScreen::new);
        event.register(ModMenus.SPICE_RACK.get(), SpiceRackScreen::new);
    }
}
