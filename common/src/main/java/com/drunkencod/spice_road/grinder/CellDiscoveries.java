package com.drunkencod.spice_road.grinder;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.core.HolderLookup;
import net.minecraft.core.UUIDUtil;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.datafix.DataFixTypes;
import net.minecraft.world.level.saveddata.SavedData;

import com.drunkencod.spice_road.Constants;
import com.drunkencod.spice_road.grinder.DiscoveryLog.Entry;

/**
 * The world's record of which effect cells each player has locked in, saved
 * with the world (in the overworld's data storage, like any vanilla saved
 * data, so it behaves the same on both loaders). Only the server keeps it: the
 * client learns what a player knows through the view of the board it is sent.
 */
public final class CellDiscoveries extends SavedData {

    private static final String NAME = Constants.MOD_ID + "_cell_discoveries";

    private static final SavedData.Factory<CellDiscoveries> FACTORY = new SavedData.Factory<>(CellDiscoveries::new,
            CellDiscoveries::load, DataFixTypes.LEVEL);

    private final Map<UUID, DiscoveryLog> logs = new HashMap<>();

    private record PlayerLog(UUID player, List<Entry> entries) {

        private static final Codec<PlayerLog> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                UUIDUtil.CODEC.fieldOf("player").forGetter(PlayerLog::player),
                Entry.CODEC.listOf().fieldOf("cells").forGetter(PlayerLog::entries))
                .apply(instance, PlayerLog::new));
    }

    /**
     * @param server The server.
     * @return The world's discoveries, created empty if there are none yet.
     */
    public static CellDiscoveries of(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(FACTORY, NAME);
    }

    private static CellDiscoveries load(CompoundTag tag, HolderLookup.Provider registries) {
        CellDiscoveries discoveries = new CellDiscoveries();
        for (Tag saved : tag.getList("players", Tag.TAG_COMPOUND)) {
            PlayerLog.CODEC.parse(NbtOps.INSTANCE, saved).resultOrPartial(
                    error -> Constants.LOG.error("Couldn't load a player's discovered cells: {}", error))
                    .ifPresent(log -> discoveries.logs.put(log.player(), DiscoveryLog.of(log.entries())));
        }
        return discoveries;
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        ListTag players = new ListTag();
        logs.forEach((player, log) -> {
            if (!log.isEmpty())
                players.add(PlayerLog.CODEC.encodeStart(NbtOps.INSTANCE, new PlayerLog(player, log.entries()))
                        .getOrThrow());
        });
        tag.put("players", players);
        return tag;
    }

    /**
     * @param player A player's UUID.
     * @param seed   The seed of a board.
     * @param layout A fingerprint of its layout.
     * @return The indices of the cells of that board the player knows.
     */
    public Set<Integer> revealed(UUID player, long seed, int layout) {
        DiscoveryLog log = logs.get(player);
        return log == null ? Set.of() : log.revealed(seed, layout);
    }

    /**
     * Remembers that a player locked a cell in.
     *
     * @param player A player's UUID.
     * @param entry  The cell.
     * @param limit  The most cells to remember for that player.
     */
    public void record(UUID player, Entry entry, int limit) {
        logs.computeIfAbsent(player, id -> new DiscoveryLog()).record(entry, limit);
        setDirty();
    }
}
