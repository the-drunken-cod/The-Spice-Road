package com.drunkencod.spice_road.platform.services;

import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;

/**
 * Cross-loader service interface for sending payloads. The payload types
 * themselves are registered by each loader's entry point.
 */
public interface INetworkHelper {

    /**
     * @param player  The player to send to.
     * @param payload A payload registered for the server-to-client direction.
     */
    void sendToPlayer(ServerPlayer player, CustomPacketPayload payload);

    /**
     * Client-side only.
     *
     * @param payload A payload registered for the client-to-server direction.
     */
    void sendToServer(CustomPacketPayload payload);
}
