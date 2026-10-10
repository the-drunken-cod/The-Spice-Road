package com.drunkencod.spice_road.command;

import java.util.Optional;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.Dynamic2CommandExceptionType;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

import com.drunkencod.spice_road.Constants;
import com.drunkencod.spice_road.grinder.GrinderCheatMode;
import com.drunkencod.spice_road.map.SpiceMaps;
import com.drunkencod.spice_road.platform.Services;
import com.drunkencod.spice_road.spice.Climate;
import com.drunkencod.spice_road.spice.Spice;
import com.drunkencod.spice_road.spice.Tier;
import com.drunkencod.spice_road.spice.region.RegionHeartSearch;
import com.drunkencod.spice_road.tooltip.SpiceProfileTooltips;

/**
 * {@code /spice_road admin ...}: operator tools for testing and balancing.
 * {@code give_map} hands a player a Spice Map, and {@code toggle_cheat_mode}
 * gives the running operator unlimited spices in the Spice Grinder. See
 * {@link SpiceSampleCommand} for {@code sample_spices}.
 */
final class SpiceAdminCommand {

    private static final String PLAYER = "player";

    private static final SimpleCommandExceptionType ERROR_OVERWORLD_ONLY = new SimpleCommandExceptionType(
            Component.translatable("commands.spice_road.overworld_only"));

    private static final Dynamic2CommandExceptionType ERROR_MAP_NOT_FOUND = new Dynamic2CommandExceptionType(
            (query, player) -> Component.translatableEscape("commands.spice_road.give_map.not_found", query,
                    player));

    /** Makes a Spice Map for a query, searching from a position out to a radius. */
    @FunctionalInterface
    private interface MapFactory {

        /**
         * @param level  The level to search in.
         * @param origin The position to search from.
         * @param radius Maximum distance to the heart, in blocks.
         * @return The map, or empty if nothing matching is in range.
         */
        Optional<ItemStack> create(ServerLevel level, BlockPos origin, int radius);
    }

    private SpiceAdminCommand() {
    }

    /** @return The {@code admin} node, to be added below the {@code /spice_road} root. */
    static LiteralArgumentBuilder<CommandSourceStack> build() {
        return Commands.literal("admin")
                .requires(source -> source.hasPermission(2))
                .then(Commands.literal("give_map")
                        .then(Commands.argument(PLAYER, EntityArgument.player())
                                .then(Commands.literal("spice")
                                        .then(CommandArguments.spice("spice").executes(context -> {
                                            Spice spice = CommandArguments.getSpice(context, "spice");
                                            return giveMap(context.getSource(),
                                                    EntityArgument.getPlayer(context, PLAYER), spice.getDisplayName(),
                                                    (level, origin, radius) -> SpiceMaps.createForSpice(level, origin,
                                                            radius, spice, SpiceMaps.DEFAULT_ZOOM, null));
                                        })))
                                .then(Commands.literal("tier")
                                        .then(CommandArguments.tier("tier").executes(context -> {
                                            Tier tier = CommandArguments.getTier(context, "tier");
                                            return giveMap(context.getSource(),
                                                    EntityArgument.getPlayer(context, PLAYER),
                                                    SpiceProfileTooltips.tierName(tier),
                                                    (level, origin, radius) -> SpiceMaps.createForTier(level, origin,
                                                            radius, tier, level.getRandom(), SpiceMaps.DEFAULT_ZOOM,
                                                            null));
                                        })))
                                .then(Commands.literal("climate")
                                        .then(CommandArguments.climate("climate").executes(context -> {
                                            Climate climate = CommandArguments.getClimate(context, "climate");
                                            return giveMap(context.getSource(),
                                                    EntityArgument.getPlayer(context, PLAYER),
                                                    climate.getDisplayName(),
                                                    (level, origin, radius) -> SpiceMaps.createForClimate(level,
                                                            origin, radius, climate, SpiceMaps.DEFAULT_ZOOM, null));
                                        })))))
                .then(Commands.literal("toggle_cheat_mode")
                        .executes(context -> toggleCheatMode(context.getSource())))
                .then(SpiceSampleCommand.build());
    }

    /**
     * Gives a player a Spice Map, found from where they stand with the
     * configured Spice Map search radius, like {@code /locate spice}.
     *
     * @param query   The queried Spice's, Tier's or Climate's name, for the
     *                not-found message.
     * @param factory Makes the map.
     * @return {@code 1}.
     * @throws CommandSyntaxException If the player isn't in the Overworld, or
     *                                nothing matching is in range.
     */
    private static int giveMap(CommandSourceStack source, ServerPlayer target, Component query, MapFactory factory)
            throws CommandSyntaxException {
        ServerLevel level = target.serverLevel();
        if (!RegionHeartSearch.hasSpiceRegions(level))
            throw ERROR_OVERWORLD_ONLY.create();

        int radius = RegionHeartSearch.cellsToBlocks(Services.CONFIG.getSpiceMapSearchRadiusCells());
        ItemStack map = factory.create(level, target.blockPosition(), radius)
                .orElseThrow(() -> ERROR_MAP_NOT_FOUND.create(query, target.getDisplayName()));
        Component mapName = map.getDisplayName();
        target.getInventory().placeItemBackInInventory(map);
        source.sendSuccess(() -> Component.translatable("commands.spice_road.give_map.success", mapName,
                target.getDisplayName()), true);
        return 1;
    }

    /**
     * Flips the running player's {@link GrinderCheatMode}.
     *
     * @return {@code 1} if it is on now, {@code 0} if off.
     * @throws CommandSyntaxException If the source isn't a player.
     */
    private static int toggleCheatMode(CommandSourceStack source) throws CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        boolean enabled = GrinderCheatMode.toggle(player.getUUID());
        if (enabled)
            Constants.LOG.info("{} turned on the Spice Grinder cheat mode", player.getGameProfile().getName());
        source.sendSuccess(() -> Component.translatable(
                "commands.spice_road.cheat_mode." + (enabled ? "enabled" : "disabled")), true);
        return enabled ? 1 : 0;
    }
}
