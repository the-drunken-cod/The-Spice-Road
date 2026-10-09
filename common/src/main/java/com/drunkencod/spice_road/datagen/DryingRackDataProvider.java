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
 * Drying Rack. The wood of a rack is taken from the slab in the bottom row's
 * center slot of its recipe, so each wood has a recipe of its own. A rack drops
 * itself (its contents spill out when it breaks).
 * Raw JSON, so it runs unchanged on both loaders.
 */
public class DryingRackDataProvider implements DataProvider {

    private final PackOutput.PathProvider recipePathProvider;
    private final PackOutput.PathProvider advancementPathProvider;
    private final PackOutput.PathProvider lootPathProvider;

    /** @param output The pack output to write into. */
    public DryingRackDataProvider(PackOutput output) {
        this.recipePathProvider = output.createPathProvider(PackOutput.Target.DATA_PACK, "recipe");
        this.advancementPathProvider = output.createPathProvider(PackOutput.Target.DATA_PACK, "advancement");
        this.lootPathProvider = output.createPathProvider(PackOutput.Target.DATA_PACK, "loot_table");
    }

    @Override
    public CompletableFuture<?> run(CachedOutput cachedOutput) {
        List<CompletableFuture<?>> writes = new ArrayList<>();
        for (SpiceRackWood wood : SpiceRackWood.values()) {
            ResourceLocation id = ResourceLocation.fromNamespaceAndPath(Constants.MOD_ID, wood.getDryingRackId());
            ResourceLocation slab = BuiltInRegistries.ITEM.getKey(wood.getSlab());
            writes.add(DataProvider.saveStable(cachedOutput, recipe(id, slab),
                    recipePathProvider.json(id.withPrefix("crafting/"))));
            writes.add(DataProvider.saveStable(cachedOutput, SpiceRackDataProvider.unlockAdvancement(id, slab),
                    advancementPathProvider.json(id.withPrefix("recipes/building/"))));
            writes.add(DataProvider.saveStable(cachedOutput, lootTable(id),
                    lootPathProvider.json(id.withPrefix("blocks/"))));
        }
        return CompletableFuture.allOf(writes.toArray(CompletableFuture[]::new));
    }

    /**
     * @return Wooden rods around a tripwire hook above a slab, making one rack of
     *         the slab's wood.
     */
    private static JsonObject recipe(ResourceLocation rack, ResourceLocation slab) {
        JsonObject rodIngredient = new JsonObject();
        rodIngredient.addProperty("tag", "c:rods/wooden");
        JsonObject hookIngredient = new JsonObject();
        hookIngredient.addProperty("item", "minecraft:tripwire_hook");
        JsonObject slabIngredient = new JsonObject();
        slabIngredient.addProperty("item", slab.toString());
        JsonObject key = new JsonObject();
        key.add("S", rodIngredient);
        key.add("H", hookIngredient);
        key.add("W", slabIngredient);
        JsonArray pattern = new JsonArray();
        pattern.add(" S ");
        pattern.add("SHS");
        pattern.add("SWS");
        JsonObject result = new JsonObject();
        result.addProperty("id", rack.toString());
        result.addProperty("count", 1);
        JsonObject json = new JsonObject();
        json.addProperty("type", "minecraft:crafting_shaped");
        json.addProperty("category", "building");
        json.addProperty("group", "drying_rack");
        json.add("pattern", pattern);
        json.add("key", key);
        json.add("result", result);
        return json;
    }

    /** @return A table dropping the rack. */
    private static JsonObject lootTable(ResourceLocation rack) {
        JsonObject survives = new JsonObject();
        survives.addProperty("condition", "minecraft:survives_explosion");
        JsonArray conditions = new JsonArray();
        conditions.add(survives);

        JsonObject entry = new JsonObject();
        entry.addProperty("type", "minecraft:item");
        entry.addProperty("name", rack.toString());
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
        return "Drying Rack Data";
    }
}
