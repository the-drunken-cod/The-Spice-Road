package com.drunkencod.spice_road.registry;

import com.drunkencod.spice_road.Constants;
import com.drunkencod.spice_road.spice.Spice;

import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * NeoForge implementation of {@link ICreativeTabHelper}, registering the tabs
 * through a {@link DeferredRegister} wired up by {@link #initialize}.
 */
public class NeoForgeCreativeTabHelper implements ICreativeTabHelper {

    private final DeferredRegister<CreativeModeTab> creativeTabs = DeferredRegister.create(
            Registries.CREATIVE_MODE_TAB,
            Constants.MOD_ID);

    public NeoForgeCreativeTabHelper() {
        creativeTabs.register(ICreativeTabHelper.TAB_GENERIC_KEY, () -> CreativeModeTab.builder()
                .title(Component.translatable(ICreativeTabHelper.TAB_GENERIC_TR_KEY))
                .icon(() -> ModItems.SPICE_GRINDER.get().getDefaultInstance())
                .displayItems((params, output) -> ModItems.populateGenericTab(output))
                .build());
        creativeTabs.register(ICreativeTabHelper.TAB_SPICES_KEY, () -> CreativeModeTab.builder()
                .title(Component.translatable(ICreativeTabHelper.TAB_SPICES_TR_KEY))
                .icon(() -> new ItemStack(ModItems.byId(Spice.HABANERO.getId())))
                .displayItems((params, output) -> ModItems.populateSpicesTab(output))
                .build());
    }

    @Override
    public void register() {
    }

    /**
     * Must be called in the NeoForge mod constructor with the mod event bus so
     * the creative tabs get registered.
     *
     * @param eventBus The mod event bus.
     */
    public void initialize(IEventBus eventBus) {
        creativeTabs.register(eventBus);
    }
}
