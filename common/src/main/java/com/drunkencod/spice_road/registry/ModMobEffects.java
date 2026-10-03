package com.drunkencod.spice_road.registry;

import net.minecraft.core.Holder;
import net.minecraft.world.effect.MobEffect;

import com.drunkencod.spice_road.effect.ChilledEffect;
import com.drunkencod.spice_road.effect.HotEffect;
import com.drunkencod.spice_road.platform.Services;

/**
 * Custom {@link MobEffect}s, via {@link Services#REGISTRY}.
 */
public final class ModMobEffects {

    /** Seasoning Effect of the fiery pole of the heat axis. */
    public static final Holder<MobEffect> HOT = Services.REGISTRY.registerMobEffect("hot", HotEffect::new);

    /** Seasoning Effect of the cooling pole of the heat axis. */
    public static final Holder<MobEffect> CHILLED = Services.REGISTRY.registerMobEffect("chilled", ChilledEffect::new);

    private ModMobEffects() {
    }

    /**
     * No-op other than forcing this class (and therefore its static
     * initializers) to load.
     */
    public static void register() {
    }
}
