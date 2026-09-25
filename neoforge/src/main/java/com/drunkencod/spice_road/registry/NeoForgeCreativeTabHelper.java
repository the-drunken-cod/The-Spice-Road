package com.drunkencod.spice_road.registry;

import com.drunkencod.spice_road.Constants;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Items;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;

public class NeoForgeCreativeTabHelper implements ICreativeTabHelper {

        private final DeferredRegister<CreativeModeTab> creativeTabs = DeferredRegister.create(
                        Registries.CREATIVE_MODE_TAB,
                        Constants.MOD_ID);

        public NeoForgeCreativeTabHelper() {
                creativeTabs.register(ICreativeTabHelper.TAB_GENERIC_KEY, () -> CreativeModeTab.builder()
                                .title(Component.translatable(ICreativeTabHelper.TAB_GENERIC_TR_KEY))
                                .icon(() -> Items.ROTTEN_FLESH.getDefaultInstance())
                                .displayItems((params, output) -> ModItems.populateGenericTab(output))
                                .build());
                creativeTabs.register(ICreativeTabHelper.TAB_SPICES_KEY, () -> CreativeModeTab.builder()
                                .title(Component.translatable(ICreativeTabHelper.TAB_SPICES_TR_KEY))
                                .icon(() -> Items.LEATHER.getDefaultInstance())
                                .displayItems((params, output) -> ModItems.populateSpicesTab(output))
                                .build());
        }

        @Override
        public void register() {
        }

        public void initialize(IEventBus eventBus) {
                creativeTabs.register(eventBus);
        }
}
