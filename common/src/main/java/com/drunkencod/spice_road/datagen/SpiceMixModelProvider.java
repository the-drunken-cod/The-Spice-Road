package com.drunkencod.spice_road.datagen;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import net.minecraft.data.CachedOutput;
import net.minecraft.data.DataProvider;
import net.minecraft.data.PackOutput;
import net.minecraft.resources.ResourceLocation;

import com.drunkencod.spice_road.Constants;

/**
 * Writes the Spice Mix item model: a flat model of the custom mix, with one
 * {@code custom_model_data} override per {@link DefaultMixPreset} pointing at
 * that preset's own flat model. Raw JSON, so it runs unchanged on both
 * loaders. Textures live in {@code textures/item/spice_mix/}.
 */
public class SpiceMixModelProvider implements DataProvider {

    /** Folder below {@code item/} holding the mix textures and preset models. */
    private static final String FOLDER = "item/spice_mix/";

    private final PackOutput.PathProvider pathProvider;

    /** @param output The pack output to write into. */
    public SpiceMixModelProvider(PackOutput output) {
        this.pathProvider = output.createPathProvider(PackOutput.Target.RESOURCE_PACK, "models");
    }

    @Override
    public CompletableFuture<?> run(CachedOutput cachedOutput) {
        List<CompletableFuture<?>> writes = new ArrayList<>();
        JsonObject mix = flat(location(FOLDER + "spice_mix"));
        JsonArray overrides = new JsonArray();
        for (DefaultMixPreset preset : DefaultMixPreset.values()) {
            ResourceLocation model = location(FOLDER + preset.getId());
            JsonObject predicate = new JsonObject();
            predicate.addProperty("custom_model_data", preset.getCustomModelData());
            JsonObject override = new JsonObject();
            override.add("predicate", predicate);
            override.addProperty("model", model.toString());
            overrides.add(override);
            writes.add(DataProvider.saveStable(cachedOutput, flat(model), pathProvider.json(model)));
        }
        mix.add("overrides", overrides);
        writes.add(DataProvider.saveStable(cachedOutput, mix, pathProvider.json(location("item/spice_mix"))));
        return CompletableFuture.allOf(writes.toArray(CompletableFuture[]::new));
    }

    private static ResourceLocation location(String path) {
        return ResourceLocation.fromNamespaceAndPath(Constants.MOD_ID, path);
    }

    /** @return An {@code item/generated} model with {@code texture} as its only layer. */
    private static JsonObject flat(ResourceLocation texture) {
        JsonObject textures = new JsonObject();
        textures.addProperty("layer0", texture.toString());
        JsonObject json = new JsonObject();
        json.addProperty("parent", "minecraft:item/generated");
        json.add("textures", textures);
        return json;
    }

    @Override
    public String getName() {
        return "Spice Mix Models";
    }
}
