package com.drunkencod.spice_road.registry;

import java.util.function.Supplier;

import net.minecraft.core.component.DataComponentType;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;

import com.drunkencod.spice_road.grinder.SeasoningSession;
import com.drunkencod.spice_road.mix.SpiceMix;
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

    /**
     * The run in progress on a Spice Grinder - see {@link SeasoningSession}.
     * Persistent (saved to NBT). Synced to clients only as a placeholder that
     * holds just the food (see {@link SeasoningSession#placeholder}), so they
     * can't rebuild the hidden board from it.
     */
    public static final Supplier<DataComponentType<SeasoningSession>> GRINDER_SESSION = Services.REGISTRY
            .registerDataComponentType("grinder_session",
                    () -> DataComponentType.<SeasoningSession>builder()
                            .persistent(SeasoningSession.CODEC)
                            .networkSynchronized(StreamCodec.of(
                                    (buf, session) -> ItemStack.STREAM_CODEC.encode(buf, session.food()),
                                    buf -> SeasoningSession.placeholder(ItemStack.STREAM_CODEC.decode(buf))))
                            .build());

    /**
     * What a Spice Mix holds - see {@link SpiceMix}. Persistent (saved to NBT)
     * and network synchronized.
     */
    public static final Supplier<DataComponentType<SpiceMix>> SPICE_MIX = Services.REGISTRY
            .registerDataComponentType("spice_mix",
                    () -> DataComponentType.<SpiceMix>builder()
                            .persistent(SpiceMix.CODEC)
                            .networkSynchronized(SpiceMix.STREAM_CODEC)
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
