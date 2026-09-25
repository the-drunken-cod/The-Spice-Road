package com.drunkencod.spice_road;

import com.drunkencod.spice_road.config.FabricConfigHelper;
import com.drunkencod.spice_road.platform.Services;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.biome.v1.BiomeModifications;
import net.fabricmc.fabric.api.biome.v1.BiomeSelectors;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.levelgen.GenerationStep;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;

public class SpiceRoadMod implements ModInitializer {

    @Override
    public void onInitialize() {
        // Register Cloth Config configs
        ((FabricConfigHelper) Services.CONFIG).register();

        SpiceRoad.init();

        // NeoForge does the equivalent via the data-driven biome_modifier JSON
        // (neoforge/src/main/resources/data/spice_road/neoforge/biome_modifier/);
        // Fabric has no JSON-based equivalent, so this is done in code instead.
        ResourceKey<PlacedFeature> spicePlant = ResourceKey.create(Registries.PLACED_FEATURE,
                ResourceLocation.fromNamespaceAndPath(Constants.MOD_ID, "spice_plant"));
        BiomeModifications.addFeature(BiomeSelectors.foundInOverworld(),
                GenerationStep.Decoration.VEGETAL_DECORATION, spicePlant);
    }
}
