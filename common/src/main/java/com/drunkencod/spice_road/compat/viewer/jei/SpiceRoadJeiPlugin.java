package com.drunkencod.spice_road.compat.viewer.jei;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.jetbrains.annotations.Nullable;

import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.ingredients.subtypes.ISubtypeInterpreter;
import mezz.jei.api.ingredients.subtypes.UidContext;
import mezz.jei.api.constants.RecipeTypes;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.recipe.IRecipeManager;
import mezz.jei.api.registration.IRecipeCatalystRegistration;
import mezz.jei.api.registration.IRecipeCategoryRegistration;
import mezz.jei.api.registration.IRecipeRegistration;
import mezz.jei.api.registration.ISubtypeRegistration;
import mezz.jei.api.runtime.IJeiRuntime;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.item.crafting.ShapedRecipe;
import net.minecraft.world.item.crafting.ShapedRecipePattern;

import com.drunkencod.spice_road.Constants;
import com.drunkencod.spice_road.compat.viewer.DryingEntry;
import com.drunkencod.spice_road.compat.viewer.SpiceOriginEntry;
import com.drunkencod.spice_road.compat.viewer.SpiceProfileEntry;
import com.drunkencod.spice_road.compat.viewer.ViewerCategory;
import com.drunkencod.spice_road.compat.viewer.ViewerRefresh;
import com.drunkencod.spice_road.mix.SpiceMix;
import com.drunkencod.spice_road.rack.WoodRackRecipe;
import com.drunkencod.spice_road.registry.ModDataComponents;
import com.drunkencod.spice_road.registry.ModItems;

/**
 * JEI entry point, found through {@link JeiPlugin} on NeoForge and the
 * {@code jei_mod_plugin} entrypoint on Fabric. Only loaded when JEI is.
 * <p>
 * EMI skips this plugin, since the mod has a native one for it.
 */
@JeiPlugin
public final class SpiceRoadJeiPlugin implements IModPlugin {

    private static final RecipeType<SpiceProfileEntry> SPICE_PROFILE = type(ViewerCategory.SPICE_PROFILE,
            SpiceProfileEntry.class);
    private static final RecipeType<SpiceOriginEntry> SPICE_ORIGIN = type(ViewerCategory.SPICE_ORIGIN,
            SpiceOriginEntry.class);
    private static final RecipeType<DryingEntry> DRYING = type(ViewerCategory.DRYING, DryingEntry.class);

    private static final ViewerRefresh.Tracker TRACKER = ViewerRefresh.register("JEI",
            SpiceRoadJeiPlugin::refreshProfiles);

    /** The running JEI runtime, or {@code null} while JEI isn't running. */
    private static @Nullable IJeiRuntime runtime;

    /** The Spice Profile entries JEI currently shows, to hide them on a refresh. */
    private static List<SpiceProfileEntry> profileEntries = List.of();

    /** Whether a refresh was requested while JEI had no runtime. */
    private static boolean refreshPending;

    @Override
    public ResourceLocation getPluginUid() {
        return ResourceLocation.fromNamespaceAndPath(Constants.MOD_ID, "jei");
    }

    @Override
    public void registerItemSubtypes(ISubtypeRegistration registration) {
        // Mix Presets and custom mixes differ only in their contents
        registration.registerSubtypeInterpreter(ModItems.SPICE_MIX.get(), new ISubtypeInterpreter<ItemStack>() {
            @Override
            public @Nullable Object getSubtypeData(ItemStack stack, UidContext context) {
                return stack.get(ModDataComponents.SPICE_MIX.get());
            }

            @Override
            public String getLegacyStringSubtypeInfo(ItemStack stack, UidContext context) {
                SpiceMix mix = stack.get(ModDataComponents.SPICE_MIX.get());
                return mix == null ? "" : mix.toString();
            }
        });
    }

    @Override
    public void registerCategories(IRecipeCategoryRegistration registration) {
        IGuiHelper gui = registration.getJeiHelpers().getGuiHelper();
        if (ViewerCategory.SPICE_PROFILE.isEnabled())
            registration.addRecipeCategories(new JeiViewerCategory<>(ViewerCategory.SPICE_PROFILE, SPICE_PROFILE,
                    gui.createDrawableItemStack(ViewerCategory.SPICE_PROFILE.getIcon())));
        if (ViewerCategory.SPICE_ORIGIN.isEnabled())
            registration.addRecipeCategories(new JeiViewerCategory<>(ViewerCategory.SPICE_ORIGIN, SPICE_ORIGIN,
                    gui.createDrawableItemStack(ViewerCategory.SPICE_ORIGIN.getIcon())));
        if (ViewerCategory.DRYING.isEnabled())
            registration.addRecipeCategories(new JeiViewerCategory<>(ViewerCategory.DRYING, DRYING,
                    gui.createDrawableItemStack(ViewerCategory.DRYING.getIcon())));
    }

