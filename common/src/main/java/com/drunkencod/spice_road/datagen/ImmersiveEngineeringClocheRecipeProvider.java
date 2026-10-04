package com.drunkencod.spice_road.datagen;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.data.CachedOutput;
import net.minecraft.data.DataProvider;
import net.minecraft.data.PackOutput;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;

import com.drunkencod.spice_road.Constants;
import com.drunkencod.spice_road.block.SpicePlants;
import com.drunkencod.spice_road.spice.Spice;

/**
 * Datagens optional Immersive Engineering Garden Cloche recipes for the Spice
 * Plants selected by {@link PotCompatSpices#isCovered(Spice)}. Spice Trees
 * aren't covered, since the cloche is too small for them.
 * <p>
 * Every cloche recipe needs water by default, which also covers the Aquatic
 * Spices. The recipe carries load conditions for both loaders so it's only
 * loaded when Immersive Engineering is present, though the mod is NeoForge
 * only. Implemented as a plain {@link DataProvider} writing raw JSON, since
 * Immersive Engineering is not a compile-time dependency. The cloche consumes
 * the planting item, so it's returned at the tier's seed drop chance.
 */
public class ImmersiveEngineeringClocheRecipeProvider implements DataProvider {

    private static final String MOD_ID = "immersiveengineering";

    /** Item every cloche recipe of this provider uses as soil. */
    private static final ResourceLocation SOIL = ResourceLocation.withDefaultNamespace("dirt");

    private final PackOutput.PathProvider recipePathProvider;

    public ImmersiveEngineeringClocheRecipeProvider(PackOutput output) {
        this.recipePathProvider = output.createPathProvider(PackOutput.Target.DATA_PACK, "recipe");
    }

    @Override
    public CompletableFuture<?> run(CachedOutput cachedOutput) {
        List<CompletableFuture<?>> futures = new ArrayList<>();

        SpicePlants.getRegistered().values().stream()
                .filter(plant -> PotCompatSpices.isCovered(plant.spice()))
                .forEach(plant -> {
                    ResourceLocation id = ResourceLocation.fromNamespaceAndPath(Constants.MOD_ID,
                            "compat/immersiveengineering/cloche/" + plant.spice().getId());
                    futures.add(DataProvider.saveStable(cachedOutput,
                            clocheRecipe(plant.spice(), plant.block().get(), plant.seedItem().get(),
                                    plant.productItem().get()),
                            recipePathProvider.json(id)));
                });

        return CompletableFuture.allOf(futures.toArray(CompletableFuture[]::new));
    }

    private static JsonObject clocheRecipe(Spice spice, Block block, Item seed, Item product) {
        ResourceLocation seedId = BuiltInRegistries.ITEM.getKey(seed);

        JsonObject recipe = CompatRecipeJson.withModLoadedConditions(MOD_ID);
        recipe.addProperty("type", "immersiveengineering:cloche");
        recipe.add("input", CompatRecipeJson.itemIngredient(seedId));
        recipe.add("soil", CompatRecipeJson.itemIngredient(SOIL));
        recipe.addProperty("time", PotCompatSpices.growTicks(spice));

        JsonObject render = new JsonObject();
        render.addProperty("type", "immersiveengineering:crop");
        render.addProperty("block", BuiltInRegistries.BLOCK.getKey(block).toString());
        recipe.add("render", render);

        JsonArray results = new JsonArray();
        ResourceLocation productId = BuiltInRegistries.ITEM.getKey(product);
        double yield = PotCompatSpices.yield(spice);
        int whole = (int) Math.floor(yield);
        float fraction = (float) (yield - whole);
        if (whole > 0)
            results.add(CompatRecipeJson.itemStack(productId, whole));
        if (fraction > 0.0F)
            results.add(chanceResult(productId, fraction));
        results.add(chanceResult(seedId, spice.getTier().getSeedDropChance()));
        recipe.add("results", results);
        return recipe;
    }

    /**
     * @return A result entry for a single item that only drops with the given
     *         chance.
     */
    private static JsonObject chanceResult(ResourceLocation item, float chance) {
        JsonObject result = new JsonObject();
        result.addProperty("chance", chance);
        result.add("output", CompatRecipeJson.itemStack(item, 1));
        return result;
    }

    @Override
    public String getName() {
        return "Immersive Engineering Cloche Recipes";
    }
}
