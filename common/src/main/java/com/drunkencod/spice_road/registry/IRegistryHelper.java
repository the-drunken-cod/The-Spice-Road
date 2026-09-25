package com.drunkencod.spice_road.registry;

import java.util.function.Supplier;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.PreparableReloadListener;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.levelgen.feature.Feature;

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
     * @return A supplier that returns the registered item
     */
    <T extends Item> Supplier<T> registerItem(String id, Supplier<T> factory);

    /**
     * Register a block under the mod's namespace.
     *
     * @param id      Registry path (e.g. {@code "my_block"})
     * @param factory Supplier that creates the block instance
     * @return A supplier that returns the registered block
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
     * @return A supplier that returns the registered feature
     */
    <T extends Feature<?>> Supplier<T> registerFeature(String id, Supplier<T> factory);

    /**
     * Register a custom {@link DataComponentType} under the mod's namespace.
     *
     * @param id      Registry path (e.g. {@code "my_component"})
     * @param factory Supplier that creates the data component type instance
     * @return A supplier that returns the registered data component type
     */
    <T> Supplier<DataComponentType<T>> registerDataComponentType(String id, Supplier<DataComponentType<T>> factory);

    /**
     * Register a datapack JSON reload listener under the mod's namespace.
     * <p>
     * On NeoForge this is necessarily deferred until
     * {@code AddServerReloadListenersEvent} fires, since that's the earliest
     * point the listener list can be appended to; calling this before
     * {@link #initialize} has wired that event up is safe, the registration
     * is just queued.
     *
     * @param id       Unique id for the listener (used for reload
     *                 ordering/dependency purposes)
     * @param listener The reload listener to register
     */
    void registerReloadListener(ResourceLocation id, PreparableReloadListener listener);
}
