package com.drunkencod.spice_road.datagen;

import java.util.concurrent.CompletableFuture;

import com.drunkencod.spice_road.Constants;
import com.drunkencod.spice_road.block.SpicePlantBlock;
import com.drunkencod.spice_road.block.SpicePlants;
import com.drunkencod.spice_road.block.SpiceTrees;
import com.drunkencod.spice_road.spice.Spice;

import net.fabricmc.fabric.api.datagen.v1.FabricDataOutput;
import net.fabricmc.fabric.api.datagen.v1.provider.FabricBlockLootTableProvider;
import net.minecraft.core.HolderLookup;

/**
 * Fabric counterpart of {@code NeoForgeSpiceLootProvider} - see that class
 * and {@link SpicePlantLootTables} for the shared loot table shape. Also
 * uses {@link Constants#DEFAULT_SPICE_PLANT_GROWTH_STAGES} instead of
 * {@code block.getMaxAge()} for the same reason (config isn't loaded during
 * {@code runData}). Spice Tree blocks use vanilla's log/sapling self-drops and
 * leaves drops, plus the Spice from ripe fruiting leaves (see
 * {@link SpiceTreeLootTables}).
 */
public class FabricSpiceLootProvider extends FabricBlockLootTableProvider {

    public FabricSpiceLootProvider(FabricDataOutput output, CompletableFuture<HolderLookup.Provider> registryLookup) {
        super(output, registryLookup);
    }

    @Override
    public void generate() {
        SpicePlants.getRegistered().forEach((spice, plant) -> {
            addSpicePlantLoot(spice, plant.block().get(), plant);
            if (plant.worldgenBlock() != plant.block())
                addSpicePlantLoot(spice, plant.worldgenBlock().get(), plant);
        });

        SpiceTrees.getRegistered().values().forEach(tree -> {
            dropSelf(tree.getLog().get());
            dropSelf(tree.getStrippedLog().get());
            dropSelf(tree.getSapling().get());
            add(tree.getLeaves().get(), SpiceTreeLootTables.withRipeLeavesHarvest(tree,
                    createLeavesDrops(tree.getLeaves().get(), tree.getSapling().get(),
                            NORMAL_LEAVES_SAPLING_CHANCES)));
        });
    }

    /**
     * Adds {@code block}'s loot table, built from {@code plant}'s seed/
     * product items and {@code spice}'s drop amount. Called once for the
     * farmed block and, for {@code CROP} Spices, again for the worldgen-only
     * {@code wild_} block, so both share the exact same loot table shape.
     */
    private void addSpicePlantLoot(Spice spice, SpicePlantBlock block, SpicePlants.RegisteredSpicePlant plant) {
        this.add(block, SpicePlantLootTables.create(
                block,
                block.getAgeProperty(),
                Constants.DEFAULT_SPICE_PLANT_GROWTH_STAGES,
                plant.seedItem().get(),
                plant.productItem().get(),
                (int) Math.floor(spice.getDropAmount() * Constants.DEFAULT_SPICE_PLANT_HARVEST_YIELD_MULTIPLIER)));
    }
}
