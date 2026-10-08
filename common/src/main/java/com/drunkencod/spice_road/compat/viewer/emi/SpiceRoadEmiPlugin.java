package com.drunkencod.spice_road.compat.viewer.emi;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import dev.emi.emi.api.EmiEntrypoint;
import dev.emi.emi.api.EmiPlugin;
import dev.emi.emi.api.EmiRegistry;
import dev.emi.emi.api.recipe.EmiRecipeCategory;
import dev.emi.emi.api.stack.Comparison;
import dev.emi.emi.api.stack.EmiStack;

import com.drunkencod.spice_road.Constants;
import com.drunkencod.spice_road.compat.viewer.DryingEntry;
import com.drunkencod.spice_road.compat.viewer.SpiceOriginEntry;
import com.drunkencod.spice_road.compat.viewer.SpiceProfileEntry;
import com.drunkencod.spice_road.compat.viewer.ViewerCategory;
import com.drunkencod.spice_road.compat.viewer.ViewerEntry;
import com.drunkencod.spice_road.compat.viewer.ViewerRefresh;
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

        if (ViewerCategory.SPICE_PROFILE.isEnabled())
            addAll(registry, ViewerCategory.SPICE_PROFILE, SpiceProfileEntry.all(TRACKER.begin()));
        if (ViewerCategory.SPICE_ORIGIN.isEnabled())
            addAll(registry, ViewerCategory.SPICE_ORIGIN, SpiceOriginEntry.all());
        if (ViewerCategory.DRYING.isEnabled())
            addAll(registry, ViewerCategory.DRYING, DryingEntry.all(registry.getRecipeManager()));
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
