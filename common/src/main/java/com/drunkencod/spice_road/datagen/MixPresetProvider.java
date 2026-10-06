package com.drunkencod.spice_road.datagen;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.mojang.serialization.JsonOps;

import net.minecraft.data.CachedOutput;
import net.minecraft.data.DataProvider;
import net.minecraft.data.PackOutput;
import net.minecraft.resources.ResourceLocation;

import com.drunkencod.spice_road.Constants;
import com.drunkencod.spice_road.mix.MixPreset;
import com.drunkencod.spice_road.mix.MixPresetReloadListener;
import com.drunkencod.spice_road.spice.Spice;

/**
 * Writes every {@link DefaultMixPreset} as
 * {@code data/spice_road/mix_preset/*.json}, plus a vanilla shapeless recipe
 * per preset (a jar and one batch of its spices) with a recipe book unlock
 * advancement. The recipe exists so recipe viewers like JEI and EMI can show
 * presets: its result carries the same components the jar-and-spices recipe
 * would produce, so whichever of the two a grid matches, the result is the
 * same. Raw JSON, so it runs unchanged on both loaders. Every preset is also
 * parsed back through the real codec, so datagen fails on a preset the game
 * would reject.
 */
public class MixPresetProvider implements DataProvider {

    private static final String JAR = ResourceLocation.fromNamespaceAndPath(Constants.MOD_ID, "jar").toString();

    private final PackOutput.PathProvider presetPathProvider;
    private final PackOutput.PathProvider recipePathProvider;
    private final PackOutput.PathProvider advancementPathProvider;

    /** @param output The pack output to write into. */
    public MixPresetProvider(PackOutput output) {
        this.presetPathProvider = output.createPathProvider(PackOutput.Target.DATA_PACK,
                MixPresetReloadListener.DIRECTORY);
        this.recipePathProvider = output.createPathProvider(PackOutput.Target.DATA_PACK, "recipe");
        this.advancementPathProvider = output.createPathProvider(PackOutput.Target.DATA_PACK, "advancement");
    }

    @Override
    public CompletableFuture<?> run(CachedOutput cachedOutput) {
        List<CompletableFuture<?>> writes = new ArrayList<>();
        for (DefaultMixPreset preset : DefaultMixPreset.values()) {
            ResourceLocation id = ResourceLocation.fromNamespaceAndPath(Constants.MOD_ID, preset.getId());
            JsonArray spices = spicesOf(preset);
            JsonObject json = new JsonObject();
            json.add("spices", spices);
            json.addProperty("custom_model_data", preset.getCustomModelData());
            MixPreset.CODEC.parse(JsonOps.INSTANCE, json).getOrThrow(
                    error -> new IllegalStateException("Invalid Mix Preset " + preset.getId() + ": " + error));
            writes.add(DataProvider.saveStable(cachedOutput, json, presetPathProvider.json(id)));

            ResourceLocation recipeId = id.withPrefix("spice_mix/");
            writes.add(DataProvider.saveStable(cachedOutput, recipe(id, preset, spices),
                    recipePathProvider.json(recipeId)));
            writes.add(DataProvider.saveStable(cachedOutput, unlockAdvancement(recipeId),
                    advancementPathProvider.json(recipeId.withPrefix("recipes/misc/"))));
        }
        return CompletableFuture.allOf(writes.toArray(CompletableFuture[]::new));
    }

    /** @return The preset's spices as a {@code [{"item", "count"}]} list, in Spice ID order. */
    private static JsonArray spicesOf(DefaultMixPreset preset) {
        JsonArray spices = new JsonArray();
        preset.getSpices().entrySet().stream()
                .sorted(Map.Entry.comparingByKey(Comparator.comparing(Spice::getId)))
                .forEach(entry -> {
                    JsonObject spice = new JsonObject();
                    spice.addProperty("item",
                            ResourceLocation.fromNamespaceAndPath(Constants.MOD_ID, entry.getKey().getId()).toString());
                    spice.addProperty("count", entry.getValue());
                    spices.add(spice);
                });
        return spices;
    }

    /**
     * @return A shapeless recipe of a jar and one batch of the preset's spices,
     *         yielding the preset's Spice Mix with all of its components.
     */
    private static JsonObject recipe(ResourceLocation id, DefaultMixPreset preset, JsonArray spices) {
        JsonArray ingredients = new JsonArray();
        ingredients.add(itemIngredient(JAR));
        for (var element : spices) {
            JsonObject spice = element.getAsJsonObject();
            for (int i = 0; i < spice.get("count").getAsInt(); i++)
                ingredients.add(itemIngredient(spice.get("item").getAsString()));
        }

        JsonObject mix = new JsonObject();
        mix.addProperty("preset", id.toString());
        mix.add("spices", spices);
        JsonObject components = new JsonObject();
        components.add(Constants.MOD_ID + ":spice_mix", mix);
        components.addProperty("minecraft:custom_model_data", preset.getCustomModelData());
        JsonObject result = new JsonObject();
        result.addProperty("id", ResourceLocation.fromNamespaceAndPath(Constants.MOD_ID, "spice_mix").toString());
        result.addProperty("count", 1);
        result.add("components", components);

        JsonObject recipe = new JsonObject();
        recipe.addProperty("type", "minecraft:crafting_shapeless");
        recipe.addProperty("category", "misc");
        recipe.addProperty("group", Constants.MOD_ID + ":spice_mix");
        recipe.add("ingredients", ingredients);
        recipe.add("result", result);
        return recipe;
    }

    private static JsonObject itemIngredient(String item) {
        JsonObject ingredient = new JsonObject();
        ingredient.addProperty("item", item);
        return ingredient;
    }

    /** @return A {@code minecraft:recipes/root}-parented advancement unlocking the recipe once a jar is held. */
    private static JsonObject unlockAdvancement(ResourceLocation recipeId) {
        JsonObject itemPredicate = new JsonObject();
        itemPredicate.addProperty("items", JAR);
        JsonArray items = new JsonArray();
        items.add(itemPredicate);
        JsonObject conditions = new JsonObject();
        conditions.add("items", items);
        JsonObject hasJar = new JsonObject();
        hasJar.addProperty("trigger", "minecraft:inventory_changed");
        hasJar.add("conditions", conditions);
        JsonObject criteria = new JsonObject();
        criteria.add("has_jar", hasJar);

        JsonArray recipes = new JsonArray();
        recipes.add(recipeId.toString());
        JsonObject rewards = new JsonObject();
        rewards.add("recipes", recipes);

        JsonArray requirement = new JsonArray();
        requirement.add("has_jar");
        JsonArray requirements = new JsonArray();
        requirements.add(requirement);

        JsonObject advancement = new JsonObject();
        advancement.addProperty("parent", "minecraft:recipes/root");
        advancement.add("criteria", criteria);
        advancement.add("requirements", requirements);
        advancement.add("rewards", rewards);
        return advancement;
    }

    @Override
    public String getName() {
        return "Mix Presets";
    }
}
