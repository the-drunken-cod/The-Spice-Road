package com.drunkencod.spice_road;

import com.drunkencod.spice_road.datagen.FabricItemModelProvider;
import com.drunkencod.spice_road.datagen.FabricSpiceCropModelProvider;
import com.drunkencod.spice_road.datagen.FabricSpiceLootProvider;
import com.drunkencod.spice_road.datagen.FabricSpiceTreeModelProvider;
import com.drunkencod.spice_road.datagen.FabricSpiceVineModelProvider;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import com.drunkencod.spice_road.datagen.BoardLayoutProvider;
import com.drunkencod.spice_road.datagen.BotanyPotsRecipeProvider;
import com.drunkencod.spice_road.datagen.SeasoningEffectProvider;
import com.drunkencod.spice_road.datagen.SpiceBlockTagProvider;
import com.drunkencod.spice_road.datagen.SpiceItemTagProvider;
import com.drunkencod.spice_road.datagen.SpiceRoadAdvancements;
import com.drunkencod.spice_road.datagen.SpiceTreeCompatRecipeProvider;
import com.drunkencod.spice_road.datagen.SpiceTreePlanksRecipeProvider;

import net.fabricmc.fabric.api.datagen.v1.DataGeneratorEntrypoint;
import net.fabricmc.fabric.api.datagen.v1.FabricDataOutput;
import net.fabricmc.fabric.api.datagen.v1.FabricDataGenerator;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.advancements.AdvancementProvider;

/**
 * Fabric datagen entry point, registering all data providers.
 */
public class ModDataGen implements DataGeneratorEntrypoint {

    @Override
    public void onInitializeDataGenerator(FabricDataGenerator generator) {
        FabricDataGenerator.Pack pack = generator.createPack();
        pack.addProvider(FabricItemModelProvider::new);
        pack.addProvider(FabricSpiceCropModelProvider::new);
        pack.addProvider(FabricSpiceTreeModelProvider::new);
        pack.addProvider(FabricSpiceVineModelProvider::new);
        pack.addProvider(FabricSpiceLootProvider::new);
        pack.addProvider((FabricDataOutput output) -> new SpiceTreeCompatRecipeProvider(output));
        pack.addProvider((FabricDataOutput output) -> new BotanyPotsRecipeProvider(output));
        pack.addProvider((FabricDataOutput output) -> new SpiceTreePlanksRecipeProvider(output));
        pack.addProvider((FabricDataOutput output) -> new SpiceItemTagProvider(output));
        pack.addProvider((FabricDataOutput output) -> new SpiceBlockTagProvider(output));
        pack.addProvider((FabricDataOutput output) -> new SeasoningEffectProvider(output));
        pack.addProvider((FabricDataOutput output) -> new BoardLayoutProvider(output));
        pack.addProvider((FabricDataOutput output, CompletableFuture<HolderLookup.Provider> registries) -> new AdvancementProvider(
                output, registries, List.of(new SpiceRoadAdvancements())));
    }
}
