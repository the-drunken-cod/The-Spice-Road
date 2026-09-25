package com.drunkencod.spice_road.datagen;

import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

import com.drunkencod.spice_road.Constants;
import com.drunkencod.spice_road.block.SpicePlantBlock;
import com.drunkencod.spice_road.block.SpicePlants;

import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.minecraft.data.loot.BlockLootSubProvider;
import net.minecraft.data.loot.LootTableProvider;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;

/**
 * Datagens loot tables for every registered Spice Plant block (see
 * {@link SpicePlants}), driven entirely by each block's own growth-stage
 * configuration - see {@link SpicePlantLootTables} for the shared table
 * shape and {@code FabricSpiceLootProvider} for the Fabric counterpart.
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
                        block.getMaxAge(),
                        plant.seedItem().get(),
                        plant.productItem().get(),
                        Constants.DEFAULT_SPICE_PLANT_HARVEST_YIELD));
            });
        }

        @Override
        protected Iterable<Block> getKnownBlocks() {
            return SpicePlants.getRegistered().values().stream()
                    .map(plant -> (Block) plant.block().get())
                    .toList();
        }
    }
}
