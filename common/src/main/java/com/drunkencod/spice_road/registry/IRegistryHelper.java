package com.drunkencod.spice_road.registry;

import java.util.Collection;
import java.util.List;
import java.util.function.BiFunction;
import java.util.function.Supplier;
import net.minecraft.advancements.CriterionTrigger;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.PreparableReloadListener;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.effect.MobEffect;
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

/**
 * Cross-loader service interface for registering items and blocks.
 * <p>
 * Usage:
 * 
 * <pre>{@code
 * public static final Supplier<MyItem> MY_ITEM = Services.REGISTRY.registerItem("my_item", MyItem::new);
 * }</pre>
 * 
 * On NeoForge, call {@code Services.REGISTRY.initialize(eventBus)} in your mod
 * constructor
 * before any registrations are used.
 */
public interface IRegistryHelper {

    /**
     * Register an item under the mod's namespace.
     *
     * @param id      Registry path (e.g. {@code "my_item"})
     * @param factory Supplier that creates the item instance
     */
    <T extends Item> Supplier<T> registerItem(String id, Supplier<T> factory);

    /**
     * Register a block under the mod's namespace.
     *
     * @param id      Registry path (e.g. {@code "my_block"})
     * @param factory Supplier that creates the block instance
     */
    <T extends Block> Supplier<T> registerBlock(String id, Supplier<T> factory);

    /**
     * Register a worldgen {@link Feature} type under the mod's namespace.
     * <p>
     * Unlike vanilla content registries like Item/Block, {@code Feature}
     * types are frozen very early (during vanilla bootstrap, before mods
     * even construct) - a direct {@code Registry.register} call at mod-init
     * time throws {@code IllegalStateException: Registry is already
     * frozen}. This must go through the same DeferredRegister-at-RegisterEvent
     * mechanism as items/blocks on NeoForge, which does handle the timing
     * correctly.
     *
     * @param id      Registry path (e.g. {@code "my_feature"})
     * @param factory Supplier that creates the feature instance
     */
    <T extends Feature<?>> Supplier<T> registerFeature(String id, Supplier<T> factory);

    /**
     * Register a custom {@link DataComponentType} under the mod's namespace.
     *
     * @param id      Registry path (e.g. {@code "my_component"})
     * @param factory Supplier that creates the data component type instance
     */
    <T> Supplier<DataComponentType<T>> registerDataComponentType(String id, Supplier<DataComponentType<T>> factory);

    /**
     * Register a custom {@link LootItemConditionType} under the mod's namespace.
     * <p>
     * Like {@link #registerFeature}, {@code BuiltInRegistries.LOOT_CONDITION_TYPE}
     * freezes during vanilla bootstrap, before mods even construct, so this needs
     * the same DeferredRegister-at-RegisterEvent mechanism on NeoForge.
     *
     * @param id      Registry path (e.g. {@code "my_condition"})
     * @param factory Supplier that creates the loot condition type instance
     */
    Supplier<LootItemConditionType> registerLootConditionType(String id, Supplier<LootItemConditionType> factory);

    /**
     * Register a custom {@link LootItemFunctionType} under the mod's namespace.
     * Frozen as early as {@link #registerLootConditionType}, with the same
     * consequences.
     *
     * @param id      Registry path (e.g. {@code "my_function"})
     * @param factory Supplier that creates the loot function type instance
     */
    <T extends LootItemFunction> Supplier<LootItemFunctionType<T>> registerLootFunctionType(String id,
            Supplier<LootItemFunctionType<T>> factory);

    /**
     * Register a custom {@link PlacementModifierType} under the mod's
     * namespace. Frozen as early as {@link #registerFeature}, with the same
     * consequences.
     *
     * @param id      Registry path (e.g. {@code "my_modifier"})
     * @param factory Supplier that creates the placement modifier type instance
     */
    <T extends PlacementModifier> Supplier<PlacementModifierType<T>> registerPlacementModifierType(String id,
            Supplier<PlacementModifierType<T>> factory);

    /**
     * Register a custom {@link TreeDecoratorType} under the mod's namespace.
     * Frozen as early as {@link #registerFeature}, with the same consequences.
     *
     * @param id      Registry path (e.g. {@code "my_decorator"})
     * @param factory Supplier that creates the tree decorator type instance
     */
    <T extends TreeDecorator> Supplier<TreeDecoratorType<T>> registerTreeDecoratorType(String id,
            Supplier<TreeDecoratorType<T>> factory);

    /**
     * Register a custom {@link MapDecorationType} under the mod's namespace.
     * Its sprite is read from
     * {@code assets/<namespace>/textures/map/decorations/<asset path>.png}.
     *
     * @param id      Registry path (e.g. {@code "my_marker"})
     * @param factory Supplier that creates the decoration type instance
     * @return A holder of the registered type, only resolvable once
     *         registration has run.
     */
    Holder<MapDecorationType> registerMapDecorationType(String id, Supplier<MapDecorationType> factory);

    /**
     * Register a {@link MobEffect} under the mod's namespace. Its icon is read
     * from {@code assets/<namespace>/textures/mob_effect/<id>.png}.
     *
     * @param id      Registry path (e.g. {@code "my_effect"})
     * @param factory Supplier that creates the effect instance
     * @return A holder of the registered effect, only resolvable once
     *         registration has run.
     */
    Holder<MobEffect> registerMobEffect(String id, Supplier<? extends MobEffect> factory);

