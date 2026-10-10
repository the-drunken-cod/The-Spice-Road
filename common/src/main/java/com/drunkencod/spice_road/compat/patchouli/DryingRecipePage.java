package com.drunkencod.spice_road.compat.patchouli;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.level.Level;

import com.drunkencod.spice_road.compat.viewer.DryingEntry;
import com.drunkencod.spice_road.compat.viewer.ViewerLayout;
import com.drunkencod.spice_road.drying.DryingRecipe;
import com.drunkencod.spice_road.registry.ModRecipeTypes;

/**
 * Page type {@code spice_road:drying_recipe}: up to two Drying Rack recipes
 * with their results' chances and drying times at the configured speed, as in
 * the Drying viewer category.
 * <p>
 * JSON: {@code "recipe": "<recipe id>"} and optionally {@code "recipe2"}, e.g.
 * {@code "spice_road:drying/dried_clove"}.
 */
public class DryingRecipePage extends ViewerEntryPage {

    private String recipe;
    private String recipe2;

    @Override
    protected List<ViewerLayout> layouts(Level level, int width) {
        List<DryingEntry> entries = new ArrayList<>();
        for (String id : new String[] { recipe, recipe2 }) {
            ResourceLocation location = id == null ? null : ResourceLocation.tryParse(id);
            if (location == null)
                continue;
            level.getRecipeManager().byKey(location)
                    .filter(holder -> holder.value().getType() == ModRecipeTypes.DRYING.get())
                    .ifPresent(holder -> entries.add(new DryingEntry(cast(holder))));
        }
        return layoutAll(entries, width);
    }

    @SuppressWarnings("unchecked")
    private static RecipeHolder<DryingRecipe> cast(RecipeHolder<?> holder) {
        return (RecipeHolder<DryingRecipe>) holder;
    }

    @Override
    protected Component missingText() {
        return Component.translatable("spice_road.guide.unknown_recipe", String.valueOf(recipe));
    }
}
