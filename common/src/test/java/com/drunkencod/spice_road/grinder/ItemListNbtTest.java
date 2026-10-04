package com.drunkencod.spice_road.grinder;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Map;

import org.junit.jupiter.api.Test;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;

class ItemListNbtTest {

    private static CompoundTag stack(String id, int count, int slot) {
        CompoundTag tag = new CompoundTag();
        tag.putByte("Slot", (byte) slot);
        tag.putString("id", id);
        tag.putInt("count", count);
        return tag;
    }

    private static ListTag list(CompoundTag... entries) {
        ListTag list = new ListTag();
        for (CompoundTag entry : entries)
            list.add(entry);
        return list;
    }

    /** A vanilla container: {@code Items} at the root. */
    private static CompoundTag chest() {
        CompoundTag tag = new CompoundTag();
        tag.put("Items", list(stack("minecraft:sugar", 5, 0), stack("minecraft:sugar", 3, 4),
                stack("minecraft:cocoa_beans", 2, 7)));
        tag.putString("id", "minecraft:trapped_chest");
        return tag;
    }

    /** An item handler: {@code ItemHandler.Items}, like the Cooking for Blockheads spice rack. */
    private static CompoundTag handler() {
        CompoundTag handler = new CompoundTag();
        handler.putInt("Size", 9);
        handler.put("Items", list(stack("minecraft:sugar", 7, 2)));
        CompoundTag tag = new CompoundTag();
        tag.put("ItemHandler", handler);
        tag.putString("id", "cookingforblockheads:spice_rack");
        return tag;
    }

    @Test
    void countsEveryStackOfAnItemInAVanillaContainer() {
        assertEquals(Map.of("minecraft:sugar", 8, "minecraft:cocoa_beans", 2), ItemListNbt.count(chest()));
    }

    @Test
    void findsTheItemsOfAnItemHandlerNoMatterHowDeepTheyAreNested() {
        assertEquals(Map.of("minecraft:sugar", 7), ItemListNbt.count(handler()));
    }

    @Test
    void theBlockEntitiesOwnIdIsNotAnItem() {
        CompoundTag tag = chest();
        tag.putInt("count", 4); // even a count next to the block entity's own id
        assertFalse(ItemListNbt.count(tag).containsKey("minecraft:trapped_chest"));
    }

    @Test
    void doesNotLookInsideAStoredStack() {
        CompoundTag shulker = stack("minecraft:shulker_box", 1, 0);
        CompoundTag components = new CompoundTag();
        components.put("minecraft:container", list(stack("minecraft:sugar", 64, 0)));
        shulker.put("components", components);
        CompoundTag tag = new CompoundTag();
        tag.put("Items", list(shulker, stack("minecraft:sugar", 1, 1)));
        assertEquals(Map.of("minecraft:shulker_box", 1, "minecraft:sugar", 1), ItemListNbt.count(tag));
    }

    @Test
    void readsTheLegacyCapitalizedCount() {
        CompoundTag entry = new CompoundTag();
        entry.putString("id", "minecraft:sugar");
        entry.putByte("Count", (byte) 6);
        CompoundTag tag = new CompoundTag();
        tag.put("Items", list(entry));
        assertEquals(Map.of("minecraft:sugar", 6), ItemListNbt.count(tag));
    }

    @Test
    void removingLowersCountsAndDropsEmptiedEntries() {
        CompoundTag tag = chest();
        assertEquals(6, ItemListNbt.remove(tag, "minecraft:sugar", 6));
        assertEquals(Map.of("minecraft:sugar", 2, "minecraft:cocoa_beans", 2), ItemListNbt.count(tag));
        assertEquals(2, tag.getList("Items", 10).size() , "the emptied stack is gone from the list");
    }

    @Test
    void removingMoreThanThereIsTakesWhatIsThere() {
        CompoundTag tag = handler();
        assertEquals(7, ItemListNbt.remove(tag, "minecraft:sugar", 100));
        assertTrue(ItemListNbt.count(tag).isEmpty());
        assertEquals(0, ItemListNbt.remove(tag, "minecraft:sugar", 1));
    }

    @Test
    void removingLeavesOtherItemsAndOtherFieldsAlone() {
        CompoundTag tag = handler();
        assertEquals(0, ItemListNbt.remove(tag, "minecraft:cocoa_beans", 5));
        assertEquals(9, tag.getCompound("ItemHandler").getInt("Size"));
        assertEquals(Map.of("minecraft:sugar", 7), ItemListNbt.count(tag));
    }
}
