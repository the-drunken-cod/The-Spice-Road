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
import com.drunkencod.spice_road.spice.Spice;
import com.drunkencod.spice_road.spice.Tier;

/**
 * Datagens the Spice item tags from the {@link Spice} enum:
 * {@link SpiceItemTags#RAW_SPICES} and {@link SpiceItemTags#DRIED_SPICES}
 * from each Spice's raw/dried item (skipping missing ones), plus
 * {@link SpiceItemTags#SPICES} including both, and each
 * {@link Tier#getItemTag() tier tag} from its Spices' raw, dried, seeds,
 * sapling and vine items. Also writes the (initially empty)
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
        futures.add(save(cachedOutput, SpiceItemTags.RAW_SPICES, spiceItems(spice -> Spice.getRawById(spice.getId()))));
        futures.add(save(cachedOutput, SpiceItemTags.DRIED_SPICES,
                spiceItems(spice -> Spice.getDriedById(spice.getId()))));
        futures.add(save(cachedOutput, SpiceItemTags.SPICES, List.of(
                "#" + SpiceItemTags.RAW_SPICES.location(),
                "#" + SpiceItemTags.DRIED_SPICES.location())));
        futures.add(save(cachedOutput, SpiceItemTags.RETAINS_FLAVOR, List.of(
                "#c:mushrooms",
                "#c:crops/grain")));
        futures.add(save(cachedOutput, SpiceItemTags.UNSEASONABLE, List.of()));
        futures.add(save(cachedOutput, SpiceItemTags.VOIDS_FLAVOR_WHEN_PLACED, List.of(
                "minecraft:pumpkin_pie")));
        for (Tier tier : Tier.values())
            futures.add(save(cachedOutput, tier.getItemTag(), tierItems(tier)));
        return CompletableFuture.allOf(futures.toArray(CompletableFuture[]::new));
    }

    /**
     * @param itemGetter Gets a Spice's item of one state, or {@code null} if
     *                   it has none.
     * @return The IDs of every Spice's item of that state, in enum order.
     */
    private static List<String> spiceItems(Function<Spice, Item> itemGetter) {
        return Arrays.stream(Spice.values())
                .map(itemGetter)
                .filter(Objects::nonNull)
                .map(item -> BuiltInRegistries.ITEM.getKey(item).toString())
                .toList();
    }

    /**
     * @param tier The tier to collect items for.
     * @return The IDs of the raw, dried, seeds, sapling and vine items of every
     *         Spice of {@code tier}, in enum order.
     */
    private static List<String> tierItems(Tier tier) {
        return Arrays.stream(Spice.values())
                .filter(spice -> spice.getTier() == tier)
                .flatMap(spice -> {
                    SpiceTree tree = SpiceTrees.getRegistered().get(spice);
                    SpiceVines.RegisteredSpiceVine vine = SpiceVines.getRegistered().get(spice);
                    return Stream.of(Spice.getRawById(spice.getId()), Spice.getDriedById(spice.getId()),
                            Spice.getSeedsById(spice.getId()), tree != null ? tree.getSaplingItem().get() : null,
                            vine != null ? vine.vineItem().get() : null);
                })
                .filter(Objects::nonNull)
                .map(item -> BuiltInRegistries.ITEM.getKey(item).toString())
                .toList();
    }

    private CompletableFuture<?> save(CachedOutput cachedOutput, TagKey<Item> tag, List<String> values) {
        JsonObject json = new JsonObject();
        json.addProperty("replace", false);
        JsonArray valuesJson = new JsonArray();
        values.forEach(valuesJson::add);
        json.add("values", valuesJson);
        return DataProvider.saveStable(cachedOutput, json, tagPathProvider.json(tag.location()));
    }

    @Override
    public String getName() {
        return "Spice Road item tags";
    }
}
