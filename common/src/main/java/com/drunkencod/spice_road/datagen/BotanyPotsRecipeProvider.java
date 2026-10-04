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
import net.minecraft.world.level.block.Block;

import com.drunkencod.spice_road.Constants;
import com.drunkencod.spice_road.block.SpicePlants;
import com.drunkencod.spice_road.block.SpiceTree;
import com.drunkencod.spice_road.block.SpiceTrees;
import com.drunkencod.spice_road.block.FruitingSpiceLeavesBlock;
import com.drunkencod.spice_road.spice.Spice;

/**
 * Datagens optional Botany Pots crop recipes for the Spices selected by
 * {@link PotCompatSpices#isCovered(Spice)}:
 * <ul>
 * <li>Spice Plants as {@code botanypots:block_derived_crop}, shown with their
 * own growth stages and dropping their Spice plus a chance of their planting
 * item. Aquatic Spices only accept Botany Pots' water soils.</li>
 * <li>Spice Trees as {@code botanypots:crop}, growing from a sapling into a
 * log topped with leaves, and dropping their Spice, logs and a chance of the
 * sapling.</li>
 * </ul>
 * Each recipe carries both NeoForge and Fabric load conditions, so it's only
 * loaded when Botany Pots is present. Implemented as a plain
 * {@link DataProvider} writing raw JSON, since Botany Pots is not a
 * compile-time dependency. Drops are written out explicitly instead of
 * deriving them from the blocks' loot tables, since those are gated on a
 * connected player and the Spice Region.
 */
public class BotanyPotsRecipeProvider implements DataProvider {

    private static final String MOD_ID = "botanypots";

    /** Botany Pots' item tag of soils that count as water. */
    private static final String WATER_SOIL_TAG = "botanypots:soil/water";

    /** Number of logs a Spice Tree drops, when its log drop chance succeeds. */
    private static final int TREE_LOG_DROP_COUNT = 2;

    /** Chance a Spice Tree drops its logs on harvest. */
    private static final float TREE_LOG_DROP_CHANCE = 0.75F;

    private final PackOutput.PathProvider recipePathProvider;

    public BotanyPotsRecipeProvider(PackOutput output) {
        this.recipePathProvider = output.createPathProvider(PackOutput.Target.DATA_PACK, "recipe");
    }

    @Override
    public CompletableFuture<?> run(CachedOutput cachedOutput) {
        List<CompletableFuture<?>> futures = new ArrayList<>();

        SpicePlants.getRegistered().values().stream()
                .filter(plant -> PotCompatSpices.isCovered(plant.spice()))
                .forEach(plant -> futures.add(DataProvider.saveStable(cachedOutput,
                        plantRecipe(plant.spice(), plant.block().get(), plant.seedItem().get(),
                                plant.productItem().get()),
                        recipePathProvider.json(recipeId(plant.spice())))));

        SpiceTrees.getRegistered().values().stream()
                .filter(tree -> PotCompatSpices.isCovered(tree.getSpice()))
                .forEach(tree -> futures.add(DataProvider.saveStable(cachedOutput, treeRecipe(tree),
                        recipePathProvider.json(recipeId(tree.getSpice())))));

        return CompletableFuture.allOf(futures.toArray(CompletableFuture[]::new));
    }

    private static ResourceLocation recipeId(Spice spice) {
        return ResourceLocation.fromNamespaceAndPath(Constants.MOD_ID, "compat/botanypots/crop/" + spice.getId());
    }

    // #region recipes

    private static JsonObject plantRecipe(Spice spice, Block block, Item seed, Item product) {
        JsonObject recipe = CompatRecipeJson.withModLoadedConditions(MOD_ID);
        recipe.addProperty("type", "botanypots:block_derived_crop");
        recipe.addProperty("block", BuiltInRegistries.BLOCK.getKey(block).toString());
        recipe.add("input", CompatRecipeJson.itemIngredient(BuiltInRegistries.ITEM.getKey(seed)));
        if (spice.isAquatic())
            recipe.add("soil", CompatRecipeJson.tagIngredient(WATER_SOIL_TAG));
        recipe.addProperty("grow_time", PotCompatSpices.growTicks(spice));

        JsonArray items = new JsonArray();
        addSpiceDrops(items, spice, product);
        items.add(drop(BuiltInRegistries.ITEM.getKey(seed), 1, spice.getTier().getSeedDropChance()));
        recipe.add("drops", dropProviders(items));
        return recipe;
    }

