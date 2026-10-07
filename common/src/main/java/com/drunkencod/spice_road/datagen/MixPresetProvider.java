package com.drunkencod.spice_road.datagen;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.serialization.JsonOps;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.data.CachedOutput;
import net.minecraft.data.DataProvider;
import net.minecraft.data.PackOutput;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;

import com.drunkencod.spice_road.Constants;
import com.drunkencod.spice_road.mix.MixPreset;
import com.drunkencod.spice_road.mix.MixPresetReloadListener;
import com.drunkencod.spice_road.mix.PresetSlot;
import com.drunkencod.spice_road.mix.SpiceMix;
import com.drunkencod.spice_road.spice.ProcessedSpice;
import com.drunkencod.spice_road.spice.Spice;

/**
 * Writes every {@link DefaultMixPreset} as
 * {@code data/spice_road/mix_preset/*.json}, plus a vanilla shapeless recipe
 * per preset (a jar and one batch of its spices) with a recipe book unlock
 * advancement. The recipe exists so recipe viewers like JEI and EMI can show
 * presets: its result carries the same components the jar-and-spices recipe
 * would produce, so whichever of the two a grid matches, the result is the
 * same. A preset's tags are hand-written, so the recipe takes the first item
 * each lists (the raw item for a Spice's variant tag). Raw JSON, so it runs unchanged on both loaders. Every preset is
 * built and written through the real codec, so datagen fails on a preset the
 * game would reject.
 */
public class MixPresetProvider implements DataProvider {

    private static final int MAX_TAG_DEPTH = 8;

    private static final String JAR = ResourceLocation.fromNamespaceAndPath(Constants.MOD_ID, "jar").toString();

    private final PackOutput.PathProvider presetPathProvider;
    private final PackOutput.PathProvider recipePathProvider;
    private final PackOutput.PathProvider advancementPathProvider;

    /** @param output The pack output to write into. */
    public MixPresetProvider(PackOutput output) {
        this.presetPathProvider = output.createPathProvider(PackOutput.Target.DATA_PACK,
                MixPresetReloadListener.DIRECTORY);
        this.recipePathProvider = output.createPathProvider(PackOutput.Target.DATA_PACK, "recipe");
        this.advancementPathProvider = output.createPathProvider(PackOutput.Target.DATA_PACK, "advancement");
    }

    @Override
    public CompletableFuture<?> run(CachedOutput cachedOutput) {
        List<CompletableFuture<?>> writes = new ArrayList<>();
        for (DefaultMixPreset defaultPreset : DefaultMixPreset.values()) {
            ResourceLocation id = ResourceLocation.fromNamespaceAndPath(Constants.MOD_ID, defaultPreset.getId());
            MixPreset preset = defaultPreset.toPreset();
            JsonElement json = MixPreset.CODEC.encodeStart(JsonOps.INSTANCE, preset).getOrThrow(
                    error -> new IllegalStateException("Invalid Mix Preset " + defaultPreset.getId() + ": " + error));
            writes.add(DataProvider.saveStable(cachedOutput, json, presetPathProvider.json(id)));

            Map<Item, Integer> spices = SpiceMix.canonical(representativeSpices(preset));
            ResourceLocation recipeId = id.withPrefix("spice_mix/");
            writes.add(DataProvider.saveStable(cachedOutput, recipe(id, preset, spices),
                    recipePathProvider.json(recipeId)));
            writes.add(DataProvider.saveStable(cachedOutput, unlockAdvancement(recipeId),
                    advancementPathProvider.json(recipeId.withPrefix("recipes/misc/"))));
        }
        return CompletableFuture.allOf(writes.toArray(CompletableFuture[]::new));
    }

    /**
     * @return One batch of the preset as concrete spices, with each tag
     *         replaced by the first item its tag file lists: the raw item for a
     *         generated variant tag, else the hand-written file's first.
     */
    private static Map<Item, Integer> representativeSpices(MixPreset preset) {
        Map<Item, Integer> spices = new LinkedHashMap<>();
        for (PresetSlot slot : preset.slots()) {
            Item item = slot.source().map(own -> own,
                    tag -> variantTagRaw(tag).orElseGet(() -> firstItemOf(tag.location(), 0)));
            spices.merge(item, slot.count(), Integer::sum);
        }
        return spices;
    }

    /**
     * @param tag An item tag.
     * @return The raw item of the Spice whose generated variant tag {@code tag}
     *         is, as those files don't exist yet while datagen runs; empty if
     *         it is none.
     */
    private static Optional<Item> variantTagRaw(TagKey<Item> tag) {
        return Arrays.stream(Spice.values())
                .filter(spice -> tag.equals(ProcessedSpice.variantTag(spice)))
                .findFirst()
                .map(spice -> Spice.getRawById(spice.getId()));
    }

