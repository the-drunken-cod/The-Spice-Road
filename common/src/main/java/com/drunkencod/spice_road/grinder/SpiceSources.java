package com.drunkencod.spice_road.grinder;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

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
import com.drunkencod.spice_road.registry.ModItems;
import com.drunkencod.spice_road.spice.SpiceProfileRegistry;

/**
 * Where a player's spices (loose, or in Spice Mixes) come from while using the
 * Spice Grinder: their own
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

    /**
     * @return How many of each loose Spice Item and each kind of Spice Mix the
     *         player's inventory and the nearby storage hold together.
     */
    public Map<GrinderSpice, Integer> available() {
        Map<GrinderSpice, Integer> total = new LinkedHashMap<>();
        Inventory inventory = player.getInventory();
        for (ItemStack stack : inventoryStacks(inventory))
            GrinderSpice.of(stack).ifPresent(spice -> total.merge(spice, stack.getCount(), Integer::sum));
        for (BlockEntity storage : storages) {
            for (CompoundTag entry : ItemListNbt.entries(storage.saveWithoutMetadata(registries)))
                spiceOf(entry).ifPresent(spice -> total.merge(spice, ItemListNbt.countOf(entry), Integer::sum));
        }
        return total;
    }

    /**
     * @param spice A loose spice or a kind of mix.
     * @return How many of it the player's inventory and the nearby storage hold together.
     */
    public int count(GrinderSpice spice) {
        return available().getOrDefault(spice, 0);
    }

    /**
     * Takes spices and mixes, from the inventory first and then from the
     * nearest storage. All or nothing: if anything can't be taken, everything
     * that was already taken from storage is put back and nothing is taken.
     *
     * @param amounts How many of each loose spice and kind of mix to take.
     * @return Whether it was all taken.
     */
    public boolean consume(Map<GrinderSpice, Integer> amounts) {
        Map<GrinderSpice, Integer> available = available();
        for (Map.Entry<GrinderSpice, Integer> amount : amounts.entrySet()) {
            if (available.getOrDefault(amount.getKey(), 0) < amount.getValue())
                return false;
        }

        Map<BlockEntity, CompoundTag> originals = new LinkedHashMap<>();
        Map<GrinderSpice, Integer> fromInventory = new HashMap<>();
        for (Map.Entry<GrinderSpice, Integer> amount : amounts.entrySet()) {
            GrinderSpice spice = amount.getKey();
            int fromHere = Math.min(amount.getValue(), inventoryCount(spice));
            fromInventory.put(spice, fromHere);
            int missing = amount.getValue() - fromHere;
            for (BlockEntity storage : storages) {
                if (missing <= 0)
                    break;
                int taken = takeFromStorage(storage, spice, missing, originals);
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

    /**
     * Reads what a saved item entry would be to the Grinder. Only Spice Mix
     * entries are parsed as whole stacks, since only they need their
     * components; anything else is judged by its ID.
     */
    private Optional<GrinderSpice> spiceOf(CompoundTag entry) {
        Item item = BuiltInRegistries.ITEM.getOptional(ResourceLocation.tryParse(entry.getString("id"))).orElse(null);
        if (item == null)
            return Optional.empty();
        if (item == ModItems.SPICE_MIX.get())
            return ItemStack.parse(registries, entry).flatMap(GrinderSpice::of);
        return SpiceProfileRegistry.getDefault(item).isPresent() ? Optional.of(GrinderSpice.loose(item))
                : Optional.empty();
    }

    private int storedCount(CompoundTag saved, GrinderSpice spice) {
        int count = 0;
        for (CompoundTag entry : ItemListNbt.entries(saved)) {
            if (spiceOf(entry).filter(spice::equals).isPresent())
                count += ItemListNbt.countOf(entry);
        }
        return count;
    }

    /** @return How many were taken, or {@code -1} if the storage didn't take them and was left as it was. */
    private int takeFromStorage(BlockEntity storage, GrinderSpice spice, int wanted,
            Map<BlockEntity, CompoundTag> originals) {
        CompoundTag before = storage.saveWithoutMetadata(registries);
        int stored = storedCount(before, spice);
        int take = Math.min(stored, wanted);
        if (take <= 0)
            return 0;
        originals.putIfAbsent(storage, before.copy());

        if (storage instanceof Container container) {
            takeFromContainer(container, spice, take);
        } else {
            CompoundTag edited = before.copy();
            ItemListNbt.remove(edited, entry -> spiceOf(entry).filter(spice::equals).isPresent(), take);
            storage.loadWithComponents(edited, registries);
        }
        storage.setChanged();
        int after = storedCount(storage.saveWithoutMetadata(registries), spice);
        if (after != stored - take) {
            revert(originals);
            return -1;
        }
        if (storage.getLevel() != null)
            storage.getLevel().sendBlockUpdated(storage.getBlockPos(), storage.getBlockState(),
                    storage.getBlockState(), 3);
        return take;
    }

    private static void takeFromContainer(Container container, GrinderSpice spice, int amount) {
        int left = amount;
        for (int slot = 0; slot < container.getContainerSize() && left > 0; slot++) {
            ItemStack stack = container.getItem(slot);
            if (spice.matches(stack))
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

    private int inventoryCount(GrinderSpice spice) {
        int count = 0;
        for (ItemStack stack : inventoryStacks(player.getInventory()))
            count += spice.matches(stack) ? stack.getCount() : 0;
        return count;
    }

    private void removeFromInventory(GrinderSpice spice, int amount) {
        int left = amount;
        for (ItemStack stack : inventoryStacks(player.getInventory())) {
            if (left <= 0)
                break;
            if (spice.matches(stack)) {
                int taken = Math.min(left, stack.getCount());
                stack.shrink(taken);
                left -= taken;
            }
        }
    }
}
