package com.drunkencod.spice_road.stats;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.jetbrains.annotations.Nullable;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.core.HolderLookup;
import net.minecraft.core.UUIDUtil;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.datafix.DataFixTypes;
import net.minecraft.world.level.saveddata.SavedData;

import com.drunkencod.spice_road.Constants;

/**
 * The world's record of which raw Spice items each player has found, saved
 * with the world (in the overworld's data storage, like {@code CellDiscoveries}).
 * Keyed by item ID rather than by the {@code Spice} enum, so datapack-added
 * Spices are recorded too. The {@code spices_found} stat only counts them; a
 * counter can't tell whether a Spice is new, so this set decides that.
 */
public final class SpiceFindings extends SavedData {

    private static final String NAME = Constants.MOD_ID + "_spice_findings";

    private static final SavedData.Factory<SpiceFindings> FACTORY = new SavedData.Factory<>(SpiceFindings::new,
            SpiceFindings::load, DataFixTypes.LEVEL);

    private final Map<UUID, Set<ResourceLocation>> found = new HashMap<>();

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

    /**
     * @param id A saved or synced item ID. One without a namespace is a
     *           built-in Spice's bare ID, e.g. {@code "cinnamon"}, as older
     *           worlds saved them.
     * @return The item ID, or {@code null} if {@code id} isn't a valid one.
     */
    static @Nullable ResourceLocation parseId(String id) {
        return id.indexOf(ResourceLocation.NAMESPACE_SEPARATOR) < 0
                ? ResourceLocation.tryBuild(Constants.MOD_ID, id)
                : ResourceLocation.tryParse(id);
    }

    private static SpiceFindings load(CompoundTag tag, HolderLookup.Provider registries) {
        SpiceFindings findings = new SpiceFindings();
        for (Tag saved : tag.getList("players", Tag.TAG_COMPOUND)) {
            PlayerFindings.CODEC.parse(NbtOps.INSTANCE, saved).resultOrPartial(
                    error -> Constants.LOG.error("Couldn't load a player's found Spices: {}", error))
                    .ifPresent(player -> {
                        for (String id : player.spices()) {
                            ResourceLocation item = parseId(id);
                            if (item != null)
                                findings.found.computeIfAbsent(player.player(), uuid -> new HashSet<>()).add(item);
                        }
                    });
        }
        return findings;
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        ListTag players = new ListTag();
        found.forEach((player, items) -> {
            if (!items.isEmpty())
                players.add(PlayerFindings.CODEC.encodeStart(NbtOps.INSTANCE,
                        new PlayerFindings(player, items.stream().sorted().map(ResourceLocation::toString).toList()))
                        .getOrThrow());
        });
        tag.put("players", players);
        return tag;
    }

    /**
     * Remembers that a player found a Spice.
     *
     * @param player A player's UUID.
     * @param item   The ID of the Spice's raw item.
     * @return Whether it was new to that player.
     */
    public boolean add(UUID player, ResourceLocation item) {
        boolean added = found.computeIfAbsent(player, uuid -> new HashSet<>()).add(item);
        if (added)
            setDirty();
        return added;
    }

    /**
     * Forgets every Spice a player has found.
     *
     * @param player A player's UUID.
     * @return How many Spices that player had found.
     */
    public int clear(UUID player) {
        Set<ResourceLocation> items = found.remove(player);
        if (items == null || items.isEmpty())
            return 0;
        setDirty();
        return items.size();
    }

    /**
     * @param player A player's UUID.
     * @return The IDs of the Spices that player has found; a copy.
     */
    public Set<ResourceLocation> itemsOf(UUID player) {
        Set<ResourceLocation> items = found.get(player);
        return items == null ? new HashSet<>() : new HashSet<>(items);
    }

    /**
     * @param player A player's UUID.
     * @param items  The IDs of the Spices to look for.
     * @return Whether that player has found every one of them; {@code false}
     *         if {@code items} is empty.
     */
    public boolean hasFoundAll(UUID player, Collection<ResourceLocation> items) {
        Set<ResourceLocation> have = found.get(player);
        return !items.isEmpty() && have != null && have.containsAll(items);
    }

    /**
     * @param player A player's UUID.
     * @return How many different Spices that player has found.
     */
    public int count(UUID player) {
        Set<ResourceLocation> items = found.get(player);
        return items == null ? 0 : items.size();
    }
}
