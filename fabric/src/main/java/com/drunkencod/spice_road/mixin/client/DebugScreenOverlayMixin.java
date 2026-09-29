package com.drunkencod.spice_road.mixin.client;

import java.util.List;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.DebugScreenOverlay;

import com.drunkencod.spice_road.client.FabricSpiceRegionDebugOverlay;

/**
 * Reports the final line count of vanilla's left F3 debug column to
 * {@link FabricSpiceRegionDebugOverlay}, since Fabric has no equivalent of
 * NeoForge's {@code CustomizeGuiOverlayEvent.DebugText} to read that column's
 * contents without a mixin.
 */
@Mixin(DebugScreenOverlay.class)
public abstract class DebugScreenOverlayMixin {

    @Inject(method = "renderLines", at = @At("HEAD"))
    private void spice_road$captureLeftColumnLineCount(GuiGraphics guiGraphics, List<String> lines,
            boolean leftSide, CallbackInfo ci) {
        if (leftSide) {
            FabricSpiceRegionDebugOverlay.recordVanillaLeftColumnLineCount(lines.size());
        }
    }
}
