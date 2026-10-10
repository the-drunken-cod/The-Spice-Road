package com.drunkencod.spice_road.command;

import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;
import java.util.function.Predicate;
import java.util.function.ToIntFunction;

import com.google.common.base.Stopwatch;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.builder.ArgumentBuilder;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;

import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;

import com.drunkencod.spice_road.Constants;
import com.drunkencod.spice_road.config.IConfigHelper;
import com.drunkencod.spice_road.platform.Services;
import com.drunkencod.spice_road.spice.Climate;
import com.drunkencod.spice_road.spice.Spice;
import com.drunkencod.spice_road.spice.Tier;
import com.drunkencod.spice_road.spice.region.HeartOutcome;
import com.drunkencod.spice_road.spice.region.RegionHeartSearch;
import com.drunkencod.spice_road.spice.region.RegionSurvey;

/**
 * {@code /spice_road admin sample_spices [<radius>] [tier|climate|spice <value>]}:
 * surveys the Spice Regions around the source, for balancing worldgen. Reports
 * what became of the cells (how many are spiceless, barren, or have a Region
 * Heart) and, per Spice, how many Hearts it got, its share of all Hearts and
 * how many regions that is per Heart on average. The filter only narrows the
 * Spices listed; shares and spacing stay relative to all Hearts, so a filtered
 * result still compares with the unfiltered one.
 * <p>
 * The walk runs on the server thread, so its radius is capped and its time is
 * budgeted by the server config; it reports what it got if the budget runs
 * out. The default radius is the Spice Map search radius.
 */
final class SpiceSampleCommand {

    /** Most Spices listed in chat. The server log always gets all of them. */
    private static final int MAX_CHAT_ROWS = 15;

    private static final String RADIUS = "radius";

    private static final SimpleCommandExceptionType ERROR_OVERWORLD_ONLY = new SimpleCommandExceptionType(
            Component.translatable("commands.spice_road.overworld_only"));

    private static final DynamicCommandExceptionType ERROR_RADIUS_TOO_LARGE = new DynamicCommandExceptionType(
            max -> Component.translatableEscape("commands.spice_road.sample_spices.radius_too_large", max));

    private SpiceSampleCommand() {
    }

    /** @return The {@code sample_spices} node, to be added below {@code admin}. */
    static LiteralArgumentBuilder<CommandSourceStack> build() {
        RequiredArgumentBuilder<CommandSourceStack, Integer> radius = Commands
                .argument(RADIUS, IntegerArgumentType.integer(1))
                .executes(context -> sample(context.getSource(), IntegerArgumentType.getInteger(context, RADIUS),
                        spice -> true));
        withFilters(radius, context -> IntegerArgumentType.getInteger(context, RADIUS));

        LiteralArgumentBuilder<CommandSourceStack> node = Commands.literal("sample_spices")
                .executes(context -> sample(context.getSource(), defaultRadius(), spice -> true))
                .then(radius);
        withFilters(node, context -> defaultRadius());
        return node;
    }

    /**
     * Adds the {@code tier}, {@code climate} and {@code spice} filters below a
     * node.
     *
     * @param radius Reads the radius to survey with from a command run through
     *               {@code parent}.
     */
    private static void withFilters(ArgumentBuilder<CommandSourceStack, ?> parent,
            ToIntFunction<CommandContext<CommandSourceStack>> radius) {
        parent.then(Commands.literal("tier").then(CommandArguments.tier("tier").executes(context -> {
            Tier tier = CommandArguments.getTier(context, "tier");
            return sample(context.getSource(), radius.applyAsInt(context), spice -> spice.getTier() == tier);
        })));
        parent.then(Commands.literal("climate").then(CommandArguments.climate("climate").executes(context -> {
            Climate climate = CommandArguments.getClimate(context, "climate");
            return sample(context.getSource(), radius.applyAsInt(context), spice -> spice.getClimate() == climate);
        })));
        parent.then(Commands.literal("spice").then(CommandArguments.spice("spice").executes(context -> {
            Spice wanted = CommandArguments.getSpice(context, "spice");
            return sample(context.getSource(), radius.applyAsInt(context), spice -> spice == wanted);
        })));
    }

    private static int defaultRadius() {
        return Services.CONFIG.getSpiceMapSearchRadiusCells();
    }

