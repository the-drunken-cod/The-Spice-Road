package com.drunkencod.spice_road.datagen;

import java.util.concurrent.CompletableFuture;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import net.minecraft.data.CachedOutput;
import net.minecraft.data.DataProvider;
import net.minecraft.data.PackOutput;
import net.minecraft.resources.ResourceLocation;

import com.drunkencod.spice_road.Constants;
import com.drunkencod.spice_road.compat.patchouli.FlavorFolio;

/**
 * Datagens the Flavor Folio's recipe: a book and any {@code #spice_road:spices}
 * item, shapeless, giving Patchouli's {@code patchouli:guide_book} with the
 * {@code patchouli:book} component set to the Folio. Carries both loaders'
 * "Patchouli is present" load conditions, so it only loads alongside
 * Patchouli. A plain {@link DataProvider} writing raw JSON, since Patchouli is
 * not a datagen-time dependency.
 */
public class FlavorFolioRecipeProvider implements DataProvider {

    private final PackOutput.PathProvider recipePathProvider;

    public FlavorFolioRecipeProvider(PackOutput output) {
        this.recipePathProvider = output.createPathProvider(PackOutput.Target.DATA_PACK, "recipe");
    }

    @Override
    public CompletableFuture<?> run(CachedOutput cachedOutput) {
        ResourceLocation id = ResourceLocation.fromNamespaceAndPath(Constants.MOD_ID, "compat/patchouli/flavor_folio");
        return DataProvider.saveStable(cachedOutput, recipe(), recipePathProvider.json(id));
    }

    private static JsonObject recipe() {
        JsonObject recipe = CompatRecipeJson.withModLoadedConditions(FlavorFolio.PATCHOULI_MOD_ID);
        recipe.addProperty("type", "minecraft:crafting_shapeless");
        recipe.addProperty("category", "misc");

        JsonArray ingredients = new JsonArray();
        ingredients.add(CompatRecipeJson.itemIngredient(ResourceLocation.withDefaultNamespace("book")));
        ingredients.add(CompatRecipeJson.tagIngredient(Constants.MOD_ID + ":spices"));
        recipe.add("ingredients", ingredients);

        JsonObject result = CompatRecipeJson.itemStack(
                ResourceLocation.fromNamespaceAndPath(FlavorFolio.PATCHOULI_MOD_ID, "guide_book"), 1);
        JsonObject components = new JsonObject();
        components.addProperty(FlavorFolio.PATCHOULI_MOD_ID + ":book", FlavorFolio.BOOK_ID);
        result.add("components", components);
        recipe.add("result", result);
        return recipe;
    }

    @Override
    public String getName() {
        return "Flavor Folio recipe";
    }
}
