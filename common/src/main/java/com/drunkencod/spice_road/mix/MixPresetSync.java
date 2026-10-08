package com.drunkencod.spice_road.mix;

import java.util.HashMap;
import java.util.Map;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

import com.drunkencod.spice_road.Constants;
import com.drunkencod.spice_road.compat.viewer.ViewerRefresh;

/**
 * Server-to-client payload carrying every Mix Preset, since
 * {@link MixPresetRegistry} is only filled on the logical server otherwise. The
 * client needs them for the creative tab. Sent to each player on join and to
 * everyone after {@code /reload}. Each loader registers the payload type and
 * forwards received payloads to {@link #handle}.
 *
 * @param presets The presets, keyed by ID.
 */
public record MixPresetSync(Map<ResourceLocation, MixPreset> presets) implements CustomPacketPayload {

    /** Payload type ID. */
    public static final CustomPacketPayload.Type<MixPresetSync> TYPE = new CustomPacketPayload.Type<>(
            ResourceLocation.fromNamespaceAndPath(Constants.MOD_ID, "mix_preset_sync"));

    /** Network codec. */
    public static final StreamCodec<RegistryFriendlyByteBuf, MixPresetSync> STREAM_CODEC = ByteBufCodecs
            .<RegistryFriendlyByteBuf, ResourceLocation, MixPreset, Map<ResourceLocation, MixPreset>>map(
                    HashMap::new, ResourceLocation.STREAM_CODEC, MixPreset.STREAM_CODEC)
            .map(MixPresetSync::new, MixPresetSync::presets);

    /** @return A payload of the current {@link MixPresetRegistry} contents. */
    public static MixPresetSync current() {
        return new MixPresetSync(MixPresetRegistry.getAll());
    }

    /** Applies a received payload on the client. */
    public void handle() {
        MixPresetRegistry.set(presets);
        ViewerRefresh.onDataChanged();
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
