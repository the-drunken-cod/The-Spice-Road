package com.drunkencod.spice_road.datagen;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import com.google.gson.JsonObject;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.data.CachedOutput;
import net.minecraft.data.DataProvider;
import net.minecraft.data.PackOutput;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;

import com.drunkencod.spice_road.Constants;
import com.drunkencod.spice_road.drying.DryingRecipe;
import com.drunkencod.spice_road.registry.ModItems;
import com.drunkencod.spice_road.spice.ProcessedSpice;
import com.drunkencod.spice_road.spice.ProcessingMethod;
import com.drunkencod.spice_road.spice.Tier;

/**
 * Datagens the {@code spice_road:drying} recipe of every
 * {@link ProcessedSpice} made by {@link ProcessingMethod#DRYING}: one raw item
 * into one dried item. The time is {@link DryingRecipe#DEFAULT_DRYING_TIME}
 * scaled by the source Spice's {@link Tier}, written into the recipe so a
 * datapack can retune it. Plain {@link DataProvider} writing raw JSON, so it
 * runs unchanged on both loaders.
 */
public class DryingRecipeProvider implements DataProvider {

    /**
     * How many times longer a Spice of each tier takes to dry than
     * {@link DryingRecipe#DEFAULT_DRYING_TIME}.
     */
    private static final Map<Tier, Double> TIME_MULTIPLIERS = Map.of(
            Tier.COMMON, 1.0,
            Tier.UNCOMMON, 1.5,
            Tier.RARE, 2.0,
            Tier.EPIC, 3.0);

    private final PackOutput.PathProvider recipePathProvider;

    public DryingRecipeProvider(PackOutput output) {
        this.recipePathProvider = output.createPathProvider(PackOutput.Target.DATA_PACK, "recipe");
    }

    @Override
    public CompletableFuture<?> run(CachedOutput cachedOutput) {
        List<CompletableFuture<?>> futures = new ArrayList<>();
        for (ProcessedSpice processed : ProcessedSpice.values()) {
            if (processed.getMethod() != ProcessingMethod.DRYING)
                continue;
            Item raw = ModItems.byId(processed.getSource().getId());
            Item dried = processed.getItem();
            if (raw == null || dried == null)
                continue;
            int time = (int) Math.round(DryingRecipe.DEFAULT_DRYING_TIME * TIME_MULTIPLIERS.get(processed.getTier()));
            ResourceLocation id = ResourceLocation.fromNamespaceAndPath(Constants.MOD_ID,
                    "drying/" + processed.getId());
            futures.add(DataProvider.saveStable(cachedOutput, recipe(raw, dried, time), recipePathProvider.json(id)));
        }
        return CompletableFuture.allOf(futures.toArray(CompletableFuture[]::new));
    }

    private static JsonObject recipe(Item raw, Item dried, int time) {
        JsonObject recipe = new JsonObject();
        recipe.addProperty("type", Constants.MOD_ID + ":drying");
        JsonObject ingredient = new JsonObject();
        ingredient.addProperty("item", BuiltInRegistries.ITEM.getKey(raw).toString());
        recipe.add("ingredient", ingredient);
        JsonObject result = new JsonObject();
        result.addProperty("id", BuiltInRegistries.ITEM.getKey(dried).toString());
        recipe.add("result", result);
        recipe.addProperty("drying_time", time);
        return recipe;
    }

    @Override
    public String getName() {
        return "Spice Road drying recipes";
    }
}
