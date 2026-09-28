package com.drunkencod.spice_road.command;

import java.util.Arrays;
import java.util.Optional;
import java.util.function.Predicate;

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
import com.drunkencod.spice_road.spice.Climate;
import com.drunkencod.spice_road.spice.Spice;
import com.drunkencod.spice_road.spice.region.RegionHeart;
import com.drunkencod.spice_road.spice.region.RegionHeartSearch;

/**
 * {@code /locate spice <spice>} and {@code /locate spice_climate <climate>}:
 * finds the nearest Region Heart matching a Spice, or matching any Spice of
 * a {@link Climate}. Merged into vanilla's {@code /locate} node, so both
 * share its permission level and sit next to {@code structure}, {@code biome}
 * and {@code poi}.
 */
public final class SpiceLocateCommand {

    // #region errors

    private static final DynamicCommandExceptionType ERROR_INVALID_SPICE = new DynamicCommandExceptionType(
            id -> Component.translatableEscape("commands.spice_road.locate.spice.invalid", id));

    private static final DynamicCommandExceptionType ERROR_SPICE_NOT_FOUND = new DynamicCommandExceptionType(
            name -> Component.translatableEscape("commands.spice_road.locate.spice.not_found", name));

    private static final DynamicCommandExceptionType ERROR_INVALID_CLIMATE = new DynamicCommandExceptionType(
            id -> Component.translatableEscape("commands.spice_road.locate.spice_climate.invalid", id));

    private static final DynamicCommandExceptionType ERROR_CLIMATE_NOT_FOUND = new DynamicCommandExceptionType(
            name -> Component.translatableEscape("commands.spice_road.locate.spice_climate.not_found", name));

    private SpiceLocateCommand() {
    }

    // #region register

    /**
     * Registers the commands.
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
                                .executes(context -> locateSpice(context.getSource(),
                                        StringArgumentType.getString(context, "spice")))))
                .then(Commands.literal("spice_climate")
                        .then(Commands.argument("climate", StringArgumentType.word())
                                .suggests((context, builder) -> SharedSuggestionProvider.suggest(
                                        Arrays.stream(Climate.values()).map(Climate::getId), builder))
                                .executes(context -> locateClimate(context.getSource(),
                                        StringArgumentType.getString(context, "climate"))))));
    }

    // #region handlers

    private static int locateSpice(CommandSourceStack source, String spiceId) throws CommandSyntaxException {
        Spice spice = Spice.byId(spiceId);
        if (spice == null)
            throw ERROR_INVALID_SPICE.create(spiceId);

        return locateNearest(source, heart -> heart.spice() == spice, spice.getDisplayName(),
                "spice " + spice.getId(), ERROR_SPICE_NOT_FOUND, "commands.spice_road.locate.spice.success");
    }

    private static int locateClimate(CommandSourceStack source, String climateId) throws CommandSyntaxException {
        Climate climate = Climate.byId(climateId);
        if (climate == null)
            throw ERROR_INVALID_CLIMATE.create(climateId);

        return locateNearest(source, heart -> heart.climate() == climate, climate.getDisplayName(),
                "climate " + climate.getId(), ERROR_CLIMATE_NOT_FOUND,
                "commands.spice_road.locate.spice_climate.success");
    }

    /**
     * Finds the nearest {@link RegionHeart} matching {@code matcher} and
     * reports it to {@code source}, shared by both {@code /locate spice} and
     * {@code /locate spice_climate}.
     *
     * @param matcher         Filters candidate Region Hearts.
     * @param displayName     The queried Spice's or Climate's translatable
     *                        name, interpolated into the not-found/success
     *                        messages.
     * @param logDescription  Plain-text description of the query for the log
     *                        line, e.g. {@code "spice cinnamon"}.
     * @param notFoundError   Thrown, with {@code displayName}, if no Region
     *                        Heart matches within the search radius.
     * @param successKey      Translation key of the success message, taking
     *                        {@code displayName}, the coordinates component
     *                        and the distance as arguments.
     * @return The distance, in blocks, to the found Region Heart.
     */
    private static int locateNearest(CommandSourceStack source, Predicate<RegionHeart> matcher,
            Component displayName, String logDescription, DynamicCommandExceptionType notFoundError,
            String successKey) throws CommandSyntaxException {
        BlockPos origin = BlockPos.containing(source.getPosition());
        Stopwatch stopwatch = Stopwatch.createStarted();
        Optional<RegionHeart> heart = RegionHeartSearch.findNearest(source.getLevel(), origin,
                RegionHeartSearch.cellsToBlocks(Services.CONFIG.getSpiceMapSearchRadiusCells()), matcher);
        stopwatch.stop();
        if (heart.isEmpty())
            throw notFoundError.create(displayName);

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
        source.sendSuccess(() -> Component.translatable(successKey, displayName, coordinates, distance), false);
        Constants.LOG.info("Locating {} took {} ms", logDescription, stopwatch.elapsed().toMillis());
        return distance;
    }
}
