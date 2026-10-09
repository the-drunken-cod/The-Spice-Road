package com.drunkencod.spice_road.datagen;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;
import java.util.stream.Stream;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.data.CachedOutput;
import net.minecraft.data.PackOutput;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.block.Block;

import com.drunkencod.spice_road.block.FruitingSpiceLeavesBlock;
import com.drunkencod.spice_road.block.SpiceBlockTags;
import com.drunkencod.spice_road.block.SpicePlants;
import com.drunkencod.spice_road.block.SpiceTree;
import com.drunkencod.spice_road.block.SpiceTrees;
import com.drunkencod.spice_road.block.SpiceVines;
import com.drunkencod.spice_road.registry.ModBlocks;
import com.drunkencod.spice_road.spice.Season;
import com.drunkencod.spice_road.spice.Spice;

/**
 * Datagens every block tag that lists the mod's blocks by ID, from the
 * registered Spice Plants, Trees and Vines:
 * <ul>
 * <li>The {@link SpiceBlockTags} Spice crop, tree, vine and rack tags, Drying Racks included.</li>
 * <li>{@code c:stripped_logs} and {@code minecraft:overworld_natural_logs}.</li>
 * <li>{@code minecraft:flowers}: Spice crops, vines and fruiting leaves, so
 * bees pollinate them.</li>
 * <li>Serene Seasons' {@code <season>_crops}, from each Spice's
 * {@link Spice#getSeasons() seasons}.</li>
 * </ul>
 * Tags that only reference other tags or vanilla blocks stay hand-written.
 */
public class SpiceBlockTagProvider extends RawTagProvider<Block> {

    /** Namespace of Serene Seasons' crop season tags. */
    private static final String SERENE_SEASONS = "sereneseasons";

    public SpiceBlockTagProvider(PackOutput output) {
        super(output, "tags/block", BuiltInRegistries.BLOCK);
    }

    @Override
    public CompletableFuture<?> run(CachedOutput cachedOutput) {
        List<CompletableFuture<?>> futures = new ArrayList<>();
        List<SpiceTree> trees = List.copyOf(SpiceTrees.getRegistered().values());

        // #region spice_road
        futures.add(save(cachedOutput, SpiceBlockTags.SPICE_CROPS, entries(SpicePlants.getAllBlocks().stream())));
        futures.add(save(cachedOutput, SpiceBlockTags.SPICE_TREE_LOGS, entries(trees.stream()
                .flatMap(tree -> Stream.of(tree.getLog().get(), tree.getStrippedLog().get())))));
        futures.add(save(cachedOutput, SpiceBlockTags.SPICE_TREE_LEAVES,
                entries(trees.stream().map(tree -> tree.getLeaves().get()))));
        futures.add(save(cachedOutput, SpiceBlockTags.SPICE_TREE_SAPLINGS,
                entries(trees.stream().map(tree -> tree.getSapling().get()))));
        futures.add(save(cachedOutput, SpiceBlockTags.SPICE_VINES,
                entries(SpiceVines.getRegistered().values().stream().map(vine -> vine.block().get()))));
        futures.add(save(cachedOutput, SpiceBlockTags.SPICE_RACKS,
                entries(ModBlocks.SPICE_RACKS.values().stream().map(Supplier::get))));
        futures.add(save(cachedOutput, SpiceBlockTags.DRYING_RACKS,
                entries(ModBlocks.DRYING_RACKS.values().stream().map(Supplier::get))));

        // #region vanilla and conventional
        futures.add(save(cachedOutput, ResourceLocation.fromNamespaceAndPath("c", "stripped_logs"),
                entries(trees.stream().map(tree -> tree.getStrippedLog().get()))));
        futures.add(save(cachedOutput, BlockTags.OVERWORLD_NATURAL_LOGS,
                entries(trees.stream().map(tree -> tree.getLog().get()))));
        List<TagValue> flowers = new ArrayList<>(List.of(
                TagValue.tag(SpiceBlockTags.SPICE_CROPS),
                TagValue.tag(SpiceBlockTags.SPICE_VINES)));
        flowers.addAll(entries(trees.stream().map(tree -> tree.getLeaves().get())
                .filter(FruitingSpiceLeavesBlock.class::isInstance)));
        futures.add(save(cachedOutput, BlockTags.FLOWERS, flowers));

        // #region compat
        for (Season season : Season.values()) {
            futures.add(save(cachedOutput,
                    ResourceLocation.fromNamespaceAndPath(SERENE_SEASONS, season.getId() + "_crops"),
                    entries(seasonalBlocks(season))));
        }
        return CompletableFuture.allOf(futures.toArray(CompletableFuture[]::new));
    }

    /**
     * @param season A season.
     * @return The blocks that grow a Spice of {@code season}: its plant
     *         (including the wild variant), sapling or vine, in enum order.
     */
    private static Stream<Block> seasonalBlocks(Season season) {
        return Stream.of(Spice.values())
                .filter(spice -> spice.getSeasons().contains(season))
                .flatMap(spice -> {
                    SpicePlants.RegisteredSpicePlant plant = SpicePlants.getRegistered().get(spice);
                    SpiceTree tree = SpiceTrees.getRegistered().get(spice);
                    SpiceVines.RegisteredSpiceVine vine = SpiceVines.getRegistered().get(spice);
                    Stream<Block> plants = plant == null ? Stream.empty()
                            : Stream.<Block>of(plant.block().get(), plant.worldgenBlock().get()).distinct();
                    return Stream.concat(plants, Stream.of(
                            tree != null ? tree.getSapling().get() : null,
                            vine != null ? vine.block().get() : null));
                });
    }

    @Override
    public String getName() {
        return "Spice Road block tags";
    }
}
