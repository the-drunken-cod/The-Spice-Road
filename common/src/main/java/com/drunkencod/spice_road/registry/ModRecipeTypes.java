package com.drunkencod.spice_road.registry;

import java.util.function.Supplier;

import net.minecraft.world.item.crafting.RecipeType;

import com.drunkencod.spice_road.drying.DryingRecipe;
import com.drunkencod.spice_road.platform.Services;

/**
 * Custom {@link RecipeType}s, via {@link Services#REGISTRY}.
 */
public final class ModRecipeTypes {

    /** Drying an item on a Drying Rack, {@code spice_road:drying}. */
    public static final Supplier<RecipeType<DryingRecipe>> DRYING = Services.REGISTRY.registerRecipeType("drying");

    private ModRecipeTypes() {
    }

    /**
     * No-op other than forcing this class (and therefore its static
     * initializers) to load.
     */
    public static void register() {
    }
}
