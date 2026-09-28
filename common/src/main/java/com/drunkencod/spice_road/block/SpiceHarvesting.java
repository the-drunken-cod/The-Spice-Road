package com.drunkencod.spice_road.block;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

import com.drunkencod.spice_road.platform.Services;
import com.drunkencod.spice_road.spice.Spice;

/**
 * Checks shared by every block whose Spice is harvested by interacting with
 * it: the Harvest Tool Requirement, the Hand-Pick Requirement, and the
 * harvest yield.
 */
public final class SpiceHarvesting {

    private SpiceHarvesting() {
    }

    /**
     * @param spice The Spice being harvested.
     * @param stack The stack held by the harvesting player.
     * @return Whether {@code stack} satisfies {@code spice}'s Harvest Tool
     *         Requirement, i.e. is in its {@link Spice#getHarvestToolTag()}.
     */
    public static boolean isHarvestTool(Spice spice, ItemStack stack) {
        return stack.is(spice.getHarvestToolTag());
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

        return player instanceof ServerPlayer serverPlayer
                && serverPlayer.server.getPlayerList().getPlayer(serverPlayer.getUUID()) == serverPlayer;
    }

    /**
     * @param spice The harvested Spice.
     * @return The number of {@code spice} items a single Spice Plant or Spice
     *         Vine harvest yields: its {@link Spice#getDropAmount()} scaled by
     *         {@code IConfigHelper#getSpicePlantHarvestYieldMultiplier()},
     *         rounded down like the datagenned loot tables.
     */
    public static int getPlantYield(Spice spice) {
        return (int) Math.floor(spice.getDropAmount() * Services.CONFIG.getSpicePlantHarvestYieldMultiplier());
    }
}
