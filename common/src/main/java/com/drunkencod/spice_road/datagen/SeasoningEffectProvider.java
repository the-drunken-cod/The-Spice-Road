package com.drunkencod.spice_road.datagen;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import com.google.gson.JsonObject;
import com.mojang.serialization.JsonOps;

import net.minecraft.data.CachedOutput;
import net.minecraft.data.DataProvider;
import net.minecraft.data.PackOutput;
import net.minecraft.resources.ResourceLocation;

import com.drunkencod.spice_road.Constants;
import com.drunkencod.spice_road.spice.effect.SeasoningEffectDef;

/**
 * Writes the {@link DefaultSeasoningEffect default Seasoning Effect catalog} as
 * {@code data/spice_road/seasoning_effect/*.json}, as raw JSON so it runs
 * unchanged on both loaders. Every entry is also parsed back through the real
 * codec, so datagen fails on an entry the game would reject.
 */
public class SeasoningEffectProvider implements DataProvider {

    private final PackOutput.PathProvider pathProvider;

    /** @param output The pack output to write into. */
    public SeasoningEffectProvider(PackOutput output) {
        this.pathProvider = output.createPathProvider(PackOutput.Target.DATA_PACK, "seasoning_effect");
    }

    @Override
    public CompletableFuture<?> run(CachedOutput cachedOutput) {
        List<CompletableFuture<?>> writes = new ArrayList<>();
        for (DefaultSeasoningEffect entry : DefaultSeasoningEffect.values()) {
            JsonObject json = new JsonObject();
            json.addProperty("effect", entry.getEffect());
            json.addProperty("kind", entry.getKind().getSerializedName());
            if (entry.getAxis() != null) {
                JsonObject pole = new JsonObject();
                pole.addProperty("axis", entry.getAxis().getSerializedName());
                pole.addProperty("positive", entry.isPositive());
                json.add("pole", pole);
            }
            if (entry.getRandomWeight() > 0)
                json.addProperty("random_weight", entry.getRandomWeight());
            json.addProperty("base_duration", entry.getBaseDuration());
            json.addProperty("max_level", entry.getMaxLevel());
            json.addProperty("level_scaling", entry.getScaling().getSerializedName());
            SeasoningEffectDef.CODEC.parse(JsonOps.INSTANCE, json).getOrThrow(
                    error -> new IllegalStateException("Invalid Seasoning Effect " + entry.getId() + ": " + error));
            writes.add(DataProvider.saveStable(cachedOutput, json,
                    pathProvider.json(ResourceLocation.fromNamespaceAndPath(Constants.MOD_ID, entry.getId()))));
        }
        return CompletableFuture.allOf(writes.toArray(CompletableFuture[]::new));
    }

    @Override
    public String getName() {
        return "Seasoning Effects";
    }
}
