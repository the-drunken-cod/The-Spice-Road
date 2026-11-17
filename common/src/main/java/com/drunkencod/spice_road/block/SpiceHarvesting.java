package com.drunkencod.spice_road.block;

import org.jetbrains.annotations.Nullable;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;

import com.drunkencod.spice_road.spice.Spice;

/**
 * Checks shared by every block whose Spice is harvested by interacting with
 * it: the Hand-Pick Requirement.
 */
public final class SpiceHarvesting {

    private SpiceHarvesting() {
    }

    /**
     * Checks {@code spice}'s Hand-Pick Requirement. A Spice without one may be
     * harvested by anyone, including simulated players like Create's Deployer.
     * Otherwise, {@code player} must be the exact instance the server's player
     * list holds for its UUID - simulated players are never added to that
     * list, and some reuse their owner's UUID, hence the identity check.
     * <p>
     * Always passes client-side, where the player list isn't available; the
     * server's check is authoritative.
     *
     * @param spice  The Spice being harvested.
     * @param player The harvesting player.
     * @return Whether {@code player} may harvest {@code spice}.
     */
    public static boolean mayHarvest(Spice spice, Player player) {
        if (!spice.requiresHandPick() || player.level().isClientSide())
            return true;

        return isConnectedPlayer(player);
    }

    /**
     * The Hand-Pick Requirement's actual test, also used to gate stat awards.
     * See {@link #mayHarvest} for why identity, rather than a UUID lookup
     * alone, is compared.
     *
     * @param entity The entity that harvested or broke the block, if any.
     * @return Whether {@code entity} is a player the server's live player list
     *         holds, i.e. a genuinely connected one rather than a simulated
     *         player.
     */
    public static boolean isConnectedPlayer(@Nullable Entity entity) {
        return entity instanceof ServerPlayer serverPlayer
                && serverPlayer.server.getPlayerList().getPlayer(serverPlayer.getUUID()) == serverPlayer;
    }
}
