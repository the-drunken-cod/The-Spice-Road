package com.drunkencod.spice_road;

import com.drunkencod.spice_road.datagen.FabricItemModelProvider;
import com.drunkencod.spice_road.datagen.FabricSpiceCropModelProvider;
import com.drunkencod.spice_road.datagen.FabricSpiceLootProvider;
import com.drunkencod.spice_road.datagen.FabricSpiceTreeModelProvider;
import com.drunkencod.spice_road.datagen.SpiceItemTagProvider;
import com.drunkencod.spice_road.datagen.SpiceTreeCompatRecipeProvider;

import net.fabricmc.fabric.api.datagen.v1.DataGeneratorEntrypoint;
import net.fabricmc.fabric.api.datagen.v1.FabricDataOutput;
import net.fabricmc.fabric.api.datagen.v1.FabricDataGenerator;

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
        pack.addProvider(FabricSpiceLootProvider::new);
        pack.addProvider((FabricDataOutput output) -> new SpiceTreeCompatRecipeProvider(output));
        pack.addProvider((FabricDataOutput output) -> new SpiceItemTagProvider(output));
    }
}
