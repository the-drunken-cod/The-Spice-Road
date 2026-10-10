package com.drunkencod.spice_road.stats;

import java.util.EnumSet;
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
import com.drunkencod.spice_road.spice.Spice;

/**
 * The world's record of which Spices each player has found, saved with the
 * world (in the overworld's data storage, like {@code CellDiscoveries}). The
 * {@code spices_found} stat only counts them; a counter can't tell whether a
 * Spice is new, so this set decides that.
 */
public final class SpiceFindings extends SavedData {

    private static final String NAME = Constants.MOD_ID + "_spice_findings";

    private static final SavedData.Factory<SpiceFindings> FACTORY = new SavedData.Factory<>(SpiceFindings::new,
            SpiceFindings::load, DataFixTypes.LEVEL);

    private final Map<UUID, Set<Spice>> found = new HashMap<>();

    private record PlayerFindings(UUID player, List<String> spices) {

        private static final Codec<PlayerFindings> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                UUIDUtil.CODEC.fieldOf("player").forGetter(PlayerFindings::player),
                Codec.STRING.listOf().fieldOf("spices").forGetter(PlayerFindings::spices))
                .apply(instance, PlayerFindings::new));
    }

    /**
     * @param server The server.
     * @return The world's findings, created empty if there are none yet.
     */
    public static SpiceFindings of(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(FACTORY, NAME);
    }

    private static SpiceFindings load(CompoundTag tag, HolderLookup.Provider registries) {
        SpiceFindings findings = new SpiceFindings();
        for (Tag saved : tag.getList("players", Tag.TAG_COMPOUND)) {
            PlayerFindings.CODEC.parse(NbtOps.INSTANCE, saved).resultOrPartial(
                    error -> Constants.LOG.error("Couldn't load a player's found Spices: {}", error))
                    .ifPresent(player -> {
                        for (String id : player.spices()) {
                            Spice spice = Spice.byId(id);
                            if (spice != null)
                                findings.found.computeIfAbsent(player.player(), uuid -> EnumSet.noneOf(Spice.class))
                                        .add(spice);
                        }
                    });
        }
        return findings;
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        ListTag players = new ListTag();
        found.forEach((player, spices) -> {
            if (!spices.isEmpty())
                players.add(PlayerFindings.CODEC.encodeStart(NbtOps.INSTANCE,
                        new PlayerFindings(player, spices.stream().map(Spice::getId).sorted().toList()))
                        .getOrThrow());
        });
        tag.put("players", players);
        return tag;
    }

    /**
     * Remembers that a player found a Spice.
     *
     * @param player A player's UUID.
     * @param spice  The Spice.
     * @return Whether it was new to that player.
     */
    public boolean add(UUID player, Spice spice) {
        boolean added = found.computeIfAbsent(player, uuid -> EnumSet.noneOf(Spice.class)).add(spice);
        if (added)
            setDirty();
        return added;
    }

    /**
     * @param player A player's UUID.
     * @return The Spices that player has found; a copy.
     */
    public Set<Spice> spicesOf(UUID player) {
        Set<Spice> spices = found.get(player);
        return spices == null ? EnumSet.noneOf(Spice.class) : EnumSet.copyOf(spices);
    }

    /**
     * @param player A player's UUID.
     * @return How many different Spices that player has found.
     */
    public int count(UUID player) {
        Set<Spice> spices = found.get(player);
        return spices == null ? 0 : spices.size();
    }
}
