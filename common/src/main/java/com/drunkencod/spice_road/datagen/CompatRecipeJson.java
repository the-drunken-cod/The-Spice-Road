package com.drunkencod.spice_road.datagen;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import net.minecraft.resources.ResourceLocation;

/**
 * Raw-JSON building blocks shared by the datagen providers that write recipes
 * for optional mods, none of which are compile-time dependencies.
 */
final class CompatRecipeJson {

    private CompatRecipeJson() {
    }

    /**
     * @param modId The ID of the mod the recipe depends on.
     * @return A recipe root carrying both NeoForge's and Fabric's
     *         "only load when {@code modId} is present" conditions. Each loader
     *         ignores the other's key.
     */
    static JsonObject withModLoadedConditions(String modId) {
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

    /**
     * @return An item stack object, {@code {"id": ..., "count": ...}}.
     */
    static JsonObject itemStack(ResourceLocation item, int count) {
        JsonObject stack = new JsonObject();
        stack.addProperty("id", item.toString());
        stack.addProperty("count", count);
        return stack;
    }

    /**
     * @return An ingredient object matching a single item, {@code {"item": ...}}.
     */
    static JsonObject itemIngredient(ResourceLocation item) {
        JsonObject ingredient = new JsonObject();
        ingredient.addProperty("item", item.toString());
        return ingredient;
    }

    /**
     * @return An ingredient object matching an item tag, {@code {"tag": ...}}.
     */
    static JsonObject tagIngredient(String tag) {
        JsonObject ingredient = new JsonObject();
        ingredient.addProperty("tag", tag);
        return ingredient;
    }
}
