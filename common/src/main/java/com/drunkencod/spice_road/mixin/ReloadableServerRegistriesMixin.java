package com.drunkencod.spice_road.mixin;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import net.minecraft.core.LayeredRegistryAccess;
import net.minecraft.server.RegistryLayer;
import net.minecraft.server.ReloadableServerRegistries;
import net.minecraft.server.packs.resources.ResourceManager;

import com.drunkencod.spice_road.loot.LootInjections;

/**
 * Hands the resource manager loot tables are loaded from to
 * {@link LootInjections}, so it can tell which inject tables exist while
 * the loaders' loot table load hooks run.
 */
@Mixin(ReloadableServerRegistries.class)
public abstract class ReloadableServerRegistriesMixin {

    @Inject(method = "reload", at = @At("HEAD"))
    private static void spice_road$captureResourceManager(LayeredRegistryAccess<RegistryLayer> registries,
            ResourceManager resourceManager, Executor executor,
            CallbackInfoReturnable<CompletableFuture<LayeredRegistryAccess<RegistryLayer>>> cir) {
        LootInjections.setResourceManager(resourceManager);
    }
}
