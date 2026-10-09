package com.drunkencod.spice_road.datagen;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.data.CachedOutput;
import net.minecraft.data.DataProvider;
import net.minecraft.data.PackOutput;
import net.minecraft.resources.ResourceLocation;

import com.drunkencod.spice_road.Constants;
import com.drunkencod.spice_road.rack.SpiceRackWood;
import com.drunkencod.spice_road.rack.WoodRackRecipe;

/**
 * Writes the recipe and its unlock advancement, and the loot table of every
 * wood's Spice Rack. A rack is six wooden slabs of the same wood around an iron
 * ingot, which decide the wood (see {@link WoodRackRecipe}). It
 * drops itself with its custom name (its contents spill out when it breaks).
 * Raw JSON, so it runs unchanged on both loaders.
 */
public class SpiceRackDataProvider implements DataProvider {

    /**
     * The slab tag every wood's rack is crafted from, which modded woods can join.
     */
    static final String WOODEN_SLABS = "minecraft:wooden_slabs";

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
        ResourceLocation recipeId = ResourceLocation.fromNamespaceAndPath(Constants.MOD_ID, "spice_rack");
        writes.add(DataProvider.saveStable(cachedOutput, recipe(), recipePathProvider.json(recipeId)));
        writes.add(DataProvider.saveStable(cachedOutput, unlockAdvancement(recipeId, "#" + WOODEN_SLABS),
                advancementPathProvider.json(recipeId.withPrefix("recipes/misc/"))));
        for (SpiceRackWood wood : SpiceRackWood.values()) {
            ResourceLocation id = ResourceLocation.fromNamespaceAndPath(Constants.MOD_ID, wood.getRackId());
            writes.add(DataProvider.saveStable(cachedOutput, lootTable(id),
                    lootPathProvider.json(id.withPrefix("blocks/"))));
        }
        return CompletableFuture.allOf(writes.toArray(CompletableFuture[]::new));
    }

    /**
     * @param rackId Gets a wood's rack ID path.
     * @return The rack to make from each wood's slab, as the {@code variants} of a
     *         {@link WoodRackRecipe}.
     */
    static JsonObject variants(Function<SpiceRackWood, String> rackId) {
        JsonObject variants = new JsonObject();
        for (SpiceRackWood wood : SpiceRackWood.values())
            variants.addProperty(BuiltInRegistries.ITEM.getKey(wood.getSlab()).toString(),
                    ResourceLocation.fromNamespaceAndPath(Constants.MOD_ID, rackId.apply(wood)).toString());
        return variants;
    }

    /**
     * @return Wooden slabs of one wood on the top and bottom row and an iron ingot
     *         between, making a rack of that wood, or an oak one if that wood has
     *         no rack.
     */
    private static JsonObject recipe() {
        JsonObject slabIngredient = new JsonObject();
        slabIngredient.addProperty("tag", WOODEN_SLABS);
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
        result.addProperty("id",
                ResourceLocation.fromNamespaceAndPath(Constants.MOD_ID, SpiceRackWood.OAK.getRackId()).toString());
        result.addProperty("count", 1);
        JsonObject json = new JsonObject();
        json.addProperty("type", "spice_road:crafting_wood_rack");
        json.addProperty("category", "misc");
        json.add("pattern", pattern);
        json.add("key", key);
        json.add("result", result);
        json.add("variants", variants(SpiceRackWood::getRackId));
        return json;
    }

    /**
     * @param recipe The recipe to unlock.
     * @param slabs  The item ID or {@code #}-prefixed item tag of the slabs.
     * @return The advancement unlocking the recipe once the player holds one of
     *         the slabs.
     */
    static JsonObject unlockAdvancement(ResourceLocation recipe, String slabs) {
        JsonObject json = new JsonObject();
        json.addProperty("parent", "minecraft:recipes/root");
        JsonArray recipes = new JsonArray();
        recipes.add(recipe.toString());
        JsonObject rewards = new JsonObject();
        rewards.add("recipes", recipes);
        json.add("rewards", rewards);

        JsonObject itemPredicate = new JsonObject();
        itemPredicate.addProperty("items", slabs);
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
