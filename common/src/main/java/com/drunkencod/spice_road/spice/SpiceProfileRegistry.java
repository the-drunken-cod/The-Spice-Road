package com.drunkencod.spice_road.spice;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import com.mojang.datafixers.util.Either;

import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;

import com.drunkencod.spice_road.Constants;
import com.drunkencod.spice_road.item.SpiceItemTags;

/**
 * Holds the Default Profile of every Spice Item, keyed by {@link Item}.
 * <p>
 * On the logical server, {@link SpiceProfileReloadListener} stores the
 * unresolved datapack entries via {@link #setPending}, which
 * {@link #resolve()} flattens into the item map once item tags are bound
 * (tags are bound after reload listeners run). Clients receive the resolved
 * map through {@link SpiceProfileSync} instead of resolving it themselves.
 * <p>
 * Precedence when several entries target the same item: a direct item ID
 * beats a tag, and among targets of the same kind the entry whose file ID
 * sorts last wins (a datapack overriding the same file ID replaces it
 * entirely, as usual).
 * <p>
 * Use {@link SpiceProfiles#get(net.minecraft.world.item.ItemStack)} rather
 * than this class directly - it also accounts for Profile Overrides.
 */
public final class SpiceProfileRegistry {

    private static volatile Map<Item, SpiceProfile> PROFILES = Map.of();

    private static volatile List<Map.Entry<ResourceLocation, SpiceProfileEntry>> PENDING = List.of();

    private SpiceProfileRegistry() {
    }

    /**
     * Stores freshly loaded datapack entries until {@link #resolve()} runs.
     *
     * @param entries Entries keyed by their file ID.
     */
    static void setPending(Map<ResourceLocation, SpiceProfileEntry> entries) {
        List<Map.Entry<ResourceLocation, SpiceProfileEntry>> sorted = new ArrayList<>(entries.entrySet());
        sorted.sort(Map.Entry.comparingByKey());
        PENDING = List.copyOf(sorted);
    }

    /**
     * Flattens the pending datapack entries into the item map, applying the
     * precedence rules, and logs every Spice Item missing from
     * {@link SpiceItemTags#SPICES} and vice versa. Must run on the logical
     * server after item tags are bound.
     *
     * @return The resolved map, to be synced to clients.
     */
    public static Map<Item, SpiceProfile> resolve() {
        Map<Item, SpiceProfile> fromTags = new HashMap<>();
        Map<Item, SpiceProfile> direct = new HashMap<>();
        for (Map.Entry<ResourceLocation, SpiceProfileEntry> file : PENDING) {
            SpiceProfile profile = file.getValue().profile();
            for (Either<TagKey<Item>, ResourceLocation> target : file.getValue().targets()) {
                target.ifLeft(tag -> BuiltInRegistries.ITEM.getTag(tag).ifPresentOrElse(
                        set -> set.forEach(holder -> fromTags.put(holder.value(), profile)),
                        () -> Constants.LOG.debug("Spice Profile {} targets unknown item tag #{}", file.getKey(),
                                tag.location())));
                target.ifRight(id -> BuiltInRegistries.ITEM.getOptional(id).ifPresentOrElse(
                        item -> direct.put(item, profile),
                        () -> Constants.LOG.debug("Spice Profile {} targets unknown item {}", file.getKey(), id)));
            }
        }
        Map<Item, SpiceProfile> resolved = new HashMap<>(fromTags);
        resolved.putAll(direct);
        PROFILES = Map.copyOf(resolved);
        logTagMismatches();
        return PROFILES;
    }

    /**
     * Replaces the item map with the one synced from the server.
     *
     * @param profiles The server's resolved map.
     */
    public static void acceptSync(Map<Item, SpiceProfile> profiles) {
        PROFILES = Map.copyOf(profiles);
    }

    /**
     * @param item The item to look up.
     * @return {@code item}'s Default Profile, if it's a Spice Item.
     */
    public static Optional<SpiceProfile> getDefault(Item item) {
        return Optional.ofNullable(PROFILES.get(item));
    }

    /** @return Every Spice Item's Default Profile. */
    public static Map<Item, SpiceProfile> getAll() {
        return PROFILES;
    }

    private static void logTagMismatches() {
        Set<Item> tagged = new HashSet<>();
        BuiltInRegistries.ITEM.getTag(SpiceItemTags.SPICES)
                .ifPresent(set -> set.stream().map(Holder::value).forEach(tagged::add));
        List<ResourceLocation> untagged = PROFILES.keySet().stream()
                .filter(item -> !tagged.contains(item))
                .map(BuiltInRegistries.ITEM::getKey)
                .sorted()
                .toList();
        List<ResourceLocation> unprofiled = tagged.stream()
                .filter(item -> !PROFILES.containsKey(item))
                .map(BuiltInRegistries.ITEM::getKey)
                .sorted()
                .toList();
        if (!untagged.isEmpty())
            Constants.LOG.warn("Items with a Spice Profile but missing from #{}: {}", SpiceItemTags.SPICES.location(),
                    untagged);
        if (!unprofiled.isEmpty())
            Constants.LOG.warn("Items in #{} without a Spice Profile (they won't act as spices): {}",
                    SpiceItemTags.SPICES.location(), unprofiled);
    }
}
