package com.drunkencod.spice_road.datagen;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import com.drunkencod.spice_road.Constants;

import net.fabricmc.fabric.api.datagen.v1.FabricDataOutput;
import net.minecraft.data.CachedOutput;
import net.minecraft.data.DataProvider;
import net.minecraft.data.PackOutput;
import net.minecraft.data.models.model.ModelTemplates;
import net.minecraft.data.models.model.TextureMapping;
import net.minecraft.resources.ResourceLocation;

/**
 * Datagens flat {@code minecraft:item/generated} item models for every item
 * queued via {@link ItemModelHelper#addFlatItem}. See
 * {@code NeoForgeItemModelProvider} for the NeoForge counterpart.
 * <p>
 * Implemented as a standalone {@link DataProvider}, since
 * {@code ItemModelGenerators} only offers flat models textured by item ID,
 * while these textures live in per-Spice folders (see {@link SpiceAssetPaths}).
 */
public class FabricItemModelProvider implements DataProvider {

    private final PackOutput.PathProvider modelPathProvider;

    public FabricItemModelProvider(FabricDataOutput output) {
        this.modelPathProvider = output.createPathProvider(PackOutput.Target.RESOURCE_PACK, "models");
    }

    @Override
    public CompletableFuture<?> run(CachedOutput cachedOutput) {
        List<CompletableFuture<?>> futures = new ArrayList<>();
        for (Map.Entry<String, ResourceLocation> item : ItemModelHelper.getFlatItems().entrySet()) {
            ResourceLocation model = ResourceLocation.fromNamespaceAndPath(Constants.MOD_ID, "item/" + item.getKey());
            ModelTemplates.FLAT_ITEM.create(model, TextureMapping.layer0(item.getValue()),
                    (modelLocation, json) -> futures.add(DataProvider.saveStable(cachedOutput, json.get(),
                            modelPathProvider.json(modelLocation))));
        }
        return CompletableFuture.allOf(futures.toArray(CompletableFuture[]::new));
    }

    @Override
    public String getName() {
        return "Item Models";
    }
}
