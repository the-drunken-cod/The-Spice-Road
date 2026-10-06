package com.drunkencod.spice_road.registry;

import java.util.function.Supplier;

import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.SimpleCraftingRecipeSerializer;

import com.drunkencod.spice_road.mix.SpiceMixRecipe;
import com.drunkencod.spice_road.platform.Services;

/**
 * Custom {@link RecipeSerializer}s, via {@link Services#REGISTRY}.
 */
public final class ModRecipeSerializers {

    /**
     * Crafting a Spice Mix from a jar and loose spices - see
     * {@link SpiceMixRecipe}. Recipe type {@code spice_road:crafting_special_spice_mix}.
     */
    public static final Supplier<RecipeSerializer<SpiceMixRecipe>> SPICE_MIX = Services.REGISTRY
            .registerRecipeSerializer("crafting_special_spice_mix",
                    () -> new SimpleCraftingRecipeSerializer<>(SpiceMixRecipe::new));

    private ModRecipeSerializers() {
    }

    /**
     * No-op other than forcing this class (and therefore its static
     * initializers) to load.
     */
    public static void register() {
    }
}
