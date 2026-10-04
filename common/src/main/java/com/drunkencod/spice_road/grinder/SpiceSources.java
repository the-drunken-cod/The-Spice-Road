package com.drunkencod.spice_road.grinder;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.chunk.LevelChunk;

import com.drunkencod.spice_road.block.SpiceBlockTags;
import com.drunkencod.spice_road.platform.Services;
import com.drunkencod.spice_road.spice.SpiceProfileRegistry;

/**
 * Where a player's spices come from while using the Spice Grinder: their own
 * inventory first, then the block entities of blocks in
 * {@link SpiceBlockTags#SPICE_STORAGE} within the configured radius, nearest
 * first. Storage is read from the block entity's saved NBT (see
 * {@link ItemListNbt}), so any block that saves its items in a list works
 * without code for it, and spices are taken through the block entity's
 * {@link Container} when it is one, or by editing that NBT otherwise.
 * <p>
 * An instance is a snapshot of what is nearby when it was made; make a new one
 * for every action.
 */
public final class SpiceSources {

    /** Most storage blocks looked at, so a crowded base can't make a scan expensive. */
    public static final int MAX_STORAGES = 64;

    private final ServerPlayer player;
    private final HolderLookup.Provider registries;
    private final List<BlockEntity> storages;

    private SpiceSources(ServerPlayer player, List<BlockEntity> storages) {
        this.player = player;
        this.registries = player.serverLevel().registryAccess();
        this.storages = storages;
    }

    /**
     * @param player A player.
     * @return A snapshot of the player's inventory and the spice storage near them.
     */
    public static SpiceSources of(ServerPlayer player) {
        return new SpiceSources(player, nearbyStorages(player));
    }

    private static List<BlockEntity> nearbyStorages(ServerPlayer player) {
        int radius = Services.CONFIG.getSeasoningStorageRadius();
        if (radius <= 0)
            return List.of();
        ServerLevel level = player.serverLevel();
        BlockPos center = player.blockPosition();
        double radiusSq = (double) radius * radius;
        List<BlockEntity> found = new ArrayList<>();
        for (int chunkX = (center.getX() - radius) >> 4; chunkX <= (center.getX() + radius) >> 4; chunkX++) {
            for (int chunkZ = (center.getZ() - radius) >> 4; chunkZ <= (center.getZ() + radius) >> 4; chunkZ++) {
                LevelChunk chunk = level.getChunkSource().getChunkNow(chunkX, chunkZ);
                if (chunk == null)
                    continue;
                for (BlockEntity entity : chunk.getBlockEntities().values()) {
                    if (entity.getBlockState().is(SpiceBlockTags.SPICE_STORAGE)
                            && entity.getBlockPos().distToCenterSqr(player.position()) <= radiusSq)
                        found.add(entity);
                }
            }
        }
        found.sort(Comparator.comparingDouble(entity -> entity.getBlockPos().distToCenterSqr(player.position())));
        return found.size() > MAX_STORAGES ? found.subList(0, MAX_STORAGES) : found;
    }

    /** @return How many of each Spice Item the player's inventory and the nearby storage hold together. */
    public Map<Item, Integer> available() {
        Map<Item, Integer> total = new LinkedHashMap<>();
        Inventory inventory = player.getInventory();
        for (ItemStack stack : inventoryStacks(inventory)) {
            if (!stack.isEmpty() && SpiceProfileRegistry.getDefault(stack.getItem()).isPresent())
                total.merge(stack.getItem(), stack.getCount(), Integer::sum);
        }
        for (BlockEntity storage : storages) {
            storedCounts(storage).forEach((id, count) -> {
                Item item = BuiltInRegistries.ITEM.getOptional(ResourceLocation.tryParse(id))
                        .orElse(null);
                if (item != null && SpiceProfileRegistry.getDefault(item).isPresent())
                    total.merge(item, count, Integer::sum);
            });
        }
        return total;
    }

    /**
     * @param item A Spice Item.
     * @return How many of it the player's inventory and the nearby storage hold together.
     */
    public int count(Item item) {
        return available().getOrDefault(item, 0);
    }

