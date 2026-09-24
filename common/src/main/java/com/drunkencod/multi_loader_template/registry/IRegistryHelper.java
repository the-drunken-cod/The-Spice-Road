package com.drunkencod.multi_loader_template.registry;

import java.util.function.Supplier;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;

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
}