    private static JsonObject treeRecipe(SpiceTree tree) {
        Spice spice = tree.getSpice();
        JsonObject recipe = CompatRecipeJson.withModLoadedConditions(MOD_ID);
        recipe.addProperty("type", "botanypots:crop");
        ResourceLocation sapling = BuiltInRegistries.ITEM.getKey(tree.getSaplingItem().get());
        recipe.add("input", CompatRecipeJson.itemIngredient(sapling));
        recipe.addProperty("grow_time", PotCompatSpices.growTicks(spice));
        recipe.add("display", treeDisplay(tree));

        JsonArray items = new JsonArray();
        addSpiceDrops(items, spice, tree.getProductItem().get());
        items.add(drop(BuiltInRegistries.BLOCK.getKey(tree.getLog().get()), TREE_LOG_DROP_COUNT,
                TREE_LOG_DROP_CHANCE));
        items.add(drop(sapling, 1, spice.getTier().getSeedDropChance()));
        recipe.add("drops", dropProviders(items));
        return recipe;
    }

    // #region display

    /**
     * Builds a tree's pot display, as two stacked parts that change with
     * growth progress: the sapling turning into a log below, and bare air
     * turning into leaves, then ripening leaves, above.
     */
    private static JsonArray treeDisplay(SpiceTree tree) {
        ResourceLocation leaves = BuiltInRegistries.BLOCK.getKey(tree.getLeaves().get());
        boolean fruiting = tree.getLeaves().get() instanceof FruitingSpiceLeavesBlock;

        JsonArray trunk = new JsonArray();
        trunk.add(displayState(BuiltInRegistries.BLOCK.getKey(tree.getSapling().get()), null));
        ResourceLocation log = BuiltInRegistries.BLOCK.getKey(tree.getLog().get());
        for (int i = 0; i < Constants.SPICE_TREE_LEAF_GROWTH_STAGES + 1; i++)
            trunk.add(displayState(log, null));

        JsonArray crown = new JsonArray();
        crown.add(displayState(ResourceLocation.withDefaultNamespace("air"), null));
        if (fruiting) {
            for (int age = 0; age <= Constants.SPICE_TREE_LEAF_GROWTH_STAGES; age++)
                crown.add(displayState(leaves, age));
        } else {
            for (int i = 0; i <= Constants.SPICE_TREE_LEAF_GROWTH_STAGES; i++)
                crown.add(displayState(leaves, null));
        }

        JsonArray display = new JsonArray();
        display.add(transitional(trunk));
        display.add(transitional(crown));
        return display;
    }

    private static JsonObject transitional(JsonArray phases) {
        JsonObject state = new JsonObject();
        state.addProperty("type", "botanypots:transitional");
        state.add("phases", phases);
        return state;
    }

    /**
     * @param age The {@code age} property to display, or {@code null} to leave
     *            the block at its default state.
     */
    private static JsonObject displayState(ResourceLocation block, Integer age) {
        JsonObject blockState = new JsonObject();
        blockState.addProperty("block", block.toString());
        if (age != null) {
            JsonObject properties = new JsonObject();
            properties.addProperty("age", Integer.toString(age));
            blockState.add("properties", properties);
        }

        JsonObject state = new JsonObject();
        state.addProperty("type", "botanypots:simple");
        state.add("block_state", blockState);
        return state;
    }

    // #region drops

    /**
     * Adds the Spice drops of one harvest: the whole part of the expected
     * yield as a guaranteed stack, and any fractional part as a single extra
     * item with that chance.
     */
    private static void addSpiceDrops(JsonArray items, Spice spice, Item product) {
        ResourceLocation id = BuiltInRegistries.ITEM.getKey(product);
        double yield = PotCompatSpices.yield(spice);
        int whole = (int) Math.floor(yield);
        float fraction = (float) (yield - whole);

        if (whole > 0)
            items.add(drop(id, whole, 1.0F));
        if (fraction > 0.0F)
            items.add(drop(id, 1, fraction));
    }

    private static JsonObject drop(ResourceLocation item, int count, float chance) {
        JsonObject drop = new JsonObject();
        drop.add("result", CompatRecipeJson.itemStack(item, count));
        if (chance < 1.0F)
            drop.addProperty("chance", chance);
        return drop;
    }

    private static JsonArray dropProviders(JsonArray items) {
        JsonObject provider = new JsonObject();
        provider.addProperty("type", "botanypots:items");
        provider.add("items", items);
        JsonArray providers = new JsonArray();
        providers.add(provider);
        return providers;
    }

    @Override
    public String getName() {
        return "Botany Pots Crop Recipes";
    }
}
