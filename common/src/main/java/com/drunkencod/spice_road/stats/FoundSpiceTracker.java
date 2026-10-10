package com.drunkencod.spice_road.stats;

import java.util.Set;
import java.util.stream.Collectors;

import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;

import com.drunkencod.spice_road.advancement.ModCriteriaTriggers;
import com.drunkencod.spice_road.block.SpiceHarvesting;
import com.drunkencod.spice_road.item.ItemIdentification;
import com.drunkencod.spice_road.item.SpiceItemTags;
import com.drunkencod.spice_road.platform.Services;

/**
 * Notices Spices entering players' inventories, by scanning every player's
 * inventory for raw Spice items ({@link SpiceItemTags#RAW_SPICES}, so
 * datapack-added ones count) at a configurable interval, keeps
 * {@link ModStats#SPICES_FOUND} in step with {@link SpiceFindings}, tells a
 * player's client when their set grows, and fires
 * {@link ModCriteriaTriggers#ALL_SPICES_FOUND} once they have found them all.
 */
public final class FoundSpiceTracker {

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
     * Records every raw Spice in the player's inventory and tops
     * {@link ModStats#SPICES_FOUND} up to the number of Spices found, so the
     * stat recovers if the player's stats were reset. Fires
     * {@link ModCriteriaTriggers#ALL_SPICES_FOUND} if that covers every raw
     * Spice; checked on every scan rather than only when something new turns
     * up, so a player who already found them all before the advancement
     * existed, or before it was reloaded, still gets it.
     *
     * @param player The player to scan.
     */
    static void scan(ServerPlayer player) {
        SpiceFindings findings = SpiceFindings.of(player.server);
        Inventory inventory = player.getInventory();
        boolean foundNew = false;
        for (int slot = 0; slot < inventory.getContainerSize(); slot++) {
            ItemStack stack = inventory.getItem(slot);
            if (ItemIdentification.isRawSpice(stack))
                foundNew |= findings.add(player.getUUID(), BuiltInRegistries.ITEM.getKey(stack.getItem()));
        }
        if (foundNew)
            Services.NETWORK.sendToPlayer(player, SpiceFindingsSync.of(player));
        int missing = findings.count(player.getUUID()) - ModStats.get(player, ModStats.SPICES_FOUND);
        if (missing > 0)
            player.awardStat(ModStats.SPICES_FOUND, missing);
        if (findings.hasFoundAll(player.getUUID(), allRawSpices()))
            ModCriteriaTriggers.ALL_SPICES_FOUND.get().trigger(player);
    }

    /** @return The IDs of every raw Spice item currently in the tag, empty if it isn't bound. */
    private static Set<ResourceLocation> allRawSpices() {
        return BuiltInRegistries.ITEM.getTag(SpiceItemTags.RAW_SPICES)
                .map(set -> set.stream()
                        .map(Holder::unwrapKey)
                        .flatMap(key -> key.stream())
                        .map(ResourceKey::location)
                        .collect(Collectors.toUnmodifiableSet()))
                .orElse(Set.of());
    }
}