    /**
     * Register a {@link MenuType} under the mod's namespace.
     *
     * @param id      Registry path (e.g. {@code "my_menu"})
     * @param factory Creates the menu on the client, from the menu ID and the inventory
     * @param <T>     The menu type
     * @return A supplier of the registered type, only usable once registration has run.
     */
    <T extends AbstractContainerMenu> Supplier<MenuType<T>> registerMenuType(String id,
            MenuType.MenuSupplier<T> factory);

    /**
     * Register a {@link RecipeSerializer} under the mod's namespace. Frozen as
     * early as {@link #registerFeature}, with the same consequences.
     *
     * @param id      Registry path (e.g. {@code "my_recipe"})
     * @param factory Supplier that creates the serializer instance
     * @param <T>     The recipe type the serializer reads and writes
     * @return A supplier of the registered serializer, only usable once
     *         registration has run.
     */
    <T extends Recipe<?>> Supplier<RecipeSerializer<T>> registerRecipeSerializer(String id,
            Supplier<RecipeSerializer<T>> factory);

    /**
     * Register a {@link RecipeType} under the mod's namespace. Frozen as early
     * as {@link #registerFeature}, with the same consequences.
     *
     * @param id  Registry path (e.g. {@code "my_recipe"}), which is also the
     *            type's ID
     * @param <T> The recipe class of the type
     * @return A supplier of the registered type, only usable once registration
     *         has run.
     */
    <T extends Recipe<?>> Supplier<RecipeType<T>> registerRecipeType(String id);

    /**
     * Creates the (still unregistered) type {@link #registerRecipeType} registers;
     * vanilla only offers this through its own, vanilla-namespaced registration.
     *
     * @param id  The type's ID, used as its name in logs
     * @param <T> The recipe class of the type
     * @return A new recipe type.
     */
    static <T extends Recipe<?>> RecipeType<T> newRecipeType(ResourceLocation id) {
        return new RecipeType<>() {
            @Override
            public String toString() {
                return id.toString();
            }
        };
    }

    /**
     * Register a {@link BlockEntityType} under the mod's namespace. Frozen as
     * early as {@link #registerFeature}, with the same consequences.
     *
     * @param id      Registry path (e.g. {@code "my_block_entity"})
     * @param factory Creates a block entity of the type
     * @param blocks  The blocks the type is valid for; on Fabric these are read
     *                immediately, so they must already be registered
     * @param <T>     The block entity class
     * @return A supplier of the registered type, only usable once registration
     *         has run.
     */
    <T extends BlockEntity> Supplier<BlockEntityType<T>> registerBlockEntityType(String id,
            BiFunction<BlockPos, BlockState, T> factory, Collection<? extends Supplier<? extends Block>> blocks);

    /**
     * {@link #registerBlockEntityType(String, BiFunction, Collection)} for a
     * type valid for a single block.
     *
     * @param id      Registry path (e.g. {@code "my_block_entity"})
     * @param factory Creates a block entity of the type
     * @param block   The block the type is valid for
     * @param <T>     The block entity class
     * @return A supplier of the registered type, only usable once registration
     *         has run.
     */
    default <T extends BlockEntity> Supplier<BlockEntityType<T>> registerBlockEntityType(String id,
            BiFunction<BlockPos, BlockState, T> factory, Supplier<? extends Block> block) {
        return registerBlockEntityType(id, factory, List.of(block));
    }

    /**
     * Register a {@link SoundEvent} under the mod's namespace, defined in
     * {@code sounds.json}. Frozen as early as {@link #registerFeature}, with the
     * same consequences.
     *
     * @param id Registry path (e.g. {@code "block.my_block.click"}), which is
     *           also the event's ID
     * @return A supplier of the registered event, only usable once registration
     *         has run.
     */
    Supplier<SoundEvent> registerSoundEvent(String id);

    /**
     * Register a datapack JSON reload listener under the mod's namespace.
     * <p>
     * On NeoForge this is necessarily deferred until
     * {@code AddServerReloadListenersEvent} fires, since that's the earliest
     * point the listener list can be appended to; calling this before
     * {@code NeoForgeRegistryHelper#initialize} has wired that event up is safe,
     * the registration
     * is just queued.
     *
     * @param id       Unique id for the listener (used for reload
     *                 ordering/dependency purposes)
     * @param listener The reload listener to register
     */
    void registerReloadListener(ResourceLocation id, PreparableReloadListener listener);

    /**
     * Register a custom {@link CriterionTrigger} under the mod's namespace.
     * Frozen as early as {@link #registerFeature}, with the same
     * consequences.
     *
     * @param id      Registry path (e.g. {@code "my_trigger"})
     * @param factory Supplier that creates the trigger instance
     */
    <T extends CriterionTrigger<?>> Supplier<T> registerCriterionTrigger(String id, Supplier<T> factory);

    /**
     * Register a custom stat under the mod's namespace. Its name is read from
     * the lang key {@code stat.<namespace>.<id>}. Frozen as early as
     * {@link #registerFeature}, with the same consequences.
     *
     * @param id Registry path (e.g. {@code "my_stat"})
     * @return The registered ID, which is the very instance the stats files
     *         resolve back to: {@code Stats.CUSTOM} keys stats by identity, so
     *         always use this one rather than building an equal one.
     */
    ResourceLocation registerCustomStat(String id);
}
