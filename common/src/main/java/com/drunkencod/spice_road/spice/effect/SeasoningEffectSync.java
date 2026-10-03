package com.drunkencod.spice_road.spice.effect;

import java.util.HashMap;
import java.util.Map;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

import com.drunkencod.spice_road.Constants;

/**
 * Server-to-client payload carrying the whole Seasoning Effect catalog, since
 * {@link SeasoningEffectRegistry} is only filled on the logical server
 * otherwise. Sent to each player on join and to everyone after {@code /reload}.
 * Each loader registers the payload type and forwards received payloads to
 * {@link #handle}.
 *
 * @param entries The catalog, keyed by entry ID.
 */
public record SeasoningEffectSync(Map<ResourceLocation, SeasoningEffectDef> entries) implements CustomPacketPayload {

    /** Payload type ID. */
    public static final CustomPacketPayload.Type<SeasoningEffectSync> TYPE = new CustomPacketPayload.Type<>(
            ResourceLocation.fromNamespaceAndPath(Constants.MOD_ID, "seasoning_effect_sync"));

    /** Network codec. */
    public static final StreamCodec<RegistryFriendlyByteBuf, SeasoningEffectSync> STREAM_CODEC = ByteBufCodecs
            .<RegistryFriendlyByteBuf, ResourceLocation, SeasoningEffectDef, Map<ResourceLocation, SeasoningEffectDef>>map(
                    HashMap::new, ResourceLocation.STREAM_CODEC, SeasoningEffectDef.STREAM_CODEC)
            .map(SeasoningEffectSync::new, SeasoningEffectSync::entries);

    /** @return A payload of the current {@link SeasoningEffectRegistry} contents. */
    public static SeasoningEffectSync current() {
        return new SeasoningEffectSync(SeasoningEffectRegistry.getAll());
    }

    /** Applies a received payload on the client. */
    public void handle() {
        SeasoningEffectRegistry.set(entries);
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
