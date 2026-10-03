package com.drunkencod.spice_road.mixin.compat.farmersdelight;

import java.util.List;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.llamalad7.mixinextras.sugar.Local;

import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.wrapper.RecipeWrapper;
import vectorwing.farmersdelight.common.crafting.CuttingBoardRecipe;

import com.drunkencod.spice_road.spice.Flavoring;

/**
 * Carries the flavor of the item on a Farmer's Delight Cutting Board over to
 * everything it's cut into, split across all resulting Flavor Carriers (e.g.
 * a pie's flavor spread over its slices).
 */
@Mixin(CuttingBoardRecipe.class)
public abstract class CuttingBoardRecipeMixin {

    @ModifyReturnValue(method = "rollResults", at = @At("RETURN"))
    private List<ItemStack> spice_road$applyFlavor(List<ItemStack> results,
            @Local(argsOnly = true) RecipeWrapper input) {
        Flavoring.applyAll(results, List.of(input.getItem(0)));
        return results;
    }
}
