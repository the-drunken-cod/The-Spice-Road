package com.drunkencod.spice_road.spice;

import java.util.HashMap;
import java.util.Map;

import net.minecraft.core.registries.Registries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;

import com.drunkencod.spice_road.Constants;
import com.drunkencod.spice_road.compat.viewer.ViewerRefresh;

/**
 * Server-to-client payload carrying every Spice Item's Default Profile, since
 * the datapack-driven {@link SpiceProfileRegistry} only exists on the
 * logical server otherwise. Sent to each player on join and to everyone after
 * {@code /reload}. Each loader registers the payload type and forwards
 * received payloads to {@link #handle}.
 *
 * @param profiles Every Spice Item's Default Profile.
 */
public record SpiceProfileSync(Map<Item, SpiceProfile> profiles) implements CustomPacketPayload {

    /** Payload type ID. */
    public static final CustomPacketPayload.Type<SpiceProfileSync> TYPE = new CustomPacketPayload.Type<>(
            ResourceLocation.fromNamespaceAndPath(Constants.MOD_ID, "spice_profile_sync"));

    /** Network codec. */
    public static final StreamCodec<RegistryFriendlyByteBuf, SpiceProfileSync> STREAM_CODEC = ByteBufCodecs
            .<RegistryFriendlyByteBuf, Item, SpiceProfile, Map<Item, SpiceProfile>>map(HashMap::new,
                    ByteBufCodecs.registry(Registries.ITEM), SpiceProfile.STREAM_CODEC.cast())
            .map(SpiceProfileSync::new, SpiceProfileSync::profiles);

    /** @return A payload of the current {@link SpiceProfileRegistry} contents. */
    public static SpiceProfileSync current() {
        return new SpiceProfileSync(SpiceProfileRegistry.getAll());
    }

    /** Applies a received payload on the client. */
    public void handle() {
        SpiceProfileRegistry.acceptSync(profiles);
        ViewerRefresh.onDataChanged();
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
