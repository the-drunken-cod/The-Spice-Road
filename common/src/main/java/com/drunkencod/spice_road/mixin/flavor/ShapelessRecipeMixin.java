package com.drunkencod.spice_road.mixin.flavor;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.llamalad7.mixinextras.sugar.Local;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.ShapelessRecipe;

import com.drunkencod.spice_road.spice.Flavoring;

/**
 * Carries ingredient flavor over to shapeless crafting outputs, whoever
 * assembles the recipe (crafting table, crafter, modded autocrafters).
 */
@Mixin(ShapelessRecipe.class)
public abstract class ShapelessRecipeMixin {

    @ModifyReturnValue(method = "assemble(Lnet/minecraft/world/item/crafting/CraftingInput;Lnet/minecraft/core/HolderLookup$Provider;)Lnet/minecraft/world/item/ItemStack;", at = @At("RETURN"))
    private ItemStack spice_road$applyFlavor(ItemStack result, @Local(argsOnly = true) CraftingInput input) {
        return Flavoring.apply(result, input.items());
    }
}
