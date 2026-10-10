package com.drunkencod.spice_road.command;

import com.mojang.brigadier.CommandDispatcher;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;

/**
 * The {@code /spice_road} command: {@code reset} for players' own progress
 * (see {@link SpiceResetCommand}), and {@code admin} for operators' testing and
 * balancing tools (see {@link SpiceAdminCommand}).
 */
public final class SpiceRoadCommand {

    private SpiceRoadCommand() {
    }

    /**
     * Registers the command.
     *
     * @param dispatcher The server's command dispatcher.
     */
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("spice_road")
                .then(SpiceResetCommand.build())
                .then(SpiceAdminCommand.build()));
    }
}
