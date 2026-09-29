package com.drunkencod.spice_road.registry;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.function.Supplier;

import net.fabricmc.fabric.api.resource.IdentifiableResourceReloadListener;
import net.fabricmc.fabric.api.resource.ResourceManagerHelper;
import net.minecraft.advancements.CriterionTrigger;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.resources.PreparableReloadListener;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.treedecorators.TreeDecorator;
import net.minecraft.world.level.levelgen.feature.treedecorators.TreeDecoratorType;
import net.minecraft.world.level.levelgen.placement.PlacementModifier;
import net.minecraft.world.level.levelgen.placement.PlacementModifierType;
import net.minecraft.world.level.saveddata.maps.MapDecorationType;
import net.minecraft.world.level.storage.loot.functions.LootItemFunction;
import net.minecraft.world.level.storage.loot.functions.LootItemFunctionType;
import net.minecraft.world.level.storage.loot.predicates.LootItemConditionType;

import com.drunkencod.spice_road.Constants;

/**
 * Fabric implementation of {@link IRegistryHelper}. Registers everything
 * eagerly, so the returned suppliers are usable immediately.
 */
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

    @Override
    public <T> Supplier<DataComponentType<T>> registerDataComponentType(String id,
            Supplier<DataComponentType<T>> factory) {
        DataComponentType<T> type = Registry.register(BuiltInRegistries.DATA_COMPONENT_TYPE,
                ResourceLocation.fromNamespaceAndPath(Constants.MOD_ID, id), factory.get());
        return () -> type;
    }

    @Override
    public Supplier<LootItemConditionType> registerLootConditionType(String id,
            Supplier<LootItemConditionType> factory) {
        LootItemConditionType type = Registry.register(BuiltInRegistries.LOOT_CONDITION_TYPE,
                ResourceLocation.fromNamespaceAndPath(Constants.MOD_ID, id), factory.get());
        return () -> type;
    }

    @Override
    public <T extends LootItemFunction> Supplier<LootItemFunctionType<T>> registerLootFunctionType(String id,
            Supplier<LootItemFunctionType<T>> factory) {
        LootItemFunctionType<T> type = Registry.register(BuiltInRegistries.LOOT_FUNCTION_TYPE,
                ResourceLocation.fromNamespaceAndPath(Constants.MOD_ID, id), factory.get());
        return () -> type;
    }

    @Override
    public <T extends PlacementModifier> Supplier<PlacementModifierType<T>> registerPlacementModifierType(String id,
            Supplier<PlacementModifierType<T>> factory) {
        PlacementModifierType<T> type = Registry.register(BuiltInRegistries.PLACEMENT_MODIFIER_TYPE,
                ResourceLocation.fromNamespaceAndPath(Constants.MOD_ID, id), factory.get());
        return () -> type;
    }

    @Override
    public <T extends TreeDecorator> Supplier<TreeDecoratorType<T>> registerTreeDecoratorType(String id,
            Supplier<TreeDecoratorType<T>> factory) {
        TreeDecoratorType<T> type = Registry.register(BuiltInRegistries.TREE_DECORATOR_TYPE,
                ResourceLocation.fromNamespaceAndPath(Constants.MOD_ID, id), factory.get());
        return () -> type;
    }

    @Override
    public Holder<MapDecorationType> registerMapDecorationType(String id, Supplier<MapDecorationType> factory) {
        return Registry.registerForHolder(BuiltInRegistries.MAP_DECORATION_TYPE,
                ResourceKey.create(Registries.MAP_DECORATION_TYPE,
                        ResourceLocation.fromNamespaceAndPath(Constants.MOD_ID, id)),
                factory.get());
    }

    @Override
    public <T extends CriterionTrigger<?>> Supplier<T> registerCriterionTrigger(String id, Supplier<T> factory) {
        T trigger = Registry.register(BuiltInRegistries.TRIGGER_TYPES,
                ResourceLocation.fromNamespaceAndPath(Constants.MOD_ID, id), factory.get());
        return () -> trigger;
    }

    @Override
    public void registerReloadListener(ResourceLocation id, PreparableReloadListener listener) {
        ResourceManagerHelper.get(PackType.SERVER_DATA)
                .registerReloadListener(new IdentifiableResourceReloadListener() {
                    @Override
                    public ResourceLocation getFabricId() {
                        return id;
                    }

                    @Override
                    public CompletableFuture<Void> reload(PreparationBarrier barrier, ResourceManager manager,
                            ProfilerFiller prepareProfiler, ProfilerFiller applyProfiler,
                            Executor backgroundExecutor, Executor gameExecutor) {
                        return listener.reload(barrier, manager, prepareProfiler, applyProfiler, backgroundExecutor,
                                gameExecutor);
                    }
                });
    }
}
