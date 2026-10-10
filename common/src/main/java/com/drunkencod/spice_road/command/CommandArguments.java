package com.drunkencod.spice_road.command;

import java.util.Arrays;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.network.chat.Component;

import com.drunkencod.spice_road.spice.Climate;
import com.drunkencod.spice_road.spice.Spice;
import com.drunkencod.spice_road.spice.Tier;

/**
 * Command arguments for the mod's enums. They are plain words with suggestions
 * rather than custom argument types, so a client without the mod can still
 * join a server that has the commands.
 */
final class CommandArguments {

    private static final DynamicCommandExceptionType ERROR_INVALID_SPICE = new DynamicCommandExceptionType(
            id -> Component.translatableEscape("commands.spice_road.invalid.spice", id));

    private static final DynamicCommandExceptionType ERROR_INVALID_TIER = new DynamicCommandExceptionType(
            id -> Component.translatableEscape("commands.spice_road.invalid.tier", id));

    private static final DynamicCommandExceptionType ERROR_INVALID_CLIMATE = new DynamicCommandExceptionType(
            id -> Component.translatableEscape("commands.spice_road.invalid.climate", id));

    private CommandArguments() {
    }

    /**
     * @param name The argument's name.
     * @return An argument taking a {@link Spice} ID.
     */
    static RequiredArgumentBuilder<CommandSourceStack, String> spice(String name) {
        return Commands.argument(name, StringArgumentType.word())
                .suggests((context, builder) -> SharedSuggestionProvider.suggest(
                        Arrays.stream(Spice.values()).map(Spice::getId), builder));
    }

    /**
     * @param name The argument's name.
     * @return An argument taking a {@link Tier} name.
     */
    static RequiredArgumentBuilder<CommandSourceStack, String> tier(String name) {
        return Commands.argument(name, StringArgumentType.word())
                .suggests((context, builder) -> SharedSuggestionProvider.suggest(
                        Arrays.stream(Tier.values()).map(Tier::getSerializedName), builder));
    }

    /**
     * @param name The argument's name.
     * @return An argument taking a {@link Climate} ID.
     */
    static RequiredArgumentBuilder<CommandSourceStack, String> climate(String name) {
        return Commands.argument(name, StringArgumentType.word())
                .suggests((context, builder) -> SharedSuggestionProvider.suggest(
                        Arrays.stream(Climate.values()).map(Climate::getId), builder));
    }

    /**
     * @param context The command's context.
     * @param name    The name of an argument made by {@link #spice}.
     * @return The Spice it holds.
     * @throws CommandSyntaxException If there is no such Spice.
     */
    static Spice getSpice(CommandContext<CommandSourceStack> context, String name) throws CommandSyntaxException {
        String id = StringArgumentType.getString(context, name);
        Spice spice = Spice.byId(id);
        if (spice == null)
            throw ERROR_INVALID_SPICE.create(id);
        return spice;
    }

    /**
     * @param context The command's context.
     * @param name    The name of an argument made by {@link #tier}.
     * @return The Tier it holds.
     * @throws CommandSyntaxException If there is no such Tier.
     */
    static Tier getTier(CommandContext<CommandSourceStack> context, String name) throws CommandSyntaxException {
        String id = StringArgumentType.getString(context, name);
        Tier tier = Tier.byName(id);
        if (tier == null)
            throw ERROR_INVALID_TIER.create(id);
        return tier;
    }

    /**
     * @param context The command's context.
     * @param name    The name of an argument made by {@link #climate}.
     * @return The Climate it holds.
     * @throws CommandSyntaxException If there is no such Climate.
     */
    static Climate getClimate(CommandContext<CommandSourceStack> context, String name)
            throws CommandSyntaxException {
        String id = StringArgumentType.getString(context, name);
        Climate climate = Climate.byId(id);
        if (climate == null)
            throw ERROR_INVALID_CLIMATE.create(id);
        return climate;
    }
}
