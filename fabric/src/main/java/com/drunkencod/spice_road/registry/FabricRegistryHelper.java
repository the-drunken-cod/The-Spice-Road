package com.drunkencod.spice_road.registry;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.core.Registry;

import java.util.function.Supplier;

import com.drunkencod.spice_road.Constants;

public class FabricRegistryHelper implements IRegistryHelper {

    @Override
    public <T extends Item> Supplier<T> registerItem(String id, Supplier<T> factory) {
        T item = Registry.register(BuiltInRegistries.ITEM,
                ResourceLocation.fromNamespaceAndPath(Constants.MOD_ID, id), factory.get());
        return () -> item;
    }

    @Override
    public <T extends Block> Supplier<T> registerBlock(String id, Supplier<T> factory) {
        T block = Registry.register(BuiltInRegistries.BLOCK,
                ResourceLocation.fromNamespaceAndPath(Constants.MOD_ID, id), factory.get());
        return () -> block;
    }

    @Override
    public <T extends Feature<?>> Supplier<T> registerFeature(String id, Supplier<T> factory) {
        T feature = Registry.register(BuiltInRegistries.FEATURE,
                ResourceLocation.fromNamespaceAndPath(Constants.MOD_ID, id), factory.get());
        return () -> feature;
    }
}
