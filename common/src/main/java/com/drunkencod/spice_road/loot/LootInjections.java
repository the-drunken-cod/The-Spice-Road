package com.drunkencod.spice_road.loot;

import java.util.Optional;

import org.jetbrains.annotations.Nullable;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.world.level.storage.loot.LootPool;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.entries.NestedLootTable;
import net.minecraft.world.level.storage.loot.providers.number.ConstantValue;

import com.drunkencod.spice_road.Constants;

/**
 * Injects loot into existing loot tables by convention: a loot table at
 * {@code spice_road:inject/<namespace>/<path>} is rolled whenever the table
 * {@code <namespace>:<path>} is. Datapacks can override or add injections
 * by placing a table at the matching path.
 * <p>
 * Each loader calls {@link #poolFor} from its own loot table load hook and
 * appends the returned pool to the target table.
 */
public final class LootInjections {

    /** Path prefix of inject tables, within this mod's namespace. */
    private static final String INJECT_PREFIX = "inject/";

    /**
     * The resource manager loot tables are currently being loaded from. Set
     * right before loading starts, since the loaders' load hooks don't expose
     * it.
     */
    private static volatile @Nullable ResourceManager resourceManager;

    private LootInjections() {
    }

    /**
     * Remembers the resource manager loot tables are about to be loaded
     * from.
     *
     * @param manager The resource manager of the ongoing reload.
     */
    public static void setResourceManager(ResourceManager manager) {
        resourceManager = manager;
    }

    /**
     * Builds the pool to append to {@code target}, if an inject table exists
     * for it.
     *
     * @param target ID of the loot table being loaded.
     * @return A pool rolling the matching inject table once, or empty if
     *         there is none.
     */
    public static Optional<LootPool> poolFor(ResourceLocation target) {
        ResourceManager manager = resourceManager;
        boolean isInjectTable = target.getNamespace().equals(Constants.MOD_ID)
                && target.getPath().startsWith(INJECT_PREFIX);
        if (manager == null || isInjectTable)
            return Optional.empty();

        ResourceLocation injectId = ResourceLocation.fromNamespaceAndPath(Constants.MOD_ID,
                INJECT_PREFIX + target.getNamespace() + "/" + target.getPath());
        ResourceLocation file = injectId.withPath(path -> Registries.elementsDirPath(Registries.LOOT_TABLE) + "/" + path
                + ".json");
        if (manager.getResource(file).isEmpty())
            return Optional.empty();

        ResourceKey<LootTable> injectKey = ResourceKey.create(Registries.LOOT_TABLE, injectId);
        return Optional.of(LootPool.lootPool()
                .setRolls(ConstantValue.exactly(1))
                .add(NestedLootTable.lootTableReference(injectKey))
                .build());
    }
}
