package com.drunkencod.spice_road.datagen;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import com.google.gson.JsonObject;

import net.minecraft.data.CachedOutput;
import net.minecraft.data.DataProvider;
import net.minecraft.data.PackOutput;
import net.minecraft.resources.ResourceLocation;

import com.drunkencod.spice_road.Constants;
import com.drunkencod.spice_road.rack.SpiceRackWood;

/**
 * Writes the models and blockstate of every wood's Drying Rack. The two
 * hand-made models in {@code models/block/drying_rack/} ({@code unheated} and
 * {@code heated}) are only the shapes; each wood gets a child of each that
 * swaps in its planks texture, a blockstate picking between them by heat and
 * facing, and an item model that shows the unheated rack.
 * Raw JSON, so it runs unchanged on both loaders.
 */
public class DryingRackAssetProvider implements DataProvider {

    private static final String FOLDER = "block/drying_rack/";
    private static final String[] SHAPES = { "unheated", "heated" };
    private static final String[] DIRECTIONS = { "north", "east", "south", "west" };
    /**
     * The blockstate rotation that turns the north-facing models to each of
     * {@link #DIRECTIONS}.
     */
    private static final int[] ROTATIONS = { 0, 90, 180, 270 };

    private final PackOutput.PathProvider modelPathProvider;
    private final PackOutput.PathProvider blockstatePathProvider;

    /** @param output The pack output to write into. */
    public DryingRackAssetProvider(PackOutput output) {
        this.modelPathProvider = output.createPathProvider(PackOutput.Target.RESOURCE_PACK, "models");
        this.blockstatePathProvider = output.createPathProvider(PackOutput.Target.RESOURCE_PACK, "blockstates");
    }

    @Override
    public CompletableFuture<?> run(CachedOutput cachedOutput) {
        List<CompletableFuture<?>> writes = new ArrayList<>();
        for (SpiceRackWood wood : SpiceRackWood.values()) {
            for (String shape : SHAPES)
                writes.add(DataProvider.saveStable(cachedOutput, blockModel(wood, shape),
                        modelPathProvider.json(blockModelLocation(wood, shape))));
            ResourceLocation id = location(wood.getDryingRackId());
            writes.add(DataProvider.saveStable(cachedOutput, blockstate(wood), blockstatePathProvider.json(id)));
            writes.add(DataProvider.saveStable(cachedOutput, itemModel(wood),
                    modelPathProvider.json(id.withPrefix("item/"))));
        }
        return CompletableFuture.allOf(writes.toArray(CompletableFuture[]::new));
    }

    private static ResourceLocation location(String path) {
        return ResourceLocation.fromNamespaceAndPath(Constants.MOD_ID, path);
    }

    /** @return Where {@code wood}'s child of the {@code shape} model goes. */
    private static ResourceLocation blockModelLocation(SpiceRackWood wood, String shape) {
        return location(FOLDER + wood.getId() + "/" + shape);
    }

    /**
     * @return A child of the hand-made {@code shape} model with {@code wood}'s
     *         planks on it, and the cutout render type NeoForge reads for the
     *         iron bars hooks.
     */
    private static JsonObject blockModel(SpiceRackWood wood, String shape) {
        JsonObject textures = new JsonObject();
        textures.addProperty("1", wood.getPlanksTexture());
        textures.addProperty("particle", wood.getPlanksTexture());
        JsonObject json = new JsonObject();
        json.addProperty("parent", location(FOLDER + shape).toString());
        json.addProperty("render_type", "minecraft:cutout");
        json.add("textures", textures);
        return json;
    }

    /**
     * @return The blockstate: the unheated or heated model turned to each facing.
     */
    private static JsonObject blockstate(SpiceRackWood wood) {
        JsonObject variants = new JsonObject();
        for (int i = 0; i < DIRECTIONS.length; i++) {
            variants.add("facing=" + DIRECTIONS[i] + ",heated=false",
                    variant(blockModelLocation(wood, "unheated"), ROTATIONS[i]));
            variants.add("facing=" + DIRECTIONS[i] + ",heated=true",
                    variant(blockModelLocation(wood, "heated"), ROTATIONS[i]));
        }
        JsonObject json = new JsonObject();
        json.add("variants", variants);
        return json;
    }

    private static JsonObject variant(ResourceLocation model, int rotationY) {
        JsonObject json = new JsonObject();
        json.addProperty("model", model.toString());
        if (rotationY != 0)
            json.addProperty("y", rotationY);
        return json;
    }

    /**
     * @return The item model: the unheated rack, which takes its GUI and hand
     *         transforms from the hand-made model it descends from.
     */
    private static JsonObject itemModel(SpiceRackWood wood) {
        JsonObject json = new JsonObject();
        json.addProperty("parent", blockModelLocation(wood, "unheated").toString());
        return json;
    }

    @Override
    public String getName() {
        return "Drying Rack Models";
    }
}
