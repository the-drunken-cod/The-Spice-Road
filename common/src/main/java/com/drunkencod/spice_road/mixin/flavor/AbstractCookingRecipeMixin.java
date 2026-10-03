package com.drunkencod.spice_road.mixin.flavor;

import java.util.List;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.llamalad7.mixinextras.sugar.Local;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.AbstractCookingRecipe;
import net.minecraft.world.item.crafting.SingleRecipeInput;

import com.drunkencod.spice_road.spice.Flavoring;

/**
 * Carries ingredient spices over to cooking outputs (furnace, smoker, blast
 * furnace, campfire, and modded machines running these recipes).
 */
@Mixin(AbstractCookingRecipe.class)
public abstract class AbstractCookingRecipeMixin {

    @ModifyReturnValue(method = "assemble(Lnet/minecraft/world/item/crafting/SingleRecipeInput;Lnet/minecraft/core/HolderLookup$Provider;)Lnet/minecraft/world/item/ItemStack;", at = @At("RETURN"))
    private ItemStack spice_road$applyFlavor(ItemStack result, @Local(argsOnly = true) SingleRecipeInput input) {
        return Flavoring.apply(result, List.of(input.item()));
    }
}
