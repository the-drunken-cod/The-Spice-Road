package com.drunkencod.multi_loader_template.registry;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.function.Supplier;

import com.drunkencod.multi_loader_template.Constants;

public class NeoForgeRegistryHelper implements IRegistryHelper {

    private final DeferredRegister<Item> items = DeferredRegister.create(BuiltInRegistries.ITEM, Constants.MOD_ID);

    private final DeferredRegister<Block> blocks = DeferredRegister.create(BuiltInRegistries.BLOCK, Constants.MOD_ID);

    @Override
    public <T extends Item> Supplier<T> registerItem(String id, Supplier<T> factory) {
        return (Supplier<T>) items.register(id, factory);
    }

    @Override
    public <T extends Block> Supplier<T> registerBlock(String id, Supplier<T> factory) {
        return (Supplier<T>) blocks.register(id, factory);
    }

    /**
     * Must be called in the NeoForge mod constructor with the mod event bus so that
     * DeferredRegisters can fire their registration events.
     */
    public void initialize(IEventBus eventBus) {
        items.register(eventBus);
        blocks.register(eventBus);
    }
}
