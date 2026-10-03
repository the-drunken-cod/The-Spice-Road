package com.drunkencod.spice_road.spice.board;

import java.util.Comparator;
import java.util.Map;
import java.util.OptionalLong;
import java.util.concurrent.ConcurrentHashMap;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.Item;

import com.drunkencod.spice_road.config.IConfigHelper;
import com.drunkencod.spice_road.platform.Services;
import com.drunkencod.spice_road.spice.Seasoning;
import com.drunkencod.spice_road.spice.SpiceProfile;
import com.drunkencod.spice_road.spice.SpiceProfileRegistry;
import com.drunkencod.spice_road.spice.SpiceProfiles;
import com.drunkencod.spice_road.spice.effect.SeasoningEffectRegistry;

/**
 * Plays a food's Seasoning Board with the {@link SeasoningBot}, for foods that
 * recipes produce. The result is a pure function of the food item, its counted
 * contributors, the world seed and the config, so equal inputs give equal,
 * stackable foods.
 */
public final class AutomaticSeasoning {

    /** Boards kept, to not rebuild one for every assembled recipe output. */
    private static final int MAX_CACHED_BOARDS = 512;

    /** Values below this magnitude are floating point noise from cancelling spices. */
    private static final double PROFILE_NOISE = 1e-6;

    private static final Map<BoardKey, SeasoningBoard> BOARDS = new ConcurrentHashMap<>();

    private record BoardKey(long seed, BoardLayout layout) {
    }

    private AutomaticSeasoning() {
    }

    /**
     * Solves the effects of a food.
     *
     * @param output    The food item being seasoned, which fixes its board.
     * @param seasoning The food's Seasoning, with its true contributor amounts.
     * @return {@code seasoning} with its effects replaced by what the bot
     *         earns, or {@code seasoning} itself while no world seed is known
     *         (e.g. a recipe preview on a dedicated server's client), where it
     *         stays display-only.
     */
    public static Seasoning solve(Item output, Seasoning seasoning) {
        OptionalLong worldSeed = SeasoningWorld.seed();
        if (worldSeed.isEmpty())
            return seasoning;
        IConfigHelper config = Services.CONFIG;

        SpiceBudget budget = budgetOf(seasoning, config);
        long seed = BoardSeed.of(worldSeed.getAsLong(), config.getSeasoningBoardSalt(),
                BuiltInRegistries.ITEM.getKey(output).toString());
        BoardLayout layout = BoardLayoutRegistry.get();
        if (BOARDS.size() >= MAX_CACHED_BOARDS)
            BOARDS.clear();
        SeasoningBoard board = BOARDS.computeIfAbsent(new BoardKey(seed, layout),
                key -> BoardGenerator.generate(key.seed(), key.layout()));

        SeasoningRun run = new SeasoningRun(board, rules(config), budget);
        SeasoningBot.play(run);
        return seasoning.withEffects(run.effects());
    }

    /**
     * @param seasoning A food's Seasoning.
     * @param config    The config to read the spice caps from.
     * @return What the food has to spend on its board: the Effective Profile of
     *         its capped contributors' Default Profiles.
     */
    public static SpiceBudget budgetOf(Seasoning seasoning, IConfigHelper config) {
        Comparator<Item> byId = Comparator.comparing(item -> BuiltInRegistries.ITEM.getKey(item));
        Map<Item, Double> counted = SpiceBudget.capAmounts(seasoning.contributors(), byId,
                config.getSeasoningMaxSpicesPerKind(), config.getSeasoningMaxSpices());
        SpiceProfile raw = SpiceProfile.ZERO;
        for (Map.Entry<Item, Double> contributor : counted.entrySet()) {
            raw = raw.add(SpiceProfileRegistry.getDefault(contributor.getKey()).orElse(SpiceProfile.ZERO)
                    .scale(contributor.getValue()));
        }
        return new SpiceBudget(raw.map(value -> Math.abs(value) < PROFILE_NOISE ? 0D : value),
                SpiceProfiles::effective);
    }

    /**
     * @param config The config to read the costs and effect cap from.
     * @return The rules a run is played by.
     */
    public static SeasoningRules rules(IConfigHelper config) {
        return new SeasoningRules(config.getSeasoningStepCost(), config.getSeasoningLockInCost(2),
                config.getSeasoningLockInCost(3), config.getSeasoningLockInCost(4), config.getSeasoningMaxEffects(),
                SeasoningEffectRegistry.catalog());
    }

    /** Forgets every cached board, after the layout changed. */
    static void clearBoards() {
        BOARDS.clear();
    }
}
