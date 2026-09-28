package com.drunkencod.spice_road.datagen;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.data.CachedOutput;
import net.minecraft.data.DataProvider;
import net.minecraft.data.PackOutput;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;

import com.drunkencod.spice_road.block.SpiceTree;
import com.drunkencod.spice_road.block.SpiceTrees;
import com.drunkencod.spice_road.spice.Spice;

/**
 * Datagens vanilla shapeless recipes converting every Spice Tree's log and
 * stripped log into 4 planks, mirroring vanilla's own log-to-planks
 * recipes, complete with a recipe-book unlock advancement. Each tree
 * Spice's target {@link VanillaPlanksType} is looked up in
 * {@link #PLANKS_TYPES}, picked by texture/color resemblance rather than
 * by which vanilla tree it's "meant" to be; a Spice missing from that map
 * defaults to {@link VanillaPlanksType#JUNGLE}, matching the Jungle-based
 * block properties every {@link SpiceTree} log currently copies. Plain
 * {@link DataProvider} writing raw JSON, so it runs unchanged on both
 * loaders.
 */
public class SpiceTreePlanksRecipeProvider implements DataProvider {

    /** Each tree Spice's planks type; Spices missing here default to {@link VanillaPlanksType#JUNGLE}. */
    private static final Map<Spice, VanillaPlanksType> PLANKS_TYPES = Map.of(
            Spice.CINNAMON, VanillaPlanksType.ACACIA);

    private final PackOutput.PathProvider recipePathProvider;
    private final PackOutput.PathProvider advancementPathProvider;

    public SpiceTreePlanksRecipeProvider(PackOutput output) {
        this.recipePathProvider = output.createPathProvider(PackOutput.Target.DATA_PACK, "recipe");
        this.advancementPathProvider = output.createPathProvider(PackOutput.Target.DATA_PACK, "advancement");
    }

    @Override
    public CompletableFuture<?> run(CachedOutput cachedOutput) {
        List<CompletableFuture<?>> futures = new ArrayList<>();

        SpiceTrees.getRegistered().forEach((spice, tree) -> {
            VanillaPlanksType planksType = PLANKS_TYPES.getOrDefault(spice, VanillaPlanksType.JUNGLE);
            futures.add(writePlanksRecipe(cachedOutput, tree.getLog().get(), planksType));
            futures.add(writePlanksRecipe(cachedOutput, tree.getStrippedLog().get(), planksType));
        });

        return CompletableFuture.allOf(futures.toArray(CompletableFuture[]::new));
    }

    /**
     * Writes one log-to-planks recipe plus its unlock advancement.
     *
     * @param cachedOutput The datagen output cache.
     * @param log          The log (or stripped log) block to convert.
     * @param planksType   The vanilla planks type it converts into.
     * @return A future completing once both files are written.
     */
    private CompletableFuture<?> writePlanksRecipe(CachedOutput cachedOutput, Block log, VanillaPlanksType planksType) {
        ResourceLocation logId = BuiltInRegistries.BLOCK.getKey(log);
        ResourceLocation planksId = BuiltInRegistries.ITEM.getKey(planksType.getPlanks());
        ResourceLocation recipeId = ResourceLocation.fromNamespaceAndPath(logId.getNamespace(),
                planksId.getPath() + "_from_" + logId.getPath());

        JsonObject recipe = new JsonObject();
        recipe.addProperty("type", "minecraft:crafting_shapeless");
        recipe.addProperty("category", "building");
        recipe.addProperty("group", "planks");
        JsonObject ingredient = new JsonObject();
        ingredient.addProperty("item", logId.toString());
        JsonArray ingredients = new JsonArray();
        ingredients.add(ingredient);
        recipe.add("ingredients", ingredients);
        JsonObject result = new JsonObject();
        result.addProperty("id", planksId.toString());
        result.addProperty("count", 4);
        recipe.add("result", result);

        CompletableFuture<?> recipeFuture = DataProvider.saveStable(cachedOutput, recipe,
                recipePathProvider.json(recipeId));
        CompletableFuture<?> advancementFuture = DataProvider.saveStable(cachedOutput,
                planksAdvancement(recipeId, logId),
                advancementPathProvider.json(recipeId.withPrefix("recipes/building_blocks/")));
        return CompletableFuture.allOf(recipeFuture, advancementFuture);
    }

    /**
     * @param recipeId The planks recipe's ID, granted as a reward.
     * @param logId    The log whose presence in inventory unlocks the recipe.
     * @return The {@code minecraft:recipes/root}-parented unlock advancement.
     */
    private static JsonObject planksAdvancement(ResourceLocation recipeId, ResourceLocation logId) {
        JsonObject advancement = new JsonObject();
        advancement.addProperty("parent", "minecraft:recipes/root");

        JsonObject rewards = new JsonObject();
        JsonArray recipes = new JsonArray();
        recipes.add(recipeId.toString());
        rewards.add("recipes", recipes);
        advancement.add("rewards", rewards);

        JsonObject itemPredicate = new JsonObject();
        itemPredicate.addProperty("items", logId.toString());
        JsonArray items = new JsonArray();
        items.add(itemPredicate);
        JsonObject conditions = new JsonObject();
        conditions.add("items", items);
        JsonObject hasLog = new JsonObject();
        hasLog.addProperty("trigger", "minecraft:inventory_changed");
        hasLog.add("conditions", conditions);
        JsonObject criteria = new JsonObject();
        criteria.add("has_log", hasLog);
        advancement.add("criteria", criteria);

        JsonArray requirement = new JsonArray();
        requirement.add("has_log");
        JsonArray requirements = new JsonArray();
        requirements.add(requirement);
        advancement.add("requirements", requirements);

        return advancement;
    }

    @Override
    public String getName() {
        return "Spice Tree Planks Recipes";
    }
}
