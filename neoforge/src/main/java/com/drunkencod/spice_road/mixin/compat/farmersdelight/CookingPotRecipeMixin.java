package com.drunkencod.spice_road.mixin.compat.farmersdelight;

import java.util.ArrayList;
import java.util.List;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.llamalad7.mixinextras.sugar.Local;

import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.wrapper.RecipeWrapper;
import vectorwing.farmersdelight.common.crafting.CookingPotRecipe;

import com.drunkencod.spice_road.spice.Flavoring;

/**
 * Carries ingredient flavor over to Farmer's Delight Cooking Pot meals. The pot passes its whole inventory, so only
 * the ingredient slots are read - the others hold finished meals, the
 * container and the output.
 */
@Mixin(CookingPotRecipe.class)
public abstract class CookingPotRecipeMixin {

    @ModifyReturnValue(method = "assemble(Lnet/neoforged/neoforge/items/wrapper/RecipeWrapper;Lnet/minecraft/core/HolderLookup$Provider;)Lnet/minecraft/world/item/ItemStack;", at = @At("RETURN"))
    private ItemStack spice_road$applyFlavor(ItemStack result, @Local(argsOnly = true) RecipeWrapper input) {
        int slots = Math.min(CookingPotRecipe.INPUT_SLOTS, input.size());
        List<ItemStack> ingredients = new ArrayList<>(slots);
        for (int i = 0; i < slots; i++)
            ingredients.add(input.getItem(i));
        return Flavoring.apply(result, ingredients);
    }
}
