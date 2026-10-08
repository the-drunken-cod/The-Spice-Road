package com.drunkencod.spice_road.registry;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.function.BiFunction;
import java.util.function.Supplier;

import net.minecraft.advancements.CriterionTrigger;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.PreparableReloadListener;
import net.minecraft.sounds.SoundEvent;
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
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.AddReloadListenerEvent;
import net.neoforged.neoforge.registries.DeferredRegister;

import com.drunkencod.spice_road.Constants;

/**
 * NeoForge implementation of {@link IRegistryHelper}. Registers everything
 * through {@link DeferredRegister}s wired up by {@link #initialize}, so the
 * returned suppliers are only usable once registration has run.
 */
public class NeoForgeRegistryHelper implements IRegistryHelper {

    private final DeferredRegister<Item> items = DeferredRegister.create(BuiltInRegistries.ITEM, Constants.MOD_ID);

    private final DeferredRegister<Block> blocks = DeferredRegister.create(BuiltInRegistries.BLOCK, Constants.MOD_ID);

    private final DeferredRegister<Feature<?>> features = DeferredRegister.create(BuiltInRegistries.FEATURE,
            Constants.MOD_ID);

    private final DeferredRegister<DataComponentType<?>> dataComponents = DeferredRegister
            .create(BuiltInRegistries.DATA_COMPONENT_TYPE, Constants.MOD_ID);

    private final DeferredRegister<LootItemConditionType> lootConditionTypes = DeferredRegister
            .create(BuiltInRegistries.LOOT_CONDITION_TYPE, Constants.MOD_ID);

    private final DeferredRegister<LootItemFunctionType<?>> lootFunctionTypes = DeferredRegister
            .create(BuiltInRegistries.LOOT_FUNCTION_TYPE, Constants.MOD_ID);

    private final DeferredRegister<PlacementModifierType<?>> placementModifierTypes = DeferredRegister
            .create(BuiltInRegistries.PLACEMENT_MODIFIER_TYPE, Constants.MOD_ID);

    private final DeferredRegister<TreeDecoratorType<?>> treeDecoratorTypes = DeferredRegister
            .create(BuiltInRegistries.TREE_DECORATOR_TYPE, Constants.MOD_ID);

    private final DeferredRegister<MapDecorationType> mapDecorationTypes = DeferredRegister
            .create(Registries.MAP_DECORATION_TYPE, Constants.MOD_ID);

    private final DeferredRegister<CriterionTrigger<?>> criterionTriggers = DeferredRegister
            .create(Registries.TRIGGER_TYPE, Constants.MOD_ID);

    private final DeferredRegister<MobEffect> mobEffects = DeferredRegister.create(Registries.MOB_EFFECT,
            Constants.MOD_ID);

    private final DeferredRegister<MenuType<?>> menuTypes = DeferredRegister.create(Registries.MENU,
            Constants.MOD_ID);

    private final DeferredRegister<ResourceLocation> customStats = DeferredRegister
            .create(Registries.CUSTOM_STAT, Constants.MOD_ID);

    private final DeferredRegister<RecipeSerializer<?>> recipeSerializers = DeferredRegister
            .create(Registries.RECIPE_SERIALIZER, Constants.MOD_ID);

    private final DeferredRegister<RecipeType<?>> recipeTypes = DeferredRegister.create(Registries.RECIPE_TYPE,
            Constants.MOD_ID);

    private final DeferredRegister<BlockEntityType<?>> blockEntityTypes = DeferredRegister
            .create(Registries.BLOCK_ENTITY_TYPE, Constants.MOD_ID);

