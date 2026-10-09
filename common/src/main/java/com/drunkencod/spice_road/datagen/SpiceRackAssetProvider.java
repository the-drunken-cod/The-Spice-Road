package com.drunkencod.spice_road.datagen;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import net.minecraft.data.CachedOutput;
import net.minecraft.data.DataProvider;
import net.minecraft.data.PackOutput;
import net.minecraft.resources.ResourceLocation;

import com.drunkencod.spice_road.Constants;
import com.drunkencod.spice_road.rack.SpiceRackWood;

/**
 * Writes the models and blockstate of every wood's Spice Rack. The three
 * hand-made models in {@code models/block/spice_rack/} ({@code floor},
 * {@code ceiling} and {@code wall}) are only the shapes; each wood gets a child
 * of each that swaps in its planks texture, a blockstate picking between them
 * by placement and facing, and an item model that shows the floor rack.
 * Raw JSON, so it runs unchanged on both loaders.
 */
public class SpiceRackAssetProvider implements DataProvider {

    private static final String FOLDER = "block/spice_rack/";
    private static final String[] SHAPES = { "floor", "ceiling", "wall" };
    private static final String[] DIRECTIONS = { "north", "east", "south", "west" };
    /** The blockstate rotation that turns a north-facing model to each of {@link #DIRECTIONS}. */
    private static final int[] ROTATIONS = { 0, 90, 180, 270 };
    private static final String POLE_TEXTURE = "minecraft:block/iron_bars";

    private final PackOutput.PathProvider modelPathProvider;
    private final PackOutput.PathProvider blockstatePathProvider;

    /** @param output The pack output to write into. */
    public SpiceRackAssetProvider(PackOutput output) {
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
            ResourceLocation id = location(wood.getRackId());
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
     *         iron bars pole.
     */
    private static JsonObject blockModel(SpiceRackWood wood, String shape) {
        JsonObject textures = new JsonObject();
        textures.addProperty("0", wood.getPlanksTexture());
        textures.addProperty("1", POLE_TEXTURE);
        textures.addProperty("particle", wood.getPlanksTexture());
        JsonObject json = new JsonObject();
        json.addProperty("parent", location(FOLDER + shape).toString());
        json.addProperty("render_type", "minecraft:cutout");
        json.add("textures", textures);
        return json;
    }

    /**
     * @return The blockstate: the ceiling, floor and wall models turned to each
     *         facing.
     */
    private static JsonObject blockstate(SpiceRackWood wood) {
        JsonObject variants = new JsonObject();
        for (int i = 0; i < DIRECTIONS.length; i++) {
            variants.add("face=ceiling,facing=" + DIRECTIONS[i],
                    variant(blockModelLocation(wood, "ceiling"), ROTATIONS[i]));
            variants.add("face=floor,facing=" + DIRECTIONS[i],
                    variant(blockModelLocation(wood, "floor"), ROTATIONS[i]));
            variants.add("face=wall,facing=" + DIRECTIONS[i],
                    variant(blockModelLocation(wood, "wall"), ROTATIONS[i]));
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

    /** @return The item model: the floor rack, set at an angle in the GUI and in hand. */
    private static JsonObject itemModel(SpiceRackWood wood) {
        JsonObject display = new JsonObject();
        display.add("gui", transform(30, 225, 0, 0, 0, 0, 0.625f));
        display.add("ground", transform(0, 0, 0, 0, 3, 0, 0.25f));
        display.add("fixed", transform(0, 0, 0, 0, 0, 0, 0.5f));
        display.add("thirdperson_righthand", transform(75, 45, 0, 0, 2.5f, 0, 0.375f));
        display.add("firstperson_righthand", transform(0, 45, 0, 0, 0, 0, 0.4f));
        display.add("firstperson_lefthand", transform(0, 225, 0, 0, 0, 0, 0.4f));
        JsonObject json = new JsonObject();
        json.addProperty("parent", blockModelLocation(wood, "floor").toString());
        json.add("display", display);
        return json;
    }

    private static JsonObject transform(float rotX, float rotY, float rotZ, float x, float y, float z, float scale) {
        JsonObject json = new JsonObject();
        json.add("rotation", vector(rotX, rotY, rotZ));
        json.add("translation", vector(x, y, z));
        json.add("scale", vector(scale, scale, scale));
        return json;
    }

    private static JsonArray vector(float x, float y, float z) {
        JsonArray array = new JsonArray();
        array.add(x);
        array.add(y);
        array.add(z);
        return array;
    }

    @Override
    public String getName() {
        return "Spice Rack Models";
    }
}
