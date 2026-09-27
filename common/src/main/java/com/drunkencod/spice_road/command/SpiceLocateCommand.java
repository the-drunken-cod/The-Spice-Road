package com.drunkencod.spice_road.command;

import java.util.Arrays;
import java.util.Optional;

import com.google.common.base.Stopwatch;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;

import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentUtils;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.util.Mth;

import com.drunkencod.spice_road.Constants;
import com.drunkencod.spice_road.platform.Services;
import com.drunkencod.spice_road.spice.Spice;
import com.drunkencod.spice_road.spice.region.RegionHeart;
import com.drunkencod.spice_road.spice.region.RegionHeartSearch;

/**
 * {@code /locate spice <spice>}: finds the nearest Region Heart of a Spice.
 * Merged into vanilla's {@code /locate} node, so it shares its permission
 * level and sits next to {@code structure}, {@code biome} and {@code poi}.
 */
public final class SpiceLocateCommand {

    private static final DynamicCommandExceptionType ERROR_INVALID_SPICE = new DynamicCommandExceptionType(
            id -> Component.translatableEscape("commands.spice_road.locate.spice.invalid", id));

    private static final DynamicCommandExceptionType ERROR_NOT_FOUND = new DynamicCommandExceptionType(
            spice -> Component.translatableEscape("commands.spice_road.locate.spice.not_found", spice));

    private SpiceLocateCommand() {
    }

    /**
     * Registers the command.
     *
     * @param dispatcher The server's command dispatcher.
     */
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("locate")
                .requires(source -> source.hasPermission(2))
                .then(Commands.literal("spice")
                        .then(Commands.argument("spice", StringArgumentType.word())
                                .suggests((context, builder) -> SharedSuggestionProvider.suggest(
                                        Arrays.stream(Spice.values()).map(Spice::getId), builder))
                                .executes(context -> locate(context.getSource(),
                                        StringArgumentType.getString(context, "spice"))))));
    }

    private static int locate(CommandSourceStack source, String spiceId) throws CommandSyntaxException {
        Spice spice = Spice.byId(spiceId);
        if (spice == null)
            throw ERROR_INVALID_SPICE.create(spiceId);

        BlockPos origin = BlockPos.containing(source.getPosition());
        Stopwatch stopwatch = Stopwatch.createStarted();
        Optional<RegionHeart> heart = RegionHeartSearch.findNearest(source.getLevel(), origin,
                Services.CONFIG.getSpiceMapSearchRadius(), spice);
        stopwatch.stop();
        if (heart.isEmpty())
            throw ERROR_NOT_FOUND.create(spice.getDisplayName());

        BlockPos pos = heart.get().pos();
        double deltaX = pos.getX() - origin.getX();
        double deltaZ = pos.getZ() - origin.getZ();
        int distance = Mth.floor(Math.sqrt((deltaX * deltaX) + (deltaZ * deltaZ)));
        Component coordinates = ComponentUtils
                .wrapInSquareBrackets(Component.translatable("chat.coordinates", pos.getX(), "~", pos.getZ()))
                .withStyle(style -> style.withColor(ChatFormatting.GREEN)
                        .withClickEvent(new ClickEvent(ClickEvent.Action.SUGGEST_COMMAND,
                                "/tp @s " + pos.getX() + " ~ " + pos.getZ()))
                        .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT,
                                Component.translatable("chat.coordinates.tooltip"))));
        source.sendSuccess(() -> Component.translatable("commands.spice_road.locate.spice.success",
                spice.getDisplayName(), coordinates, distance), false);
        Constants.LOG.info("Locating spice {} took {} ms", spice.getId(), stopwatch.elapsed().toMillis());
        return distance;
    }
}
