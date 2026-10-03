package com.drunkencod.spice_road.registry;

import java.util.function.Supplier;

import net.minecraft.core.component.DataComponentType;

import com.drunkencod.spice_road.platform.Services;
import com.drunkencod.spice_road.spice.Seasoning;

/**
 * Custom {@link DataComponentType}s, via {@link Services#REGISTRY}.
 */
public final class ModDataComponents {

    /**
     * What a seasoned food carries - see {@link Seasoning}. Its presence marks
     * the food as seasoned. Persistent (saved to NBT) and network
     * synchronized.
     */
    public static final Supplier<DataComponentType<Seasoning>> SEASONING = Services.REGISTRY
            .registerDataComponentType("seasoning",
                    () -> DataComponentType.<Seasoning>builder()
                            .persistent(Seasoning.CODEC)
                            .networkSynchronized(Seasoning.STREAM_CODEC)
                            .build());

    private ModDataComponents() {
    }

    /**
     * No-op other than forcing this class (and therefore
     * {@link #SEASONING}'s static initializer) to load.
     */
    public static void register() {
    }
}
