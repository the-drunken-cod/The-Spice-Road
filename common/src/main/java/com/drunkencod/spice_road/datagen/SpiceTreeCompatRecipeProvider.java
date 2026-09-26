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

import com.drunkencod.spice_road.Constants;
import com.drunkencod.spice_road.block.SpiceTree;
import com.drunkencod.spice_road.block.SpiceTrees;

/**
 * Datagens optional mod-compat recipes that strip {@link SpiceTree.HarvestPart#BARK}
 * Spice Tree logs into their stripped log plus the tree's Spice, mirroring what
 * stripping the placed block in-world yields:
 * <ul>
 * <li>Create's Mechanical Saw ({@code create:cutting}). Written to the exact
 * ID of the plain log-to-stripped-log recipe Create generates at runtime for
 * every {@code *_log}/{@code stripped_*_log} pair, so it replaces that recipe
 * instead of competing with it (Create's runtime pack has the lowest
 * priority).</li>
 * <li>Farmer's Delight's Cutting Board ({@code farmersdelight:cutting}).</li>
 * </ul>
 * Each recipe carries both NeoForge and Fabric load conditions, so it's only
 * loaded when the target mod is present. Implemented as a plain
 * {@link DataProvider} writing raw JSON, since neither mod is a compile-time
 * dependency.
 * <p>
 * The Spice count is {@code Spice#getDropAmount()} at the default yield
 * multiplier, since recipes can't read live config.
 */
public class SpiceTreeCompatRecipeProvider implements DataProvider {

    private final PackOutput.PathProvider recipePathProvider;

    public SpiceTreeCompatRecipeProvider(PackOutput output) {
        this.recipePathProvider = output.createPathProvider(PackOutput.Target.DATA_PACK, "recipe");
    }

    @Override
    public CompletableFuture<?> run(CachedOutput cachedOutput) {
        List<CompletableFuture<?>> futures = new ArrayList<>();

        SpiceTrees.getRegistered().values().stream()
                .filter(tree -> tree.getHarvestPart() == SpiceTree.HarvestPart.BARK)
                .forEach(tree -> {
                    ResourceLocation log = BuiltInRegistries.BLOCK.getKey(tree.getLog().get());
                    ResourceLocation strippedLog = BuiltInRegistries.BLOCK.getKey(tree.getStrippedLog().get());
                    Item product = tree.getProductItem().get();
                    int count = (int) Math.floor(
                            tree.getSpice().getDropAmount() * Constants.DEFAULT_SPICE_TREE_HARVEST_YIELD_MULTIPLIER);

                    ResourceLocation createId = ResourceLocation.fromNamespaceAndPath("create",
                            "cutting/runtime_generated/compat/" + log.getNamespace() + "/" + log.getPath() + "_to_"
                                    + strippedLog.getPath());
                    futures.add(DataProvider.saveStable(cachedOutput, createCuttingRecipe(log, strippedLog, product, count),
                            recipePathProvider.json(createId)));

                    ResourceLocation farmersDelightId = ResourceLocation.fromNamespaceAndPath(Constants.MOD_ID,
                            "compat/farmersdelight/cutting/" + log.getPath());
                    futures.add(DataProvider.saveStable(cachedOutput,
                            farmersDelightCuttingRecipe(log, strippedLog, product, count),
                            recipePathProvider.json(farmersDelightId)));
                });

        return CompletableFuture.allOf(futures.toArray(CompletableFuture[]::new));
    }

    private static JsonObject createCuttingRecipe(ResourceLocation log, ResourceLocation strippedLog, Item product,
            int count) {
        JsonObject recipe = withModLoadedConditions("create");
        recipe.addProperty("type", "create:cutting");
        recipe.add("ingredients", singleItemIngredient(log));
        recipe.addProperty("processing_time", 50);

        JsonArray results = new JsonArray();
        results.add(itemStack(strippedLog, 1));
        if (count > 0)
            results.add(itemStack(BuiltInRegistries.ITEM.getKey(product), count));
        recipe.add("results", results);
        return recipe;
    }

    private static JsonObject farmersDelightCuttingRecipe(ResourceLocation log, ResourceLocation strippedLog,
            Item product, int count) {
        JsonObject recipe = withModLoadedConditions("farmersdelight");
        recipe.addProperty("type", "farmersdelight:cutting");
        recipe.add("ingredients", singleItemIngredient(log));

        JsonArray results = new JsonArray();
        results.add(wrapItem(itemStack(strippedLog, 1)));
        if (count > 0)
            results.add(wrapItem(itemStack(BuiltInRegistries.ITEM.getKey(product), count)));
        recipe.add("result", results);

        JsonObject sound = new JsonObject();
        sound.addProperty("sound_id", "minecraft:item.axe.strip");
        recipe.add("sound", sound);

        JsonObject stripAbility = new JsonObject();
        stripAbility.addProperty("type", "farmersdelight:item_ability");
        stripAbility.addProperty("action", "axe_strip");
        JsonObject axesTag = new JsonObject();
        axesTag.addProperty("tag", "minecraft:axes");
        JsonArray tool = new JsonArray();
        tool.add(stripAbility);
        tool.add(axesTag);
        recipe.add("tool", tool);
        return recipe;
    }

    /**
     * @return A recipe root carrying both NeoForge's and Fabric's
     *         "only load when {@code modId} is present" conditions.
     */
    private static JsonObject withModLoadedConditions(String modId) {
        JsonObject recipe = new JsonObject();

        JsonObject neoForgeCondition = new JsonObject();
        neoForgeCondition.addProperty("type", "neoforge:mod_loaded");
        neoForgeCondition.addProperty("modid", modId);
        JsonArray neoForgeConditions = new JsonArray();
        neoForgeConditions.add(neoForgeCondition);
        recipe.add("neoforge:conditions", neoForgeConditions);

        JsonObject fabricCondition = new JsonObject();
        fabricCondition.addProperty("condition", "fabric:all_mods_loaded");
        JsonArray modIds = new JsonArray();
        modIds.add(modId);
        fabricCondition.add("values", modIds);
        JsonArray fabricConditions = new JsonArray();
        fabricConditions.add(fabricCondition);
        recipe.add("fabric:load_conditions", fabricConditions);

        return recipe;
    }

    private static JsonArray singleItemIngredient(ResourceLocation item) {
        JsonObject ingredient = new JsonObject();
        ingredient.addProperty("item", item.toString());
        JsonArray ingredients = new JsonArray();
        ingredients.add(ingredient);
        return ingredients;
    }

    private static JsonObject itemStack(ResourceLocation item, int count) {
        JsonObject stack = new JsonObject();
        stack.addProperty("id", item.toString());
        stack.addProperty("count", count);
        return stack;
    }

    private static JsonObject wrapItem(JsonObject stack) {
        JsonObject wrapper = new JsonObject();
        wrapper.add("item", stack);
        return wrapper;
    }

    @Override
    public String getName() {
        return "Spice Tree Compat Recipes";
    }
}
