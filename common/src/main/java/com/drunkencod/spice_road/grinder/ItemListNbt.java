package com.drunkencod.spice_road.grinder;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;

/**
 * Reads and edits the items a block entity stores, straight from its saved
 * NBT, without knowing anything about the block entity's class. An item entry
 * is a compound with a string {@code id} and a {@code count}, sitting in a
 * list; that is how vanilla containers ({@code Items}), item handlers
 * ({@code ItemHandler.Items}) and most mods' inventories save their stacks. The
 * walk goes through every other compound and list but never into an entry, so
 * what a stored shulker box or bundle holds doesn't count.
 */
public final class ItemListNbt {

    private ItemListNbt() {
    }

    /**
     * @param tag The saved NBT of a block entity.
     * @return How many of each item (by ID string) it stores.
     */
    public static Map<String, Integer> count(CompoundTag tag) {
        Map<String, Integer> counts = new HashMap<>();
        walk(tag, (list, index, entry) -> {
            counts.merge(entry.getString("id"), countOf(entry), Integer::sum);
            return false;
        });
        return counts;
    }

    /**
     * Takes up to {@code amount} of an item out of the NBT, in place: counts
     * are lowered and emptied entries are removed from their list.
     *
     * @param tag    The saved NBT of a block entity, modified.
     * @param itemId The item's ID string.
     * @param amount The most to take.
     * @return How many were taken.
     */
    public static int remove(CompoundTag tag, String itemId, int amount) {
        return remove(tag, entry -> entry.getString("id").equals(itemId), amount);
    }

    /**
     * @param tag The saved NBT of a block entity.
     * @return Every item entry it stores, as saved (an {@code id}, a
     *         {@code count} and possibly {@code components}).
     */
    public static List<CompoundTag> entries(CompoundTag tag) {
        List<CompoundTag> entries = new ArrayList<>();
        walk(tag, (list, index, entry) -> {
            entries.add(entry);
            return false;
        });
        return entries;
    }

    /**
     * Takes up to {@code amount} items of the entries that match out of the
     * NBT, in place: counts are lowered and emptied entries are removed from
     * their list.
     *
     * @param tag     The saved NBT of a block entity, modified.
     * @param matches Which entries to take from.
     * @param amount  The most to take.
     * @return How many were taken.
     */
    public static int remove(CompoundTag tag, Predicate<CompoundTag> matches, int amount) {
        int[] left = { amount };
        walk(tag, (list, index, entry) -> {
            if (left[0] <= 0 || !matches.test(entry))
                return false;
            int count = countOf(entry);
            int taken = Math.min(count, left[0]);
            left[0] -= taken;
            if (taken >= count) {
                list.remove(index);
                return true;
            }
            setCount(entry, count - taken);
            return false;
        });
        return amount - left[0];
    }

    /**
     * @param entry An item entry.
     * @return How many items it holds.
     */
    public static int countOf(CompoundTag entry) {
        return entry.contains("count", Tag.TAG_ANY_NUMERIC) ? entry.getInt("count") : entry.getInt("Count");
    }

    /** What to do with an entry; returns whether it removed the entry from its list. */
    private interface EntryVisitor {
        boolean visit(ListTag list, int index, CompoundTag entry);
    }

    private static void walk(CompoundTag tag, EntryVisitor visitor) {
        for (String key : tag.getAllKeys())
            walkValue(tag.get(key), visitor);
    }

    private static void walkValue(Tag value, EntryVisitor visitor) {
        if (value instanceof CompoundTag compound) {
            if (!isEntry(compound))
                walk(compound, visitor);
        } else if (value instanceof ListTag list) {
            for (int i = list.size() - 1; i >= 0; i--) {
                Tag element = list.get(i);
                if (element instanceof CompoundTag compound && isEntry(compound))
                    visitor.visit(list, i, compound);
                else
                    walkValue(element, visitor);
            }
        }
    }

    private static boolean isEntry(CompoundTag tag) {
        return tag.contains("id", Tag.TAG_STRING)
                && (tag.contains("count", Tag.TAG_ANY_NUMERIC) || tag.contains("Count", Tag.TAG_ANY_NUMERIC));
    }

    private static void setCount(CompoundTag entry, int count) {
        entry.putInt(entry.contains("count", Tag.TAG_ANY_NUMERIC) ? "count" : "Count", count);
    }
}
