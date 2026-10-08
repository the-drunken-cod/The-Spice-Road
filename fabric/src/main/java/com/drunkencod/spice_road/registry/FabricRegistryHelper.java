package com.drunkencod.spice_road.registry;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.function.BiFunction;
import java.util.function.Supplier;

import net.fabricmc.fabric.api.resource.IdentifiableResourceReloadListener;
import net.fabricmc.fabric.api.resource.ResourceManagerHelper;
import net.minecraft.advancements.CriterionTrigger;
import net.minecraft.core.BlockPos;
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
import net.minecraft.sounds.SoundEvent;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
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
    public Holder<MobEffect> registerMobEffect(String id, Supplier<? extends MobEffect> factory) {
        return Registry.registerForHolder(BuiltInRegistries.MOB_EFFECT,
                ResourceKey.create(Registries.MOB_EFFECT, ResourceLocation.fromNamespaceAndPath(Constants.MOD_ID, id)),
                factory.get());
    }

    @Override
    public <T extends AbstractContainerMenu> Supplier<MenuType<T>> registerMenuType(String id,
            MenuType.MenuSupplier<T> factory) {
        MenuType<T> type = Registry.register(BuiltInRegistries.MENU,
                ResourceLocation.fromNamespaceAndPath(Constants.MOD_ID, id),
                new MenuType<>(factory, FeatureFlags.DEFAULT_FLAGS));
        return () -> type;
    }

    @Override
    public <T extends Recipe<?>> Supplier<RecipeSerializer<T>> registerRecipeSerializer(String id,
            Supplier<RecipeSerializer<T>> factory) {
        RecipeSerializer<T> serializer = Registry.register(BuiltInRegistries.RECIPE_SERIALIZER,
                ResourceLocation.fromNamespaceAndPath(Constants.MOD_ID, id), factory.get());
        return () -> serializer;
    }

    @Override
    public <T extends Recipe<?>> Supplier<RecipeType<T>> registerRecipeType(String id) {
        ResourceLocation location = ResourceLocation.fromNamespaceAndPath(Constants.MOD_ID, id);
        RecipeType<T> type = Registry.register(BuiltInRegistries.RECIPE_TYPE, location,
                IRegistryHelper.newRecipeType(location));
        return () -> type;
    }

    @Override
    public <T extends BlockEntity> Supplier<BlockEntityType<T>> registerBlockEntityType(String id,
            BiFunction<BlockPos, BlockState, T> factory, Supplier<? extends Block> block) {
        BlockEntityType<T> type = Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE,
                ResourceLocation.fromNamespaceAndPath(Constants.MOD_ID, id),
                BlockEntityType.Builder.<T>of(factory::apply, block.get()).build(null));
        return () -> type;
    }

    @Override
    public Supplier<SoundEvent> registerSoundEvent(String id) {
        ResourceLocation location = ResourceLocation.fromNamespaceAndPath(Constants.MOD_ID, id);
        SoundEvent event = Registry.register(BuiltInRegistries.SOUND_EVENT, location,
                SoundEvent.createVariableRangeEvent(location));
        return () -> event;
    }

    @Override
    public <T extends CriterionTrigger<?>> Supplier<T> registerCriterionTrigger(String id, Supplier<T> factory) {
        T trigger = Registry.register(BuiltInRegistries.TRIGGER_TYPES,
                ResourceLocation.fromNamespaceAndPath(Constants.MOD_ID, id), factory.get());
        return () -> trigger;
    }

    @Override
    public ResourceLocation registerCustomStat(String id) {
        ResourceLocation location = ResourceLocation.fromNamespaceAndPath(Constants.MOD_ID, id);
        return Registry.register(BuiltInRegistries.CUSTOM_STAT, location, location);
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
