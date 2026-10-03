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

import com.drunkencod.spice_road.item.SpiceItemTags;
import com.drunkencod.spice_road.platform.Services;
import com.drunkencod.spice_road.registry.ModDataComponents;

/**
 * Requires sneaking to place a stack carrying a
 * {@code spice_road:seasoning} as a block that isn't a {@code BlockEntity},
 * since such a block has nowhere to keep that data and would otherwise
 * silently discard it - notably
 * Farmer's Delight-style placeable food (pie slices, food bowls). Cancelling
 * the placement (returning {@code PASS}) falls through to vanilla's own eat
 * fallback, so the stack is eaten instead.
 * <p>
 * Catches both a stack that's a {@code BlockItem} of a non-{@code BlockEntity}
 * block, and one in {@link SpiceItemTags#VOIDS_FLAVOR_WHEN_PLACED} for items
 * that place as a block through other means (e.g. vanilla
 * {@code minecraft:pumpkin_pie}, made placeable by a Farmer's Delight Mixin
 * rather than by being a {@code BlockItem}). Configurable via
 * {@code IConfigHelper#isSneakRequiredToPlaceFlavoredFood}.
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
        if (!placesAsBlock(stack) || !stack.has(ModDataComponents.SEASONING.get()))
            return;
        cir.setReturnValue(InteractionResult.PASS);
        cir.cancel();
    }

    /**
     * @param stack The stack about to be used on a block.
     * @return Whether {@code stack} would place a block that can't be a
     *         {@code BlockEntity}, and would therefore discard any Seasoning
     *         it carries.
     */
    private static boolean placesAsBlock(ItemStack stack) {
        if (stack.getItem() instanceof BlockItem blockItem)
            return !(blockItem.getBlock() instanceof EntityBlock);
        return stack.is(SpiceItemTags.VOIDS_FLAVOR_WHEN_PLACED);
    }
}
