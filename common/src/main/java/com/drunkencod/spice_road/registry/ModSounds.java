package com.drunkencod.spice_road.registry;

import java.util.function.Supplier;

import net.minecraft.sounds.SoundEvent;

import com.drunkencod.spice_road.platform.Services;

/**
 * Custom {@link SoundEvent}s, via {@link Services#REGISTRY}. What they play is
 * defined in {@code sounds.json}.
 */
public final class ModSounds {

    /** An item is hung onto a Drying Rack. */
    public static final Supplier<SoundEvent> DRYING_RACK_ADD_ITEM = Services.REGISTRY
            .registerSoundEvent("block.drying_rack.add_item");

    /** An item is taken from a Drying Rack. */
    public static final Supplier<SoundEvent> DRYING_RACK_REMOVE_ITEM = Services.REGISTRY
            .registerSoundEvent("block.drying_rack.remove_item");

    private ModSounds() {
    }

    /**
     * No-op other than forcing this class (and therefore its static
     * initializers) to load.
     */
    public static void register() {
    }
}
