package com.drunkencod.spice_road.block;

import org.jetbrains.annotations.Nullable;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

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

        return isConnectedPlayer(player);
    }

    /**
     * The Hand-Pick Requirement's actual test, also used by
     * {@code ConnectedPlayerCondition} to gate break-time loot. See
     * {@link #mayHarvest} for why identity, rather than a UUID lookup alone,
     * is compared.
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

    /**
     * Consumes one durability point of the harvest tool a Spice was broken
     * with, so that breaking a mature plant while holding the tool costs the
     * same as harvesting it by interaction. Does nothing unless the Spice has a
     * Harvest Tool Requirement that {@code tool} satisfies, and the player is
     * allowed to harvest it at all.
     * <p>
     * This is on top of any durability the vanilla mining itself costs, which
     * only applies to blocks that aren't instant-break.
     *
     * @param spice  The Spice whose plant was broken.
     * @param level  The level the plant was broken in.
     * @param player The player who broke it.
     * @param tool   The item the block was broken with - a copy, hence the
     *               live main-hand stack being damaged instead.
     */
    public static void hurtHarvestTool(Spice spice, Level level, Player player, ItemStack tool) {
        if (level.isClientSide() || !spice.requiresHarvestTool() || !isHarvestTool(spice, tool)
                || !mayHarvest(spice, player))
            return;

        player.getMainHandItem().hurtAndBreak(1, player, EquipmentSlot.MAINHAND);
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
