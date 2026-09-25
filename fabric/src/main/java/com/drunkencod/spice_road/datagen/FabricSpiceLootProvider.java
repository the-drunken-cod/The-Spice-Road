package com.drunkencod.spice_road.datagen;

import java.util.concurrent.CompletableFuture;

import com.drunkencod.spice_road.Constants;
import com.drunkencod.spice_road.block.SpicePlantBlock;
import com.drunkencod.spice_road.block.SpicePlants;

import net.fabricmc.fabric.api.datagen.v1.FabricDataOutput;
import net.fabricmc.fabric.api.datagen.v1.provider.FabricBlockLootTableProvider;
import net.minecraft.core.HolderLookup;

/**
 * Fabric counterpart of {@code NeoForgeSpiceLootProvider} - see that class
 * and {@link SpicePlantLootTables} for the shared loot table shape. Also
 * uses {@link Constants#DEFAULT_SPICE_PLANT_GROWTH_STAGES} instead of
 * {@code block.getMaxAge()} for the same reason (config isn't loaded during
 * {@code runData}).
 */
public class FabricSpiceLootProvider extends FabricBlockLootTableProvider {

    public FabricSpiceLootProvider(FabricDataOutput output, CompletableFuture<HolderLookup.Provider> registryLookup) {
        super(output, registryLookup);
    }

    @Override
    public void generate() {
        SpicePlants.getRegistered().forEach((spice, plant) -> {
            SpicePlantBlock block = plant.block().get();
            this.add(block, SpicePlantLootTables.create(
                    block,
                    block.getAgeProperty(),
                    Constants.DEFAULT_SPICE_PLANT_GROWTH_STAGES,
                    plant.seedItem().get(),
                    plant.productItem().get(),
                    Constants.DEFAULT_SPICE_PLANT_HARVEST_YIELD));
        });
    }
}
