package com.drunkencod.spice_road.mixin.network;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.minecraft.network.protocol.game.ServerboundSetCreativeModeSlotPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.world.item.ItemStack;

import com.drunkencod.spice_road.grinder.SeasoningSession;
import com.drunkencod.spice_road.registry.ModDataComponents;

/**
 * Keeps a Spice Grinder's run from being overwritten by creative mode. A
 * creative client sends back the stacks it holds, and the Grinder it holds
 * only carries the empty placeholder session (clients never receive the real
 * one), so the server puts the real session of the stack that is in that slot
 * back before the stack is stored.
 */
@Mixin(ServerGamePacketListenerImpl.class)
public abstract class ServerGamePacketListenerImplMixin {

    @Shadow
    public ServerPlayer player;

    @Inject(method = "handleSetCreativeModeSlot", at = @At("HEAD"))
    private void spice_road$keepGrinderSession(ServerboundSetCreativeModeSlotPacket packet, CallbackInfo ci) {
        ItemStack sent = packet.itemStack();
        if (!SeasoningSession.isPlaceholder(sent.get(ModDataComponents.GRINDER_SESSION.get())))
            return;
        int slot = packet.slotNum();
        if (slot < 0 || slot >= player.inventoryMenu.slots.size())
            return;
        SeasoningSession real = player.inventoryMenu.getSlot(slot).getItem()
                .get(ModDataComponents.GRINDER_SESSION.get());
        if (real != null && !SeasoningSession.isPlaceholder(real))
            sent.set(ModDataComponents.GRINDER_SESSION.get(), real);
    }
}
