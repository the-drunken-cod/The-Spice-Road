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

import com.drunkencod.spice_road.Constants;
import com.drunkencod.spice_road.rack.SpiceRackWood;

/**
 * Writes the recipe, its unlock advancement and the loot table of every wood's
 * Spice Rack. A rack is six slabs of its wood around a wooden rod, and drops
 * itself with its custom name (its contents spill out when it breaks).
 * Raw JSON, so it runs unchanged on both loaders.
 */
public class SpiceRackDataProvider implements DataProvider {

    private final PackOutput.PathProvider recipePathProvider;
    private final PackOutput.PathProvider advancementPathProvider;
    private final PackOutput.PathProvider lootPathProvider;

    /** @param output The pack output to write into. */
    public SpiceRackDataProvider(PackOutput output) {
        this.recipePathProvider = output.createPathProvider(PackOutput.Target.DATA_PACK, "recipe");
        this.advancementPathProvider = output.createPathProvider(PackOutput.Target.DATA_PACK, "advancement");
        this.lootPathProvider = output.createPathProvider(PackOutput.Target.DATA_PACK, "loot_table");
    }

    @Override
    public CompletableFuture<?> run(CachedOutput cachedOutput) {
        List<CompletableFuture<?>> writes = new ArrayList<>();
        for (SpiceRackWood wood : SpiceRackWood.values()) {
            ResourceLocation id = ResourceLocation.fromNamespaceAndPath(Constants.MOD_ID, wood.getRackId());
            ResourceLocation slab = BuiltInRegistries.ITEM.getKey(wood.getSlab());
            writes.add(DataProvider.saveStable(cachedOutput, recipe(id, slab), recipePathProvider.json(id)));
            writes.add(DataProvider.saveStable(cachedOutput, unlockAdvancement(id, slab),
                    advancementPathProvider.json(id.withPrefix("recipes/misc/"))));
            writes.add(DataProvider.saveStable(cachedOutput, lootTable(id),
                    lootPathProvider.json(id.withPrefix("blocks/"))));
        }
        return CompletableFuture.allOf(writes.toArray(CompletableFuture[]::new));
    }

    /**
     * @return Slabs on the top and bottom row and a rod between, making one rack.
     */
    private static JsonObject recipe(ResourceLocation rack, ResourceLocation slab) {
        JsonObject slabIngredient = new JsonObject();
        slabIngredient.addProperty("item", slab.toString());
        JsonObject ironIngredient = new JsonObject();
        ironIngredient.addProperty("tag", "c:ingots/iron");
        JsonObject key = new JsonObject();
        key.add("S", slabIngredient);
        key.add("I", ironIngredient);
        JsonArray pattern = new JsonArray();
        pattern.add("SSS");
        pattern.add(" I ");
        pattern.add("SSS");
        JsonObject result = new JsonObject();
        result.addProperty("id", rack.toString());
        result.addProperty("count", 1);
        JsonObject json = new JsonObject();
        json.addProperty("type", "minecraft:crafting_shaped");
        json.addProperty("category", "misc");
        json.addProperty("group", "spice_rack");
        json.add("pattern", pattern);
        json.add("key", key);
        json.add("result", result);
        return json;
    }

    /**
     * @return The advancement unlocking the recipe once the player holds the wood's
     *         slab.
     */
    private static JsonObject unlockAdvancement(ResourceLocation recipe, ResourceLocation slab) {
        JsonObject json = new JsonObject();
        json.addProperty("parent", "minecraft:recipes/root");
        JsonArray recipes = new JsonArray();
        recipes.add(recipe.toString());
        JsonObject rewards = new JsonObject();
        rewards.add("recipes", recipes);
        json.add("rewards", rewards);

        JsonObject itemPredicate = new JsonObject();
        itemPredicate.addProperty("items", slab.toString());
        JsonArray items = new JsonArray();
        items.add(itemPredicate);
        JsonObject conditions = new JsonObject();
        conditions.add("items", items);
        JsonObject hasSlab = new JsonObject();
        hasSlab.addProperty("trigger", "minecraft:inventory_changed");
        hasSlab.add("conditions", conditions);
        JsonObject criteria = new JsonObject();
        criteria.add("has_slab", hasSlab);
        json.add("criteria", criteria);

        JsonArray requirement = new JsonArray();
        requirement.add("has_slab");
        JsonArray requirements = new JsonArray();
        requirements.add(requirement);
        json.add("requirements", requirements);
        return json;
    }

    /** @return A table dropping the rack, keeping its custom name. */
    private static JsonObject lootTable(ResourceLocation rack) {
        JsonObject survives = new JsonObject();
        survives.addProperty("condition", "minecraft:survives_explosion");
        JsonArray conditions = new JsonArray();
        conditions.add(survives);

        JsonArray include = new JsonArray();
        include.add("minecraft:custom_name");
        JsonObject copyName = new JsonObject();
        copyName.addProperty("function", "minecraft:copy_components");
        copyName.addProperty("source", "block_entity");
        copyName.add("include", include);
        JsonArray functions = new JsonArray();
        functions.add(copyName);

        JsonObject entry = new JsonObject();
        entry.addProperty("type", "minecraft:item");
        entry.addProperty("name", rack.toString());
        entry.add("functions", functions);
        JsonArray entries = new JsonArray();
        entries.add(entry);

        JsonObject pool = new JsonObject();
        pool.addProperty("rolls", 1.0);
        pool.addProperty("bonus_rolls", 0.0);
        pool.add("conditions", conditions);
        pool.add("entries", entries);
        JsonArray pools = new JsonArray();
        pools.add(pool);

        JsonObject json = new JsonObject();
        json.addProperty("type", "minecraft:block");
        json.addProperty("random_sequence", rack.withPrefix("blocks/").toString());
        json.add("pools", pools);
        return json;
    }

    @Override
    public String getName() {
        return "Spice Rack Data";
    }
}
