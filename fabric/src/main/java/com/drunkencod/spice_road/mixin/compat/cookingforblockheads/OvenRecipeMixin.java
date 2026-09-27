package com.drunkencod.spice_road.mixin.compat.cookingforblockheads;

import java.util.List;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.llamalad7.mixinextras.sugar.Local;

import net.blay09.mods.cookingforblockheads.recipe.OvenRecipe;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.SingleRecipeInput;

import com.drunkencod.spice_road.spice.Flavoring;

/**
 * Carries ingredient flavor over to Cooking for Blockheads Oven outputs,
 * applying the cooking variance.
 */
@Mixin(OvenRecipe.class)
public abstract class OvenRecipeMixin {

    @ModifyReturnValue(method = "assemble(Lnet/minecraft/world/item/crafting/SingleRecipeInput;Lnet/minecraft/core/HolderLookup$Provider;)Lnet/minecraft/world/item/ItemStack;", at = @At("RETURN"))
    private ItemStack spice_road$applyFlavor(ItemStack result, @Local(argsOnly = true) SingleRecipeInput input) {
        return Flavoring.apply(result, List.of(input.item()), true);
    }
}
