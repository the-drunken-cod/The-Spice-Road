package com.drunkencod.spice_road.grinder;

import java.util.Optional;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

import com.drunkencod.spice_road.Constants;

/**
 * Client-to-server: what the player wants to do in the open Spice Grinder GUI.
 * The server validates every intent and answers with a {@link GrinderViewPayload}.
 *
 * @param containerId The ID of the open menu, so a stale packet from a closed
 *                    GUI is ignored.
 * @param kind        What the player wants.
 * @param data        The ordinal of the {@code Direction} for {@link Kind#MOVE},
 *                    or how many spices to add or remove at most for the spice
 *                    intents.
 * @param item        The spice item for the spice intents.
 */
public record GrinderIntentPayload(int containerId, Kind kind, int data, Optional<ResourceLocation> item)
        implements CustomPacketPayload {

    /** Payload type ID. */
    public static final CustomPacketPayload.Type<GrinderIntentPayload> TYPE = new CustomPacketPayload.Type<>(
            ResourceLocation.fromNamespaceAndPath(Constants.MOD_ID, "grinder_intent"));

    /** Network codec. */
    public static final StreamCodec<RegistryFriendlyByteBuf, GrinderIntentPayload> STREAM_CODEC = StreamCodec
            .composite(
                    ByteBufCodecs.VAR_INT, GrinderIntentPayload::containerId,
                    ByteBufCodecs.idMapper(i -> Kind.values()[i], Kind::ordinal), GrinderIntentPayload::kind,
                    ByteBufCodecs.VAR_INT, GrinderIntentPayload::data,
                    ByteBufCodecs.optional(ResourceLocation.STREAM_CODEC), GrinderIntentPayload::item,
                    GrinderIntentPayload::new);

    /** What the player wants to do. */
    public enum Kind {
        /** Add one of {@code item} to the draft. */
        ADD_DRAFT_SPICE,
        /** Take one of {@code item} out of the draft. */
        REMOVE_DRAFT_SPICE,
        /** Start the run from the draft. */
        SEASON,
        /** Step in the direction with the ordinal {@code data}. */
        MOVE,
        /** Lock in the cell the pawn stands on. */
        LOCK_IN,
        /** Add one of {@code item} to the run in progress. */
        ADD_SPICE,
        /** Finish the run. */
        ACCEPT,
        /** Finish the run, after being told it would waste the spices. */
        ACCEPT_CONFIRMED
    }

    /**
     * @param player The player who sent it.
     */
    public void handle(ServerPlayer player) {
        if (player.containerMenu instanceof SpiceGrinderMenu menu && menu.containerId == containerId)
            GrinderActions.handle(menu, player, this);
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
