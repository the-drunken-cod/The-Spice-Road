package com.drunkencod.spice_road.registry;

import java.util.function.Supplier;

import net.minecraft.core.component.DataComponentType;

import com.drunkencod.spice_road.platform.Services;
import com.drunkencod.spice_road.spice.SpiceProfile;

/**
 * Custom {@link DataComponentType}s, via {@link Services#REGISTRY}.
 */
public final class ModDataComponents {

    /**
     * Per-stack override of a spice's {@link SpiceProfile}, taking priority
     * over the datapack-registered default for the stack's item - see
     * {@code SpiceProfiles#get}. Persistent (saved to NBT) and network
     * synchronized.
     */
    public static final Supplier<DataComponentType<SpiceProfile>> SPICE_PROFILE = Services.REGISTRY
            .registerDataComponentType("spice_profile",
                    () -> DataComponentType.<SpiceProfile>builder()
                            .persistent(SpiceProfile.CODEC)
                            .networkSynchronized(SpiceProfile.STREAM_CODEC)
                            .build());

    private ModDataComponents() {
    }

    /**
     * No-op other than forcing this class (and therefore
     * {@link #SPICE_PROFILE}'s static initializer) to load.
     */
    public static void register() {
    }
}
