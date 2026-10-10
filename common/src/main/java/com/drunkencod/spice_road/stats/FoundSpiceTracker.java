package com.drunkencod.spice_road.stats;

import java.util.HashMap;
import java.util.Map;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import com.drunkencod.spice_road.block.SpiceHarvesting;
import com.drunkencod.spice_road.platform.Services;
import com.drunkencod.spice_road.spice.Spice;

/**
 * Notices Spices entering players' inventories, by scanning every player's
 * inventory for raw Spice items at a configurable interval, keeps
 * {@link ModStats#SPICES_FOUND} in step with {@link SpiceFindings}, and tells
 * a player's client when their set grows.
 */
public final class FoundSpiceTracker {

    private static Map<Item, Spice> rawItems;

    private FoundSpiceTracker() {
    }

    /**
     * Called at the end of every server tick by the loaders.
     *
     * @param server The server.
     */
    public static void onServerTick(MinecraftServer server) {
        int interval = Services.CONFIG.getStatsFoundScanIntervalTicks();
        if (interval <= 0 || server.getTickCount() % interval != 0)
            return;
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            if (SpiceHarvesting.isConnectedPlayer(player))
                scan(player);
        }
    }

    /**
     * Records every Spice whose raw item is in the player's inventory and tops
     * {@link ModStats#SPICES_FOUND} up to the number of Spices found, so the
     * stat recovers if the player's stats were reset.
     *
     * @param player The player to scan.
     */
    static void scan(ServerPlayer player) {
        SpiceFindings findings = SpiceFindings.of(player.server);
        Map<Item, Spice> raw = rawItems();
        Inventory inventory = player.getInventory();
        boolean foundNew = false;
        for (int slot = 0; slot < inventory.getContainerSize(); slot++) {
            ItemStack stack = inventory.getItem(slot);
            Spice spice = stack.isEmpty() ? null : raw.get(stack.getItem());
            if (spice != null)
                foundNew |= findings.add(player.getUUID(), spice);
        }
        if (foundNew)
            Services.NETWORK.sendToPlayer(player, SpiceFindingsSync.of(player));
        int missing = findings.count(player.getUUID()) - ModStats.get(player, ModStats.SPICES_FOUND);
        if (missing > 0)
            player.awardStat(ModStats.SPICES_FOUND, missing);
    }

    /** @return Every Spice by its raw item, built once the items are registered. */
    private static Map<Item, Spice> rawItems() {
        if (rawItems == null) {
            Map<Item, Spice> items = new HashMap<>();
            for (Spice spice : Spice.values()) {
                Item item = Spice.getRawById(spice.getId());
                if (item != null)
                    items.put(item, spice);
            }
            rawItems = items;
        }
        return rawItems;
    }
}