    /**
     * Surveys and reports.
     *
     * @param radius Radius around the source, in cells.
     * @param filter Which Spices to list.
     * @return How many Region Hearts the survey found.
     * @throws CommandSyntaxException If the radius is over the configured
     *                                limit, or the source isn't in the Overworld.
     */
    private static int sample(CommandSourceStack source, int radius, Predicate<Spice> filter)
            throws CommandSyntaxException {
        IConfigHelper config = Services.CONFIG;
        int maxRadius = config.getCommandSampleMaxRadiusCells();
        if (radius > maxRadius)
            throw ERROR_RADIUS_TOO_LARGE.create(maxRadius);
        ServerLevel level = source.getLevel();
        if (!RegionHeartSearch.hasSpiceRegions(level))
            throw ERROR_OVERWORLD_ONLY.create();

        BlockPos origin = BlockPos.containing(source.getPosition());
        Stopwatch stopwatch = Stopwatch.createStarted();
        RegionSurvey survey = RegionHeartSearch.survey(level, origin, radius,
                TimeUnit.MILLISECONDS.toNanos(config.getCommandSampleTimeBudgetMs()));
        long millis = stopwatch.elapsed().toMillis();

        List<Spice> spices = Arrays.stream(Spice.values()).filter(filter)
                .sorted(Comparator.comparing(Spice::getTier)
                        .thenComparing(Comparator.comparingInt(survey::heartsOf).reversed())
                        .thenComparing(Spice::getId))
                .toList();
        String where = origin.getX() + ", " + origin.getZ();

        source.sendSuccess(() -> Component.translatable("commands.spice_road.sample_spices.header",
                survey.cellsVisited(), where, radius, millis), false);
        Constants.LOG.info("Sampled {} Spice Regions around {} (radius {}) in {} ms", survey.cellsVisited(), where,
                radius, millis);
        if (!survey.isComplete()) {
            source.sendSuccess(() -> Component.translatable("commands.spice_road.sample_spices.partial",
                    survey.cellsVisited(), survey.cellsTotal()).withStyle(ChatFormatting.YELLOW), false);
        }

        for (HeartOutcome outcome : HeartOutcome.values()) {
            String share = percent(survey.cellsVisited() == 0 ? 0D
                    : (double) survey.count(outcome) / survey.cellsVisited());
            source.sendSuccess(() -> Component.translatable("commands.spice_road.sample_spices.outcome",
                    Component.translatable(outcome.getTranslationKey()), survey.count(outcome), share), false);
            Constants.LOG.info("  {}: {} ({})", outcome.getId(), survey.count(outcome), share);
        }

        for (int i = 0; i < spices.size(); i++) {
            Spice spice = spices.get(i);
            Constants.LOG.info("  {} [{}]: {} hearts ({}), 1 per {} regions", spice.getId(),
                    spice.getTier().getSerializedName(), survey.heartsOf(spice), percent(survey.shareOf(spice)),
                    regionsPerHeart(survey, spice));
            if (i < MAX_CHAT_ROWS)
                source.sendSuccess(() -> row(spice, survey), false);
        }
        if (spices.size() > MAX_CHAT_ROWS) {
            int more = spices.size() - MAX_CHAT_ROWS;
            source.sendSuccess(() -> Component.translatable("commands.spice_road.sample_spices.more", more), false);
        }
        return survey.heartCount();
    }

    /** @return A Spice's line in the report, its name colored by its Tier. */
    private static Component row(Spice spice, RegionSurvey survey) {
        Component name = spice.getDisplayName().copy().withStyle(spice.getTier().getRarity().color());
        if (survey.heartsOf(spice) == 0)
            return Component.translatable("commands.spice_road.sample_spices.spice.none", name);
        return Component.translatable("commands.spice_road.sample_spices.spice", name, survey.heartsOf(spice),
                percent(survey.shareOf(spice)), regionsPerHeart(survey, spice));
    }

    private static String regionsPerHeart(RegionSurvey survey, Spice spice) {
        return String.format(Locale.ROOT, "%.1f", survey.cellsPerHeartOf(spice));
    }

    private static String percent(double fraction) {
        return String.format(Locale.ROOT, "%.1f%%", fraction * 100D);
    }
}