    /**
     * Takes spices, from the inventory first and then from the nearest
     * storage. All or nothing: if anything can't be taken, everything that was
     * already taken from storage is put back and nothing is taken.
     *
     * @param amounts How many of each Spice Item to take.
     * @return Whether it was all taken.
     */
    public boolean consume(Map<Item, Integer> amounts) {
        Map<Item, Integer> available = available();
        for (Map.Entry<Item, Integer> amount : amounts.entrySet()) {
            if (available.getOrDefault(amount.getKey(), 0) < amount.getValue())
                return false;
        }

        Map<BlockEntity, CompoundTag> originals = new LinkedHashMap<>();
        Map<Item, Integer> fromInventory = new HashMap<>();
        for (Map.Entry<Item, Integer> amount : amounts.entrySet()) {
            Item item = amount.getKey();
            int fromHere = Math.min(amount.getValue(), inventoryCount(item));
            fromInventory.put(item, fromHere);
            int missing = amount.getValue() - fromHere;
            for (BlockEntity storage : storages) {
                if (missing <= 0)
                    break;
                int taken = takeFromStorage(storage, item, missing, originals);
                if (taken < 0) {
                    revert(originals);
                    return false;
                }
                missing -= taken;
            }
            if (missing > 0) {
                revert(originals);
                return false;
            }
        }
        fromInventory.forEach(this::removeFromInventory);
        return true;
    }

    // #region Storage

    private Map<String, Integer> storedCounts(BlockEntity storage) {
        return ItemListNbt.count(storage.saveWithoutMetadata(registries));
    }

    /** @return How many were taken, or {@code -1} if the storage didn't take them and was left as it was. */
    private int takeFromStorage(BlockEntity storage, Item item, int wanted, Map<BlockEntity, CompoundTag> originals) {
        String id = BuiltInRegistries.ITEM.getKey(item).toString();
        CompoundTag before = storage.saveWithoutMetadata(registries);
        int stored = ItemListNbt.count(before).getOrDefault(id, 0);
        int take = Math.min(stored, wanted);
        if (take <= 0)
            return 0;
        originals.putIfAbsent(storage, before.copy());

        if (storage instanceof Container container) {
            takeFromContainer(container, item, take);
        } else {
            CompoundTag edited = before.copy();
            ItemListNbt.remove(edited, id, take);
            storage.loadWithComponents(edited, registries);
        }
        storage.setChanged();
        int after = storedCounts(storage).getOrDefault(id, 0);
        if (after != stored - take) {
            revert(originals);
            return -1;
        }
        if (storage.getLevel() != null)
            storage.getLevel().sendBlockUpdated(storage.getBlockPos(), storage.getBlockState(),
                    storage.getBlockState(), 3);
        return take;
    }

    private static void takeFromContainer(Container container, Item item, int amount) {
        int left = amount;
        for (int slot = 0; slot < container.getContainerSize() && left > 0; slot++) {
            ItemStack stack = container.getItem(slot);
            if (stack.is(item))
                left -= container.removeItem(slot, Math.min(left, stack.getCount())).getCount();
        }
    }

    private void revert(Map<BlockEntity, CompoundTag> originals) {
        originals.forEach((storage, original) -> {
            storage.loadWithComponents(original, registries);
            storage.setChanged();
            if (storage.getLevel() != null)
                storage.getLevel().sendBlockUpdated(storage.getBlockPos(), storage.getBlockState(),
                        storage.getBlockState(), 3);
        });
    }

    // #region Inventory

    private static List<ItemStack> inventoryStacks(Inventory inventory) {
        List<ItemStack> stacks = new ArrayList<>(inventory.items);
        stacks.addAll(inventory.offhand);
        return stacks;
    }

    private int inventoryCount(Item item) {
        int count = 0;
        for (ItemStack stack : inventoryStacks(player.getInventory()))
            count += stack.is(item) ? stack.getCount() : 0;
        return count;
    }

    private void removeFromInventory(Item item, int amount) {
        int left = amount;
        for (ItemStack stack : inventoryStacks(player.getInventory())) {
            if (left <= 0)
                break;
            if (stack.is(item)) {
                int taken = Math.min(left, stack.getCount());
                stack.shrink(taken);
                left -= taken;
            }
        }
    }
}
