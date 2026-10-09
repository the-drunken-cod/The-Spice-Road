package com.drunkencod.spice_road.compat.viewer.emi;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import dev.emi.emi.api.EmiEntrypoint;
import dev.emi.emi.api.EmiPlugin;
import dev.emi.emi.api.EmiRegistry;
import dev.emi.emi.api.recipe.EmiCraftingRecipe;
import dev.emi.emi.api.recipe.EmiRecipeCategory;
import dev.emi.emi.api.stack.Comparison;
import dev.emi.emi.api.stack.EmiIngredient;
import dev.emi.emi.api.stack.EmiStack;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeType;

import com.drunkencod.spice_road.Constants;
import com.drunkencod.spice_road.compat.viewer.DryingEntry;
import com.drunkencod.spice_road.compat.viewer.SpiceOriginEntry;
import com.drunkencod.spice_road.compat.viewer.SpiceProfileEntry;
import com.drunkencod.spice_road.compat.viewer.ViewerCategory;
import com.drunkencod.spice_road.compat.viewer.ViewerEntry;
import com.drunkencod.spice_road.compat.viewer.ViewerRefresh;
import com.drunkencod.spice_road.rack.WoodRackRecipe;
import com.drunkencod.spice_road.registry.ModDataComponents;
import com.drunkencod.spice_road.registry.ModItems;

/**
 * EMI entry point, found through {@link EmiEntrypoint} on NeoForge and the
 * {@code emi} entrypoint on Fabric. Only loaded when EMI is.
 */
@EmiEntrypoint
public final class SpiceRoadEmiPlugin implements EmiPlugin {

    /**
     * EMI's own reload entry point. Not part of its API, so it is looked up by
     * name; a refresh that can't find it only leaves the entries stale until
     * EMI reloads by itself.
     */
    private static final String RELOAD_MANAGER = "dev.emi.emi.runtime.EmiReloadManager";

    private static final ViewerRefresh.Tracker TRACKER = ViewerRefresh.register("EMI", SpiceRoadEmiPlugin::reload);

    /** One EMI category per {@link ViewerCategory}, kept across reloads. */
    private static final Map<ViewerCategory, EmiRecipeCategory> CATEGORIES = new EnumMap<>(ViewerCategory.class);

    @Override
    public void register(EmiRegistry registry) {
        // Mix Presets and custom mixes differ only in their contents
        registry.setDefaultComparison(EmiStack.of(ModItems.SPICE_MIX.get()),
                Comparison.compareData(stack -> stack.get(ModDataComponents.SPICE_MIX.get())));

        addWoodRackVariants(registry);
        if (ViewerCategory.SPICE_PROFILE.isEnabled())
            addAll(registry, ViewerCategory.SPICE_PROFILE, SpiceProfileEntry.all(TRACKER.begin()));
        if (ViewerCategory.SPICE_ORIGIN.isEnabled())
            addAll(registry, ViewerCategory.SPICE_ORIGIN, SpiceOriginEntry.all());
        if (ViewerCategory.DRYING.isEnabled())
            addAll(registry, ViewerCategory.DRYING, DryingEntry.all(registry.getRecipeManager()));
    }

    /**
     * EMI shows a {@link WoodRackRecipe} once, for its own result, so each other
     * wood's rack gets a crafting recipe of its own with its slab filled in. Their
     * IDs start with {@code /}, which is how EMI tells a synthetic recipe from one
     * missing in the recipe manager.
     */
    private static void addWoodRackVariants(EmiRegistry registry) {
        for (RecipeHolder<?> holder : registry.getRecipeManager().getAllRecipesFor(RecipeType.CRAFTING)) {
            if (!(holder.value() instanceof WoodRackRecipe recipe))
                continue;
            for (WoodRackRecipe.Variant variant : recipe.getVariants()) {
                List<EmiIngredient> inputs = new ArrayList<>();
                for (int i = 0; i < 9; i++)
                    inputs.add(EmiStack.EMPTY);
                for (int i = 0; i < variant.ingredients().size(); i++)
                    inputs.set(i / variant.width() * 3 + i % variant.width(),
                            EmiIngredient.of(variant.ingredients().get(i)));
                registry.addRecipe(new EmiCraftingRecipe(inputs, EmiStack.of(variant.result()),
                        holder.id().withPath(path -> "/" + path + "/" + BuiltInRegistries.ITEM.getKey(variant.slab()).getPath())));
            }
        }
    }

    private static void addAll(EmiRegistry registry, ViewerCategory category, List<? extends ViewerEntry> entries) {
        EmiRecipeCategory emiCategory = category(category);
        registry.addCategory(emiCategory);
        category.getCatalysts().forEach(catalyst -> registry.addWorkstation(emiCategory, EmiStack.of(catalyst)));
        entries.forEach(entry -> registry.addRecipe(new EmiViewerRecipe(entry, emiCategory)));
    }

    private static synchronized EmiRecipeCategory category(ViewerCategory category) {
        return CATEGORIES.computeIfAbsent(category,
                key -> new EmiRecipeCategory(key.getId(), EmiStack.of(key.getIcon())));
    }

    /** Asks EMI to reload, which rebuilds every entry from the current data. */
    private static void reload() {
        try {
            Class.forName(RELOAD_MANAGER).getMethod("reload").invoke(null);
        } catch (ReflectiveOperationException | LinkageError | RuntimeException e) {
            Constants.LOG.warn("Could not ask EMI to reload; the Spice Profile entries stay outdated until it does", e);
        }
    }
}
