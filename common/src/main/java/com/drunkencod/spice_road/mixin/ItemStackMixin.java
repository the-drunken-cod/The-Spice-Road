package com.drunkencod.spice_road.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;

import com.drunkencod.spice_road.block.StrippedSpiceLogBlock;

/**
 * Exposes the player using an item on a block to
 * {@link StrippedSpiceLogBlock}, whose bark drop happens in {@code onPlace},
 * where that player isn't otherwise known.
 */
@Mixin(ItemStack.class)
public abstract class ItemStackMixin {

    @Inject(method = "useOn", at = @At("HEAD"))
    private void spice_road$trackItemUser(UseOnContext context, CallbackInfoReturnable<InteractionResult> cir) {
        StrippedSpiceLogBlock.setItemUser(context.getPlayer());
    }

    @Inject(method = "useOn", at = @At("RETURN"))
    private void spice_road$clearItemUser(UseOnContext context, CallbackInfoReturnable<InteractionResult> cir) {
        StrippedSpiceLogBlock.setItemUser(null);
    }
}
