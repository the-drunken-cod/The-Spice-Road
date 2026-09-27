package com.drunkencod.spice_road;

import com.drunkencod.spice_road.block.SpiceTrees;
import com.drunkencod.spice_road.config.FabricConfigHelper;
import com.drunkencod.spice_road.platform.Services;
import com.drunkencod.spice_road.spice.SpiceProfileRegistry;
import com.drunkencod.spice_road.spice.SpiceProfileSync;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.registry.StrippableBlockRegistry;
import net.fabricmc.fabric.api.biome.v1.BiomeModifications;
import net.fabricmc.fabric.api.biome.v1.BiomeSelectors;
import net.fabricmc.fabric.api.event.lifecycle.v1.CommonLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.levelgen.GenerationStep;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;

/**
 * Fabric main entry point.
 */
public class SpiceRoadMod implements ModInitializer {

    @Override
    public void onInitialize() {
        // Register Cloth Config configs
        ((FabricConfigHelper) Services.CONFIG).register();

        SpiceRoad.init();
        SpiceRoad.commonSetup();

        registerSpiceProfileSync();

        // NeoForge gets this mapping from the datagenned neoforge:strippables data map.
        SpiceTrees.getRegistered().values().forEach(
                tree -> StrippableBlockRegistry.register(tree.getLog().get(), tree.getStrippedLog().get()));

        // NeoForge does the equivalent via the data-driven biome_modifier JSON
        // (neoforge/src/main/resources/data/spice_road/neoforge/biome_modifier/);
        // Fabric has no JSON-based equivalent, so this is done in code instead.
        ResourceKey<PlacedFeature> spicePlant = ResourceKey.create(Registries.PLACED_FEATURE,
                ResourceLocation.fromNamespaceAndPath(Constants.MOD_ID, "spice_plant"));
        BiomeModifications.addFeature(BiomeSelectors.foundInOverworld(),
                GenerationStep.Decoration.VEGETAL_DECORATION, spicePlant);
    }

    /**
     * Resolves Default Profiles once the server's item tags are bound, and
     * syncs them to each player on join and to everyone after
     * {@code /reload}. NeoForge does the same through its own events.
     */
    private static void registerSpiceProfileSync() {
        PayloadTypeRegistry.playS2C().register(SpiceProfileSync.TYPE, SpiceProfileSync.STREAM_CODEC);
        CommonLifecycleEvents.TAGS_LOADED.register((registries, client) -> {
            if (!client)
                SpiceProfileRegistry.resolve();
        });
        ServerLifecycleEvents.SYNC_DATA_PACK_CONTENTS
                .register((player, joined) -> ServerPlayNetworking.send(player, SpiceProfileSync.current()));
    }
}
