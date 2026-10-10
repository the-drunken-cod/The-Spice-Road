package com.drunkencod.spice_road.stats;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import io.netty.buffer.ByteBuf;

import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

import com.drunkencod.spice_road.Constants;

/**
 * Server-to-client payload carrying the Spices the receiving player has found,
 * so the client can show it (e.g. in the Flavor Folio). Sent to a player on
 * join and whenever their set grows. Each loader registers the payload type
 * and forwards received payloads to {@link #handle}.
 *
 * @param spices The item IDs of the found Spices.
 */
public record SpiceFindingsSync(List<String> spices) implements CustomPacketPayload {

    /** Payload type ID. */
    public static final CustomPacketPayload.Type<SpiceFindingsSync> TYPE = new CustomPacketPayload.Type<>(
            ResourceLocation.fromNamespaceAndPath(Constants.MOD_ID, "spice_findings_sync"));

    /** Network codec. */
    public static final StreamCodec<ByteBuf, SpiceFindingsSync> STREAM_CODEC = ByteBufCodecs.STRING_UTF8
            .apply(ByteBufCodecs.list())
            .map(SpiceFindingsSync::new, SpiceFindingsSync::spices);

    /**
     * @param player A connected player.
     * @return A payload of the Spices that player has found.
     */
    public static SpiceFindingsSync of(ServerPlayer player) {
        return new SpiceFindingsSync(SpiceFindings.of(player.server).itemsOf(player.getUUID()).stream()
                .map(ResourceLocation::toString).sorted().toList());
    }

    /** Applies a received payload on the client. */
    public void handle() {
        Set<ResourceLocation> found = new HashSet<>();
        for (String id : spices) {
            ResourceLocation item = SpiceFindings.parseId(id);
            if (item != null)
                found.add(item);
        }
        ClientSpiceFindings.set(found);
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
