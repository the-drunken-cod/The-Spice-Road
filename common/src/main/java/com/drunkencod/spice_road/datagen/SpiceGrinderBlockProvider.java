package com.drunkencod.spice_road.datagen;

import java.util.concurrent.CompletableFuture;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import net.minecraft.data.CachedOutput;
import net.minecraft.data.DataProvider;
import net.minecraft.data.PackOutput;
import net.minecraft.resources.ResourceLocation;

import com.drunkencod.spice_road.Constants;

/**
 * Writes the blockstate and the loot table of the placed Spice Grinder. The
 * blockstate turns the hand-made {@code block/spice_grinder_block} model to
 * each facing. The loot table always drops the Grinder with the components of
 * its block entity (its run and custom name), and deliberately has no
 * explosion survival condition, so no run is lost to a blast.
 * Raw JSON, so it runs unchanged on both loaders.
 */
public class SpiceGrinderBlockProvider implements DataProvider {

    private static final String ID = "spice_grinder";
    private static final String MODEL = "block/spice_grinder_block";
    private static final String[] FACINGS = { "north", "east", "south", "west" };

    private final PackOutput.PathProvider blockstatePathProvider;
    private final PackOutput.PathProvider lootPathProvider;

    /** @param output The pack output to write into. */
    public SpiceGrinderBlockProvider(PackOutput output) {
        this.blockstatePathProvider = output.createPathProvider(PackOutput.Target.RESOURCE_PACK, "blockstates");
        this.lootPathProvider = output.createPathProvider(PackOutput.Target.DATA_PACK, "loot_table");
    }

    @Override
    public CompletableFuture<?> run(CachedOutput cachedOutput) {
        ResourceLocation id = ResourceLocation.fromNamespaceAndPath(Constants.MOD_ID, ID);
        return CompletableFuture.allOf(
                DataProvider.saveStable(cachedOutput, blockstate(), blockstatePathProvider.json(id)),
                DataProvider.saveStable(cachedOutput, lootTable(id), lootPathProvider.json(id.withPrefix("blocks/"))));
    }

    /** @return The model for each facing: north is unturned, the rest a quarter turn apart. */
    private static JsonObject blockstate() {
        JsonObject variants = new JsonObject();
        for (int i = 0; i < FACINGS.length; i++) {
            JsonObject variant = new JsonObject();
            variant.addProperty("model", ResourceLocation.fromNamespaceAndPath(Constants.MOD_ID, MODEL).toString());
            if (i > 0)
                variant.addProperty("y", i * 90);
            variants.add("facing=" + FACINGS[i], variant);
        }
        JsonObject json = new JsonObject();
        json.add("variants", variants);
        return json;
    }

    /** @return A table dropping the Grinder with all components of the block entity. */
    private static JsonObject lootTable(ResourceLocation block) {
        JsonObject copyComponents = new JsonObject();
        copyComponents.addProperty("function", "minecraft:copy_components");
        copyComponents.addProperty("source", "block_entity");
        JsonArray functions = new JsonArray();
        functions.add(copyComponents);

        JsonObject entry = new JsonObject();
        entry.addProperty("type", "minecraft:item");
        entry.addProperty("name", block.toString());
        entry.add("functions", functions);
        JsonArray entries = new JsonArray();
        entries.add(entry);

        JsonObject pool = new JsonObject();
        pool.addProperty("rolls", 1.0);
        pool.addProperty("bonus_rolls", 0.0);
        pool.add("entries", entries);
        JsonArray pools = new JsonArray();
        pools.add(pool);

        JsonObject json = new JsonObject();
        json.addProperty("type", "minecraft:block");
        json.addProperty("random_sequence", block.withPrefix("blocks/").toString());
        json.add("pools", pools);
        return json;
    }

    @Override
    public String getName() {
        return "Spice Grinder Block";
    }
}
