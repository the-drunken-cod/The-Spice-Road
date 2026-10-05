package com.drunkencod.spice_road.registry;

import com.mojang.serialization.MapCodec;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.conditions.ICondition;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

import com.drunkencod.spice_road.Constants;
import com.drunkencod.spice_road.compat.CompatRecipeMod;
import com.drunkencod.spice_road.compat.NeoForgeCompatRecipesCondition;

/**
 * Registers the mod's NeoForge load conditions. Fabric registers its own in
 * {@code SpiceRoadMod#onInitialize}.
 */
public final class NeoForgeConditions {

    private static final DeferredRegister<MapCodec<? extends ICondition>> CONDITIONS = DeferredRegister
            .create(NeoForgeRegistries.CONDITION_SERIALIZERS, Constants.MOD_ID);

    static {
        CONDITIONS.register(CompatRecipeMod.CONDITION_ID, () -> NeoForgeCompatRecipesCondition.CODEC);
    }

    private NeoForgeConditions() {
    }

    /**
     * Must be called in the NeoForge mod constructor with the mod event bus.
     *
     * @param eventBus The mod event bus.
     */
    public static void register(IEventBus eventBus) {
        CONDITIONS.register(eventBus);
    }
}