    /**
     * Reads the first item a hand-written item tag lists, from the classpath
     * resources, as tags aren't bound while datagen runs. Nested tags are
     * followed, but only if they are hand-written too.
     *
     * @param tag   The item tag's ID.
     * @param depth How many tags deep this lookup already is.
     * @return The first listed item that exists.
     * @throws IllegalStateException If the tag file is missing or lists no
     *                               registered item.
     */
    private static Item firstItemOf(ResourceLocation tag, int depth) {
        String path = "/data/" + tag.getNamespace() + "/tags/item/" + tag.getPath() + ".json";
        if (depth > MAX_TAG_DEPTH)
            throw new IllegalStateException("Item tag " + tag + " is nested too deeply");
        try (InputStream stream = MixPresetProvider.class.getResourceAsStream(path)) {
            if (stream == null)
                throw new IllegalStateException("Mix Presets need the hand-written item tag " + tag
                        + ", but " + path + " doesn't exist");
            JsonArray values = JsonParser.parseReader(new InputStreamReader(stream, StandardCharsets.UTF_8))
                    .getAsJsonObject().getAsJsonArray("values");
            for (JsonElement value : values) {
                String entry = value.isJsonObject() ? value.getAsJsonObject().get("id").getAsString()
                        : value.getAsString();
                if (entry.startsWith("#")) {
                    try {
                        return firstItemOf(ResourceLocation.parse(entry.substring(1)), depth + 1);
                    } catch (IllegalStateException e) {
                        continue;
                    }
                }
                Optional<Item> item = BuiltInRegistries.ITEM.getOptional(ResourceLocation.parse(entry));
                if (item.isPresent())
                    return item.get();
            }
        } catch (IOException e) {
            throw new IllegalStateException("Couldn't read the item tag " + tag, e);
        }
        throw new IllegalStateException("The item tag " + tag + " lists no registered item");
    }

    /**
     * @return A shapeless recipe of a jar and one batch of the preset's spices,
     *         yielding the preset's Spice Mix with all of its components. For a
     *         preset with tags, the spices are the first of each tag, so the
     *         recipe only matches (and yields exactly what the special recipe
     *         would for) that combination.
     */
    private static JsonObject recipe(ResourceLocation id, MixPreset preset, Map<Item, Integer> spices) {
        JsonArray ingredients = new JsonArray();
        ingredients.add(itemIngredient(JAR));
        spices.forEach((item, count) -> {
            for (int i = 0; i < count; i++)
                ingredients.add(itemIngredient(BuiltInRegistries.ITEM.getKey(item).toString()));
        });

        JsonObject mix = new JsonObject();
        mix.addProperty("preset", id.toString());
        mix.add("spices", SpiceMix.SPICES_CODEC.encodeStart(JsonOps.INSTANCE, spices).getOrThrow());
        JsonObject components = new JsonObject();
        components.add(Constants.MOD_ID + ":spice_mix", mix);
        components.addProperty("minecraft:custom_model_data", preset.customModelData().orElseThrow());
        JsonObject result = new JsonObject();
        result.addProperty("id", ResourceLocation.fromNamespaceAndPath(Constants.MOD_ID, "spice_mix").toString());
        result.addProperty("count", 1);
        result.add("components", components);

        JsonObject recipe = new JsonObject();
        recipe.addProperty("type", "minecraft:crafting_shapeless");
        recipe.addProperty("category", "misc");
        recipe.addProperty("group", Constants.MOD_ID + ":spice_mix");
        recipe.add("ingredients", ingredients);
        recipe.add("result", result);
        return recipe;
    }

    private static JsonObject itemIngredient(String item) {
        JsonObject ingredient = new JsonObject();
        ingredient.addProperty("item", item);
        return ingredient;
    }

    /**
     * @return A {@code minecraft:recipes/root}-parented advancement unlocking the
     *         recipe once a jar is held.
     */
    private static JsonObject unlockAdvancement(ResourceLocation recipeId) {
        JsonObject itemPredicate = new JsonObject();
        itemPredicate.addProperty("items", JAR);
        JsonArray items = new JsonArray();
        items.add(itemPredicate);
        JsonObject conditions = new JsonObject();
        conditions.add("items", items);
        JsonObject hasJar = new JsonObject();
        hasJar.addProperty("trigger", "minecraft:inventory_changed");
        hasJar.add("conditions", conditions);
        JsonObject criteria = new JsonObject();
        criteria.add("has_jar", hasJar);

        JsonArray recipes = new JsonArray();
        recipes.add(recipeId.toString());
        JsonObject rewards = new JsonObject();
        rewards.add("recipes", recipes);

        JsonArray requirement = new JsonArray();
        requirement.add("has_jar");
        JsonArray requirements = new JsonArray();
        requirements.add(requirement);

        JsonObject advancement = new JsonObject();
        advancement.addProperty("parent", "minecraft:recipes/root");
        advancement.add("criteria", criteria);
        advancement.add("requirements", requirements);
        advancement.add("rewards", rewards);
        return advancement;
    }

    @Override
    public String getName() {
        return "Mix Presets";
    }
}
