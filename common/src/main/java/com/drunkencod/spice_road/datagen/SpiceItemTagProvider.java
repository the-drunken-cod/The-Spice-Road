package com.drunkencod.spice_road.datagen;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;
import java.util.stream.Stream;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.data.CachedOutput;
import net.minecraft.data.DataProvider;
import net.minecraft.data.PackOutput;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;

import com.drunkencod.spice_road.block.SpiceTree;
import com.drunkencod.spice_road.block.SpiceTrees;
import com.drunkencod.spice_road.block.SpiceVines;
import com.drunkencod.spice_road.item.SpiceItemTags;
import com.drunkencod.spice_road.spice.ProcessedSpice;
import com.drunkencod.spice_road.spice.Spice;
import com.drunkencod.spice_road.spice.Tier;

/**
 * Datagens the Spice item tags from the {@link Spice} enum:
 * {@link SpiceItemTags#RAW_SPICES} from each Spice's raw item (skipping
 * missing ones) and {@link SpiceItemTags#PROCESSED_SPICES} from the
 * {@link ProcessedSpice} enum, plus {@link SpiceItemTags#SPICES} including
 * both, and each {@link Tier#getItemTag() tier tag} from its Spices' raw,
 * processed, seeds, sapling and vine items. Also writes the (initially empty)
 * {@link SpiceItemTags#RETAINS_FLAVOR} and
 * {@link SpiceItemTags#UNSEASONABLE} tags so they exist for datapacks to
 * add to, and {@link SpiceItemTags#VOIDS_FLAVOR_WHEN_PLACED} with its one
 * known default member. Plain {@link DataProvider} writing raw JSON, so it
 * runs unchanged on both loaders.
 */
public class SpiceItemTagProvider implements DataProvider {

    private final PackOutput.PathProvider tagPathProvider;

    public SpiceItemTagProvider(PackOutput output) {
        this.tagPathProvider = output.createPathProvider(PackOutput.Target.DATA_PACK, "tags/item");
    }

    @Override
    public CompletableFuture<?> run(CachedOutput cachedOutput) {
        List<CompletableFuture<?>> futures = new ArrayList<>();
        futures.add(save(cachedOutput, SpiceItemTags.RAW_SPICES,
                spiceItems(spice -> Spice.getRawById(spice.getId())).stream().map(TagValue::of).toList()));
        futures.add(save(cachedOutput, SpiceItemTags.PROCESSED_SPICES,
                itemIds(Arrays.stream(ProcessedSpice.values()).map(ProcessedSpice::getItem)).stream()
                        .map(TagValue::of).toList()));
        futures.add(save(cachedOutput, SpiceItemTags.SPICES, List.of(
                TagValue.of("#" + SpiceItemTags.RAW_SPICES.location()),
                TagValue.of("#" + SpiceItemTags.PROCESSED_SPICES.location()))));
        futures.add(save(cachedOutput, SpiceItemTags.RETAINS_FLAVOR, List.of(
                TagValue.of("#c:mushrooms"),
                TagValue.optional("#c:crops/grain"))));
        futures.add(save(cachedOutput, SpiceItemTags.UNSEASONABLE, List.of()));
        futures.add(save(cachedOutput, SpiceItemTags.VOIDS_FLAVOR_WHEN_PLACED, List.of(
                TagValue.of("minecraft:pumpkin_pie"))));
        for (Tier tier : Tier.values())
            futures.add(save(cachedOutput, tier.getItemTag(), tierItems(tier).stream().map(TagValue::of).toList()));
        return CompletableFuture.allOf(futures.toArray(CompletableFuture[]::new));
    }

    /** One {@code values} entry of a tag: a required reference, or an {@link #optional} one that may not exist. */
    private record TagValue(String id, boolean required) {
        static TagValue of(String id) {
            return new TagValue(id, true);
        }

        /** @param id A reference tolerated as missing, e.g. another mod's convention tag that may not be loaded. */
        static TagValue optional(String id) {
            return new TagValue(id, false);
        }
    }

    /**
     * @param itemGetter Gets a Spice's item of one state, or {@code null} if
     *                   it has none.
     * @return The IDs of every Spice's item of that state, in enum order.
     */
    private static List<String> spiceItems(Function<Spice, Item> itemGetter) {
        return itemIds(Arrays.stream(Spice.values()).map(itemGetter));
    }

    /**
     * @param tier The tier to collect items for.
     * @return The IDs of the raw, processed, seeds, sapling and vine items of
     *         every Spice of {@code tier}, in enum order.
     */
    private static List<String> tierItems(Tier tier) {
        return itemIds(Arrays.stream(Spice.values())
                .filter(spice -> spice.getTier() == tier)
                .flatMap(spice -> {
                    SpiceTree tree = SpiceTrees.getRegistered().get(spice);
                    SpiceVines.RegisteredSpiceVine vine = SpiceVines.getRegistered().get(spice);
                    Stream<Item> processed = ProcessedSpice.bySource(spice).stream().map(ProcessedSpice::getItem);
                    return Stream.concat(Stream.of(Spice.getRawById(spice.getId())), Stream.concat(processed,
                            Stream.of(Spice.getSeedsById(spice.getId()),
                                    tree != null ? tree.getSaplingItem().get() : null,
                                    vine != null ? vine.vineItem().get() : null)));
                }));
    }

    /** @return The IDs of {@code items}, skipping {@code null}s, in order. */
    private static List<String> itemIds(Stream<Item> items) {
        return items.filter(Objects::nonNull)
                .map(item -> BuiltInRegistries.ITEM.getKey(item).toString())
                .toList();
    }

    private CompletableFuture<?> save(CachedOutput cachedOutput, TagKey<Item> tag, List<TagValue> values) {
        JsonObject json = new JsonObject();
        json.addProperty("replace", false);
        JsonArray valuesJson = new JsonArray();
        for (TagValue value : values) {
            if (value.required()) {
                valuesJson.add(value.id());
                continue;
            }
            JsonObject entry = new JsonObject();
            entry.addProperty("id", value.id());
            entry.addProperty("required", false);
            valuesJson.add(entry);
        }
        json.add("values", valuesJson);
        return DataProvider.saveStable(cachedOutput, json, tagPathProvider.json(tag.location()));
    }

    @Override
    public String getName() {
        return "Spice Road item tags";
    }
}
