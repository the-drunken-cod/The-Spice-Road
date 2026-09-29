package com.drunkencod.spice_road.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.block.EntityBlock;

import com.drunkencod.spice_road.platform.Services;
import com.drunkencod.spice_road.spice.SpiceProfiles;

/**
 * Requires sneaking to place a stack carrying a {@code spice_road:spice_profile}
 * override as a block that isn't a {@code BlockEntity}, since such a block has
 * nowhere to keep that data and would otherwise silently discard it - notably
 * Farmer's Delight-style placeable food (pie slices, food bowls). Configurable
 * via {@code IConfigHelper#isSneakRequiredToPlaceFlavoredFood}.
 */
@Mixin(ItemStack.class)
public abstract class FlavoredFoodPlacementMixin {

    @Inject(method = "useOn", at = @At("HEAD"), cancellable = true)
    private void spice_road$requireSneakToPlaceFlavoredFood(UseOnContext context,
            CallbackInfoReturnable<InteractionResult> cir) {
        ItemStack stack = (ItemStack) (Object) this;
        Player player = context.getPlayer();
        if (player == null || player.isShiftKeyDown() || !Services.CONFIG.isSneakRequiredToPlaceFlavoredFood())
            return;
        if (!(stack.getItem() instanceof BlockItem blockItem) || blockItem.getBlock() instanceof EntityBlock)
            return;
        if (!SpiceProfiles.hasOverride(stack))
            return;
        cir.setReturnValue(InteractionResult.PASS);
        cir.cancel();
    }
}