    @Override
    public void registerRecipes(IRecipeRegistration registration) {
        if (ViewerCategory.SPICE_PROFILE.isEnabled()) {
            profileEntries = SpiceProfileEntry.all(TRACKER.begin());
            registration.addRecipes(SPICE_PROFILE, profileEntries);
        }
        if (ViewerCategory.SPICE_ORIGIN.isEnabled())
            registration.addRecipes(SPICE_ORIGIN, SpiceOriginEntry.all());
        var level = Minecraft.getInstance().level;
        if (level != null)
            addWoodRackVariants(registration, level.getRecipeManager());
        if (ViewerCategory.DRYING.isEnabled() && level != null)
            registration.addRecipes(DRYING, DryingEntry.all(level.getRecipeManager()));
    }

    /**
     * JEI shows a {@link WoodRackRecipe} once, for its own result, so each other
     * wood's rack gets a crafting recipe of its own with its slab filled in.
     */
    private static void addWoodRackVariants(IRecipeRegistration registration, RecipeManager recipes) {
        List<RecipeHolder<CraftingRecipe>> shown = new ArrayList<>();
        for (RecipeHolder<CraftingRecipe> holder : recipes
                .getAllRecipesFor(net.minecraft.world.item.crafting.RecipeType.CRAFTING)) {
            if (!(holder.value() instanceof WoodRackRecipe recipe))
                continue;
            for (WoodRackRecipe.Variant variant : recipe.getVariants()) {
                ShapedRecipePattern pattern = new ShapedRecipePattern(variant.width(), variant.height(),
                        variant.ingredients(), Optional.empty());
                shown.add(new RecipeHolder<>(
                        holder.id().withSuffix("/" + BuiltInRegistries.ITEM.getKey(variant.slab()).getPath()),
                        new ShapedRecipe(recipe.getGroup(), recipe.category(), pattern, variant.result(), false)));
            }
        }
        registration.addRecipes(RecipeTypes.CRAFTING, shown);
    }

    @Override
    public void registerRecipeCatalysts(IRecipeCatalystRegistration registration) {
        addCatalysts(registration, ViewerCategory.SPICE_PROFILE, SPICE_PROFILE);
        addCatalysts(registration, ViewerCategory.SPICE_ORIGIN, SPICE_ORIGIN);
        addCatalysts(registration, ViewerCategory.DRYING, DRYING);
    }

    @Override
    public void onRuntimeAvailable(IJeiRuntime jeiRuntime) {
        runtime = jeiRuntime;
        if (refreshPending) {
            refreshPending = false;
            refreshProfiles();
        }
    }

    @Override
    public void onRuntimeUnavailable() {
        runtime = null;
        profileEntries = List.of();
        TRACKER.reset();
    }

    /**
     * Swaps the shown Spice Profile entries for ones built from the current
     * data, through JEI's runtime recipe manager.
     */
    private static void refreshProfiles() {
        IJeiRuntime current = runtime;
        if (current == null) {
            refreshPending = true;
            return;
        }
        if (!ViewerCategory.SPICE_PROFILE.isEnabled())
            return;
        IRecipeManager recipes = current.getRecipeManager();
        List<SpiceProfileEntry> fresh = SpiceProfileEntry.all(TRACKER.begin());
        if (!profileEntries.isEmpty())
            recipes.hideRecipes(SPICE_PROFILE, profileEntries);
        recipes.addRecipes(SPICE_PROFILE, fresh);
        profileEntries = fresh;
    }

    private static void addCatalysts(IRecipeCatalystRegistration registration, ViewerCategory category,
            RecipeType<?> type) {
        if (category.isEnabled() && !category.getCatalysts().isEmpty())
            registration.addRecipeCatalysts(type, category.getCatalysts().toArray(ItemStack[]::new));
    }

    private static <T> RecipeType<T> type(ViewerCategory category, Class<? extends T> entryClass) {
        return RecipeType.create(Constants.MOD_ID, category.getId().getPath(), entryClass);
    }
}
