package com.drunkencod.spice_road.registry;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.PreparableReloadListener;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.AddReloadListenerEvent;
import net.neoforged.neoforge.registries.DeferredRegister;

import com.drunkencod.spice_road.Constants;

public class NeoForgeRegistryHelper implements IRegistryHelper {

    private final DeferredRegister<Item> items = DeferredRegister.create(BuiltInRegistries.ITEM, Constants.MOD_ID);

    private final DeferredRegister<Block> blocks = DeferredRegister.create(BuiltInRegistries.BLOCK, Constants.MOD_ID);

    private final DeferredRegister<Feature<?>> features = DeferredRegister.create(BuiltInRegistries.FEATURE,
            Constants.MOD_ID);

    private final DeferredRegister<DataComponentType<?>> dataComponents = DeferredRegister
            .create(BuiltInRegistries.DATA_COMPONENT_TYPE, Constants.MOD_ID);

    private final List<PreparableReloadListener> pendingReloadListeners = new ArrayList<>();

    @Override
    public <T extends Item> Supplier<T> registerItem(String id, Supplier<T> factory) {
        return (Supplier<T>) items.register(id, factory);
    }

    @Override
    public <T extends Block> Supplier<T> registerBlock(String id, Supplier<T> factory) {
        return (Supplier<T>) blocks.register(id, factory);
    }

    @Override
    public <T extends Feature<?>> Supplier<T> registerFeature(String id, Supplier<T> factory) {
        return (Supplier<T>) features.register(id, factory);
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> Supplier<DataComponentType<T>> registerDataComponentType(String id,
            Supplier<DataComponentType<T>> factory) {
        return (Supplier<DataComponentType<T>>) (Supplier<?>) dataComponents.register(id, factory::get);
    }

    @Override
    public void registerReloadListener(ResourceLocation id, PreparableReloadListener listener) {
        pendingReloadListeners.add(listener);
    }

    private void onAddReloadListeners(AddReloadListenerEvent event) {
        for (PreparableReloadListener listener : pendingReloadListeners) {
            event.addListener(listener);
        }
    }

    /**
     * Must be called in the NeoForge mod constructor with the mod event bus so that
     * DeferredRegisters can fire their registration events.
     */
    public void initialize(IEventBus eventBus) {
        items.register(eventBus);
        blocks.register(eventBus);
        features.register(eventBus);
        dataComponents.register(eventBus);
        // AddReloadListenerEvent is a game event, not a mod-bus event.
        NeoForge.EVENT_BUS.addListener(this::onAddReloadListeners);
    }
}
