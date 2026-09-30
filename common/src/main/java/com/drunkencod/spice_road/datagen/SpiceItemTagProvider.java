package com.drunkencod.spice_road.datagen;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Stream;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.data.CachedOutput;
import net.minecraft.data.PackOutput;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;

import com.drunkencod.spice_road.block.SpicePlants;
import com.drunkencod.spice_road.block.SpiceTree;
import com.drunkencod.spice_road.block.SpiceTrees;
import com.drunkencod.spice_road.block.SpiceVines;
import com.drunkencod.spice_road.item.SpiceItemTags;
import com.drunkencod.spice_road.spice.ProcessedSpice;
import com.drunkencod.spice_road.spice.Spice;
import com.drunkencod.spice_road.spice.Tier;

/**
 * Datagens the item tags from the {@link Spice} and {@link ProcessedSpice}
 * enums and what they registered:
 * <ul>
 * <li>{@link SpiceItemTags#RAW_SPICES}, {@link SpiceItemTags#PROCESSED_SPICES}
 * and {@link SpiceItemTags#SPICES} including both.</li>
 * <li>Each {@link Tier#getItemTag() tier tag}, from its Spices' raw,
 * processed, seeds, sapling and vine items.</li>
 * <li>{@link SpiceItemTags#SPICE_PLANT_SEEDS} and the Spice Tree log, leaves
 * and sapling tags, plus {@code c:stripped_logs}.</li>
 * <li>The (initially empty) {@link SpiceItemTags#RETAINS_FLAVOR} and
 * {@link SpiceItemTags#UNSEASONABLE} tags so they exist for datapacks to add
 * to, and {@link SpiceItemTags#VOIDS_FLAVOR_WHEN_PLACED} with its one known
 * default member.</li>
 * </ul>
 */
public class SpiceItemTagProvider extends RawTagProvider<Item> {

    public SpiceItemTagProvider(PackOutput output) {
        super(output, "tags/item", BuiltInRegistries.ITEM);
    }

    @Override
    public CompletableFuture<?> run(CachedOutput cachedOutput) {
        List<CompletableFuture<?>> futures = new ArrayList<>();
        // #region spices
        futures.add(save(cachedOutput, SpiceItemTags.RAW_SPICES,
                entries(Arrays.stream(Spice.values()).map(spice -> Spice.getRawById(spice.getId())))));
        futures.add(save(cachedOutput, SpiceItemTags.PROCESSED_SPICES,
                entries(Arrays.stream(ProcessedSpice.values()).map(ProcessedSpice::getItem))));
        futures.add(save(cachedOutput, SpiceItemTags.SPICES, List.of(
                TagValue.tag(SpiceItemTags.RAW_SPICES),
                TagValue.tag(SpiceItemTags.PROCESSED_SPICES))));
        for (Tier tier : Tier.values())
            futures.add(save(cachedOutput, tier.getItemTag(), entries(tierItems(tier))));

        // #region plants and trees
        futures.add(save(cachedOutput, SpiceItemTags.SPICE_PLANT_SEEDS,
                entries(SpicePlants.getRegistered().values().stream().map(plant -> plant.seedItem().get()))));
        futures.add(save(cachedOutput, SpiceItemTags.SPICE_TREE_LOGS, entries(SpiceTrees.getRegistered().values()
                .stream().flatMap(tree -> Stream.of(tree.getLog().get(), tree.getStrippedLog().get()))
                .map(Block::asItem))));
        futures.add(save(cachedOutput, SpiceItemTags.SPICE_TREE_LEAVES, entries(SpiceTrees.getRegistered().values()
                .stream().map(tree -> tree.getLeaves().get().asItem()))));
        futures.add(save(cachedOutput, SpiceItemTags.SPICE_TREE_SAPLINGS, entries(SpiceTrees.getRegistered()
                .values().stream().map(tree -> tree.getSaplingItem().get()))));
        futures.add(save(cachedOutput, ResourceLocation.fromNamespaceAndPath("c", "stripped_logs"),
                entries(SpiceTrees.getRegistered().values().stream()
                        .map(tree -> tree.getStrippedLog().get().asItem()))));

        // #region flavor
        futures.add(save(cachedOutput, SpiceItemTags.RETAINS_FLAVOR, List.of(
                TagValue.of("#c:mushrooms"),
                TagValue.optional("#c:crops/grain"))));
        futures.add(save(cachedOutput, SpiceItemTags.UNSEASONABLE, List.of()));
        futures.add(save(cachedOutput, SpiceItemTags.VOIDS_FLAVOR_WHEN_PLACED, List.of(
                TagValue.of("minecraft:pumpkin_pie"))));
        return CompletableFuture.allOf(futures.toArray(CompletableFuture[]::new));
    }

    /**
     * @param tier The tier to collect items for.
     * @return The raw, processed, seeds, sapling and vine items of every Spice
     *         of {@code tier}, in enum order, with {@code null}s for missing
     *         ones.
     */
    private static Stream<Item> tierItems(Tier tier) {
        return Arrays.stream(Spice.values())
                .filter(spice -> spice.getTier() == tier)
                .flatMap(spice -> {
                    SpiceTree tree = SpiceTrees.getRegistered().get(spice);
                    SpiceVines.RegisteredSpiceVine vine = SpiceVines.getRegistered().get(spice);
                    Stream<Item> processed = ProcessedSpice.bySource(spice).stream().map(ProcessedSpice::getItem);
                    return Stream.concat(Stream.of(Spice.getRawById(spice.getId())), Stream.concat(processed,
                            Stream.of(Spice.getSeedsById(spice.getId()),
                                    tree != null ? tree.getSaplingItem().get() : null,
                                    vine != null ? vine.vineItem().get() : null)));
                });
    }

    @Override
    public String getName() {
        return "Spice Road item tags";
    }
}