    private final DeferredRegister<SoundEvent> soundEvents = DeferredRegister.create(Registries.SOUND_EVENT,
            Constants.MOD_ID);

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
    public Supplier<LootItemConditionType> registerLootConditionType(String id,
            Supplier<LootItemConditionType> factory) {
        return lootConditionTypes.register(id, factory);
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T extends LootItemFunction> Supplier<LootItemFunctionType<T>> registerLootFunctionType(String id,
            Supplier<LootItemFunctionType<T>> factory) {
        return (Supplier<LootItemFunctionType<T>>) (Supplier<?>) lootFunctionTypes.register(id, factory::get);
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T extends PlacementModifier> Supplier<PlacementModifierType<T>> registerPlacementModifierType(String id,
            Supplier<PlacementModifierType<T>> factory) {
        return (Supplier<PlacementModifierType<T>>) (Supplier<?>) placementModifierTypes.register(id, factory::get);
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T extends TreeDecorator> Supplier<TreeDecoratorType<T>> registerTreeDecoratorType(String id,
            Supplier<TreeDecoratorType<T>> factory) {
        return (Supplier<TreeDecoratorType<T>>) (Supplier<?>) treeDecoratorTypes.register(id, factory::get);
    }

    @Override
    public Holder<MapDecorationType> registerMapDecorationType(String id, Supplier<MapDecorationType> factory) {
        return mapDecorationTypes.register(id, factory);
    }

    @Override
    public Holder<MobEffect> registerMobEffect(String id, Supplier<? extends MobEffect> factory) {
        return mobEffects.register(id, factory);
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T extends AbstractContainerMenu> Supplier<MenuType<T>> registerMenuType(String id,
            MenuType.MenuSupplier<T> factory) {
        return (Supplier<MenuType<T>>) (Supplier<?>) menuTypes.register(id,
                () -> new MenuType<>(factory, FeatureFlags.DEFAULT_FLAGS));
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T extends Recipe<?>> Supplier<RecipeSerializer<T>> registerRecipeSerializer(String id,
            Supplier<RecipeSerializer<T>> factory) {
        return (Supplier<RecipeSerializer<T>>) (Supplier<?>) recipeSerializers.register(id, factory::get);
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T extends Recipe<?>> Supplier<RecipeType<T>> registerRecipeType(String id) {
        return (Supplier<RecipeType<T>>) (Supplier<?>) recipeTypes.register(id,
                () -> IRegistryHelper.newRecipeType(ResourceLocation.fromNamespaceAndPath(Constants.MOD_ID, id)));
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T extends BlockEntity> Supplier<BlockEntityType<T>> registerBlockEntityType(String id,
            BiFunction<BlockPos, BlockState, T> factory, Collection<? extends Supplier<? extends Block>> blocks) {
        return (Supplier<BlockEntityType<T>>) (Supplier<?>) blockEntityTypes.register(id,
                () -> BlockEntityType.Builder.<T>of(factory::apply,
                        blocks.stream().map(Supplier::get).toArray(Block[]::new)).build(null));
    }

    @Override
    public Supplier<SoundEvent> registerSoundEvent(String id) {
        return soundEvents.register(id,
                () -> SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath(Constants.MOD_ID, id)));
    }

    @Override
    public void registerReloadListener(ResourceLocation id, PreparableReloadListener listener) {
        pendingReloadListeners.add(listener);
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T extends CriterionTrigger<?>> Supplier<T> registerCriterionTrigger(String id, Supplier<T> factory) {
        return (Supplier<T>) (Supplier<?>) criterionTriggers.register(id, factory::get);
    }

    @Override
    public ResourceLocation registerCustomStat(String id) {
        ResourceLocation location = ResourceLocation.fromNamespaceAndPath(Constants.MOD_ID, id);
        customStats.register(id, () -> location);
        return location;
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
        lootConditionTypes.register(eventBus);
        lootFunctionTypes.register(eventBus);
        mapDecorationTypes.register(eventBus);
        placementModifierTypes.register(eventBus);
        treeDecoratorTypes.register(eventBus);
        criterionTriggers.register(eventBus);
        mobEffects.register(eventBus);
        menuTypes.register(eventBus);
        customStats.register(eventBus);
        recipeSerializers.register(eventBus);
        recipeTypes.register(eventBus);
        blockEntityTypes.register(eventBus);
        soundEvents.register(eventBus);
        // AddReloadListenerEvent is a game event, not a mod-bus event.
        NeoForge.EVENT_BUS.addListener(this::onAddReloadListeners);
    }
}
