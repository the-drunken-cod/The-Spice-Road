package com.drunkencod.spice_road.grinder;

import java.util.function.Consumer;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

import com.drunkencod.spice_road.Constants;

/**
 * Server-to-client: the whole {@link GrinderView} of the open Spice Grinder
 * menu. Each loader registers the payload type and forwards received payloads
 * to {@link #handle}, which hands them to whatever the client set through
 * {@link #setClientHandler}, so this class never refers to client-only classes.
 *
 * @param containerId The ID of the menu the view is for.
 * @param view        What the screen should show.
 */
public record GrinderViewPayload(int containerId, GrinderView view) implements CustomPacketPayload {

    /** Payload type ID. */
    public static final CustomPacketPayload.Type<GrinderViewPayload> TYPE = new CustomPacketPayload.Type<>(
            ResourceLocation.fromNamespaceAndPath(Constants.MOD_ID, "grinder_view"));

    /** Network codec. */
    public static final StreamCodec<RegistryFriendlyByteBuf, GrinderViewPayload> STREAM_CODEC = StreamCodec
            .composite(
                    ByteBufCodecs.VAR_INT, GrinderViewPayload::containerId,
                    GrinderView.STREAM_CODEC, GrinderViewPayload::view,
                    GrinderViewPayload::new);

    private static volatile Consumer<GrinderViewPayload> clientHandler = payload -> {
    };

    /**
     * @param handler What the client does with a received view; set once from
     *                client-only code.
     */
    public static void setClientHandler(Consumer<GrinderViewPayload> handler) {
        clientHandler = handler;
    }

    /** Applies a received payload on the client. */
    public void handle() {
        clientHandler.accept(this);
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
