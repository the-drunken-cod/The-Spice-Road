package com.drunkencod.spice_road.registry;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.function.Supplier;

import com.drunkencod.spice_road.Constants;

public class NeoForgeRegistryHelper implements IRegistryHelper {

    private final DeferredRegister<Item> items = DeferredRegister.create(BuiltInRegistries.ITEM, Constants.MOD_ID);

    private final DeferredRegister<Block> blocks = DeferredRegister.create(BuiltInRegistries.BLOCK, Constants.MOD_ID);

    private final DeferredRegister<Feature<?>> features = DeferredRegister.create(BuiltInRegistries.FEATURE,
            Constants.MOD_ID);

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

    /**
     * Must be called in the NeoForge mod constructor with the mod event bus so that
     * DeferredRegisters can fire their registration events.
     */
    public void initialize(IEventBus eventBus) {
        items.register(eventBus);
        blocks.register(eventBus);
        features.register(eventBus);
    }
}
