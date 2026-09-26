package com.drunkencod.spice_road.datagen;

import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Stream;

import com.drunkencod.spice_road.Constants;
import com.drunkencod.spice_road.block.SpicePlantBlock;
import com.drunkencod.spice_road.block.SpicePlants;
import com.drunkencod.spice_road.block.SpiceTrees;

import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.minecraft.data.loot.BlockLootSubProvider;
import net.minecraft.data.loot.LootTableProvider;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;

/**
 * Datagens loot tables for every registered Spice Plant block (see
 * {@link SpicePlants}) - see {@link SpicePlantLootTables} for the shared
 * table shape and {@code FabricSpiceLootProvider} for the Fabric
 * counterpart. Spice Tree blocks (see {@link SpiceTrees}) use vanilla's
 * log/sapling self-drops and leaves drops (sapling chance, sticks, and the
 * leaves themselves with shears/Silk Touch).
 * <p>
 * Uses {@link Constants#DEFAULT_SPICE_PLANT_GROWTH_STAGES} rather than
 * {@code block.getMaxAge()} for the loot condition's threshold age: a pure
 * {@code runData} pass never loads config (see
 * {@link com.drunkencod.spice_road.block.SpicePlantBlock#getMaxAge()}), so
 * calling {@code getMaxAge()} here would crash datagen the same way reading
 * config during block registration did. Re-run datagen after changing that
 * default to regenerate the loot tables.
 */
public class NeoForgeSpiceLootProvider extends LootTableProvider {

    public NeoForgeSpiceLootProvider(PackOutput output, CompletableFuture<HolderLookup.Provider> registries) {
        super(output, Set.of(), List.of(
                new LootTableProvider.SubProviderEntry(SpicePlantBlockLoot::new, LootContextParamSets.BLOCK)),
                registries);
    }

    private static class SpicePlantBlockLoot extends BlockLootSubProvider {

        protected SpicePlantBlockLoot(HolderLookup.Provider registries) {
            super(Set.of(), FeatureFlags.REGISTRY.allFlags(), registries);
        }

        @Override
        protected void generate() {
            SpicePlants.getRegistered().forEach((spice, plant) -> {
                SpicePlantBlock block = plant.block().get();
                this.add(block, SpicePlantLootTables.create(
                        block,
                        block.getAgeProperty(),
                        Constants.DEFAULT_SPICE_PLANT_GROWTH_STAGES,
                        plant.seedItem().get(),
                        plant.productItem().get(),
                        (int) Math.floor(
                                spice.getDropAmount() * Constants.DEFAULT_SPICE_PLANT_HARVEST_YIELD_MULTIPLIER)));
            });

            SpiceTrees.getRegistered().values().forEach(tree -> {
                dropSelf(tree.getLog().get());
                dropSelf(tree.getStrippedLog().get());
                dropSelf(tree.getSapling().get());
                add(tree.getLeaves().get(), createLeavesDrops(tree.getLeaves().get(), tree.getSapling().get(),
                        NORMAL_LEAVES_SAPLING_CHANCES));
            });
        }

        @Override
        protected Iterable<Block> getKnownBlocks() {
            return Stream.concat(
                    SpicePlants.getRegistered().values().stream().map(plant -> (Block) plant.block().get()),
                    SpiceTrees.getRegistered().values().stream().flatMap(tree -> tree.getBlocks().stream()))
                    .toList();
        }
    }
}
