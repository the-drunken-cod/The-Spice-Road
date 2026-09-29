package com.drunkencod.spice_road.registry;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Supplier;

import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.world.item.Item;

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

    /**
     * Every Flavor Contributor that pushed a non-zero share onto this stack's
     * {@link #SPICE_PROFILE}, by item ID. Present exactly when
     * {@link #SPICE_PROFILE} is. Persistent (saved to NBT) and network
     * synchronized.
     */
    public static final Supplier<DataComponentType<Set<Item>>> FLAVOR_CONTRIBUTORS = Services.REGISTRY
            .registerDataComponentType("flavor_contributors",
                    () -> DataComponentType.<Set<Item>>builder()
                            .persistent(BuiltInRegistries.ITEM.byNameCodec().listOf()
                                    .xmap(LinkedHashSet::new, List::copyOf))
                            .networkSynchronized(ByteBufCodecs.collection(LinkedHashSet::new,
                                    ByteBufCodecs.registry(Registries.ITEM)))
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
