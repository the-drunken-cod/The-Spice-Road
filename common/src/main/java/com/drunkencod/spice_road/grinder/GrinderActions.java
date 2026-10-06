package com.drunkencod.spice_road.grinder;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Supplier;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import com.drunkencod.spice_road.config.IConfigHelper;
import com.drunkencod.spice_road.grinder.GrinderView.Event;
import com.drunkencod.spice_road.platform.Services;
import com.drunkencod.spice_road.registry.ModDataComponents;
import com.drunkencod.spice_road.registry.ModItems;
import com.drunkencod.spice_road.spice.FlavorAxis;
import com.drunkencod.spice_road.spice.Seasoning;
import com.drunkencod.spice_road.spice.SpiceProfile;
import com.drunkencod.spice_road.spice.SpiceProfiles;
import com.drunkencod.spice_road.spice.board.AutomaticSeasoning;
import com.drunkencod.spice_road.spice.board.BoardGeometry;
import com.drunkencod.spice_road.spice.board.Cell;
import com.drunkencod.spice_road.spice.board.CellKind;
import com.drunkencod.spice_road.spice.board.BoardLayoutRegistry;
import com.drunkencod.spice_road.spice.board.BoardSeed;
import com.drunkencod.spice_road.spice.board.CellView;
import com.drunkencod.spice_road.spice.board.CellViews;
import com.drunkencod.spice_road.spice.board.Direction;
import com.drunkencod.spice_road.spice.board.PointsLedger;
import com.drunkencod.spice_road.spice.board.SeasoningBoard;
import com.drunkencod.spice_road.spice.board.SeasoningRun;
import com.drunkencod.spice_road.spice.effect.SeasoningEffectRegistry;
import com.drunkencod.spice_road.spice.effect.SeasoningEffects;
import com.drunkencod.spice_road.stats.ModStats;

/**
 * The server side of the Spice Grinder GUI: validates and commits every
 * {@link GrinderIntentPayload} and builds the {@link GrinderView} the player
 * sees. Nothing here trusts the client; a refused intent changes nothing and
 * answers with {@link Event#REFUSED}.
 */
public final class GrinderActions {

    private static final double STEP_EPSILON = 1e-9;
    private static final int MAX_STEPS_SHOWN = 9;

    /** Most spices a single shift-click adds or removes. */
    public static final int BATCH_SIZE = 8;

    private GrinderActions() {
    }

    /**
     * Carries out an intent of the player who has {@code menu} open.
     *
     * @param menu   The player's open Spice Grinder menu.
     * @param player The player.
     * @param intent What they want to do.
     */
    public static void handle(SpiceGrinderMenu menu, ServerPlayer player, GrinderIntentPayload intent) {
        int amount = Math.clamp(intent.data(), 1, BATCH_SIZE);
        Event event = switch (intent.kind()) {
            case ADD_DRAFT_SPICE -> spiceOf(intent)
                    .map(spice -> repeat(amount, () -> addToDraft(menu, player, spice))).orElse(Event.REFUSED);
            case REMOVE_DRAFT_SPICE -> spiceOf(intent)
                    .map(spice -> repeat(amount, () -> removeFromDraft(menu, spice))).orElse(Event.REFUSED);
            case SEASON -> season(menu, player);
            case MOVE -> move(menu, player, intent.data());
            case LOCK_IN -> lockIn(menu, player);
            case ADD_SPICE -> spiceOf(intent).map(spice -> repeat(amount, () -> addToRun(menu, player, spice)))
                    .orElse(Event.REFUSED);
            case ACCEPT -> accept(menu, player, false);
            case ACCEPT_CONFIRMED -> accept(menu, player, true);
        };
        if (event != null)
            menu.syncView(event);
    }

    /**
     * Runs a single-spice action up to {@code times}, stopping at the first
     * refusal.
     *
     * @param times  How many times to try.
     * @param action Adds or removes one spice.
     * @return {@link Event#REFUSED} if not even the first try worked, otherwise
     *         the event of the last one that did.
     */
    private static Event repeat(int times, Supplier<Event> action) {
        Event result = Event.REFUSED;
        for (int i = 0; i < times; i++) {
            Event event = action.get();
            if (event == Event.REFUSED)
                break;
            result = event;
        }
        return result;
    }

    // #region Draft

    private static Event addToDraft(SpiceGrinderMenu menu, ServerPlayer player, GrinderSpice spice) {
        if (menu.session() != null)
            return Event.REFUSED;
        int count = menu.draft().getOrDefault(spice, 0);
        ItemStack food = menu.food();
        if (food.isEmpty() || !SpiceGrinderMenu.isSeasonable(food))
            return Event.REFUSED;
        Map<GrinderSpice, Integer> next = new LinkedHashMap<>(menu.draft());
        next.put(spice, count + 1);
        if (!withinCaps(GrinderSpice.expand(next), Services.CONFIG)
                || SpiceSources.of(player).count(spice) < food.getCount() * (count + 1))
            return Event.REFUSED;
        menu.draft().put(spice, count + 1);
        return Event.NONE;
    }

    private static Event removeFromDraft(SpiceGrinderMenu menu, GrinderSpice spice) {
        int count = menu.draft().getOrDefault(spice, 0);
        if (menu.session() != null || count <= 0)
            return Event.REFUSED;
        if (count == 1)
            menu.draft().remove(spice);
        else
            menu.draft().put(spice, count - 1);
        return Event.NONE;
    }

    private static Event season(SpiceGrinderMenu menu, ServerPlayer player) {
        SpiceSources sources = SpiceSources.of(player);
        if (!canSeason(menu, sources.available()))
            return Event.REFUSED;
        ItemStack food = menu.food().copy();
        Map<GrinderSpice, Integer> draft = new LinkedHashMap<>(menu.draft());
        Map<GrinderSpice, Integer> needed = new LinkedHashMap<>();
        draft.forEach((spice, count) -> needed.put(spice, count * food.getCount()));
        if (!sources.consume(needed))
            return Event.REFUSED;
        int jars = 0;
        for (Map.Entry<GrinderSpice, Integer> entry : needed.entrySet())
            jars += entry.getKey().mix().isPresent() ? entry.getValue() : 0;
        returnJars(player, jars);
        Map<Item, Integer> spices = GrinderSpice.expand(draft);

        IConfigHelper config = Services.CONFIG;
        long worldSeed = player.serverLevel().getServer().getWorldData().worldGenOptions().seed();
        long boardSeed = BoardSeed.of(worldSeed, config.getSeasoningBoardSalt(),
                BuiltInRegistries.ITEM.getKey(food.getItem()).toString());
        var layout = BoardLayoutRegistry.get();
        SeasoningBoard board = AutomaticSeasoning.board(boardSeed, layout);
        SeasoningRun run = new SeasoningRun(board, AutomaticSeasoning.rules(config),
                PointsLedger.start(rawProfile(spices, config), SpiceProfiles::effectiveValue));

        menu.grinder().set(ModDataComponents.GRINDER_SESSION.get(),
                new SeasoningSession(food, spices, boardSeed, layout, run.state()));
        menu.clearFood();
        menu.draft().clear();
        return Event.NONE;
    }

    /**
     * @param menu      A menu in the draft phase.
     * @param available How many of each loose spice and kind of Spice Mix the
     *                  player's inventory and the nearby storage hold.
     * @return Whether the draft can be started: seasonable food, at least one
     *         spice, enough of every spice, and some points.
     */
    static boolean canSeason(SpiceGrinderMenu menu, Map<GrinderSpice, Integer> available) {
        ItemStack food = menu.food();
        if (menu.session() != null || food.isEmpty() || !SpiceGrinderMenu.isSeasonable(food)
                || menu.draft().isEmpty())
            return false;
        for (Map.Entry<GrinderSpice, Integer> spice : menu.draft().entrySet()) {
            if (available.getOrDefault(spice.getKey(), 0) < spice.getValue() * food.getCount())
                return false;
        }
        PointsLedger ledger = PointsLedger.start(rawProfile(GrinderSpice.expand(menu.draft()), Services.CONFIG),
                SpiceProfiles::effectiveValue);
        for (FlavorAxis axis : FlavorAxis.values()) {
            if (ledger.points(axis) > 0D)
                return true;
        }
        return false;
    }

    // #region Run

    private static Event move(SpiceGrinderMenu menu, ServerPlayer player, int ordinal) {
        SeasoningSession session = menu.session();
        if (session == null || ordinal < 0 || ordinal >= Direction.values().length)
            return Event.REFUSED;
        Run run = Run.of(session);
        SeasoningRun.MoveResult result = run.run.move(Direction.values()[ordinal]);
        if (!result.moved())
            return Event.REFUSED;
        menu.grinder().set(ModDataComponents.GRINDER_SESSION.get(), session.withState(run.run.state()));
        ModStats.award(player, ModStats.GRINDER_MOVES);
        if (result.mineHit())
            return result.dud() ? Event.MINE_DUD : Event.MINE_HIT;
        return Event.NONE;
    }

    private static Event lockIn(SpiceGrinderMenu menu, ServerPlayer player) {
        SeasoningSession session = menu.session();
        if (session == null)
            return Event.REFUSED;
        Run run = Run.of(session);
        int cell = BoardGeometry.index(run.run.x(), run.run.y());
        if (!run.run.lockIn())
            return Event.REFUSED;
        menu.grinder().set(ModDataComponents.GRINDER_SESSION.get(), session.withState(run.run.state()));
        if (CellDiscoveries.of(player.server).record(player.getUUID(),
                new DiscoveryLog.Entry(session.boardSeed(), session.layout().hashCode(), cell),
                Services.CONFIG.getSeasoningDiscoveryLimit()))
            ModStats.award(player, ModStats.CELLS_UNLOCKED);
        return Event.LOCKED_IN;
    }

    private static Event addToRun(SpiceGrinderMenu menu, ServerPlayer player, GrinderSpice spice) {
        SeasoningSession session = menu.session();
        if (session == null)
            return Event.REFUSED;
        int foods = session.food().getCount();
        Map<Item, Integer> spices = new LinkedHashMap<>(session.spices());
        spice.perUnit().forEach((item, count) -> spices.merge(item, count, Integer::sum));
        if (!withinCaps(spices, Services.CONFIG) || !SpiceSources.of(player).consume(Map.of(spice, foods)))
            return Event.REFUSED;
        menu.grinder().set(ModDataComponents.GRINDER_SESSION.get(), session.withSpices(spices));
        if (spice.mix().isPresent())
            returnJars(player, foods);
        return Event.NONE;
    }

    /**
     * @param spices How many of each Spice Item per food.
     * @param config The config to read the caps from.
     * @return Whether no kind exceeds the per-kind cap and the total doesn't
     *         exceed the total cap.
     */
    private static boolean withinCaps(Map<Item, Integer> spices, IConfigHelper config) {
        int total = 0;
        for (int count : spices.values()) {
            if (count > config.getSeasoningMaxSpicesPerKind())
                return false;
            total += count;
        }
        return total <= config.getSeasoningMaxSpices();
    }

    /** Gives the player the jars of the Spice Mixes they used, dropping what doesn't fit. */
    private static void returnJars(ServerPlayer player, int count) {
        Item jar = ModItems.JAR.get();
        int left = count;
        while (left > 0) {
            int stack = Math.min(left, jar.getDefaultMaxStackSize());
            player.getInventory().placeItemBackInInventory(new ItemStack(jar, stack));
            left -= stack;
        }
    }

    private static Event accept(SpiceGrinderMenu menu, ServerPlayer player, boolean confirmed) {
        SeasoningSession session = menu.session();
        if (session == null)
            return Event.REFUSED;
        Run run = Run.of(session);
        if (run.run.effects().isEmpty() && !confirmed)
            return Event.CONFIRM_REQUIRED;

        Map<Item, Double> contributors = new LinkedHashMap<>();
        session.spices().forEach((item, count) -> contributors.put(item, count.doubleValue()));
        ItemStack result = session.food().copy();
        result.set(ModDataComponents.SEASONING.get(), new Seasoning(contributors, run.run.effects()));
        menu.grinder().remove(ModDataComponents.GRINDER_SESSION.get());
        if (SeasoningEffects.isFlawless(run.run.effects(), SeasoningEffectRegistry.catalog()))
            ModStats.award(player, ModStats.FLAWLESS_RUNS);
        player.getInventory().placeItemBackInInventory(result);
        player.closeContainer();
        return null;
    }

    // #region View

    /**
     * @param menu  The player's Spice Grinder menu.
     * @param event What the last action did.
     * @return What the player may see of the menu's state.
     */
    public static GrinderView viewOf(SpiceGrinderMenu menu, Event event) {
        IConfigHelper config = Services.CONFIG;
        Map<GrinderSpice, Integer> available = menu.inventory().player instanceof ServerPlayer player
                ? SpiceSources.of(player).available()
                : Map.of();
        SeasoningSession session = menu.session();
        if (session == null)
            return draftView(menu, event, config, available);

        Run run = Run.of(session);
        List<Double> points = new ArrayList<>();
        List<Integer> poles = new ArrayList<>();
        for (FlavorAxis axis : FlavorAxis.values()) {
            points.add(run.run.points(axis));
            poles.add(run.run.ledger().pole(axis));
        }
        List<Integer> stepsLeft = new ArrayList<>();
        double stepCost = config.getSeasoningStepCost();
        for (Direction direction : Direction.values()) {
            stepsLeft.add(run.run.canMove(direction)
                    ? (int) Math.min(MAX_STEPS_SHOWN,
                            Math.floor((run.run.points(direction.axis()) + STEP_EPSILON) / stepCost))
                    : 0);
        }
        Set<Integer> revealed = menu.inventory().player instanceof ServerPlayer viewer
                ? CellDiscoveries.of(viewer.server).revealed(viewer.getUUID(), session.boardSeed(),
                        session.layout().hashCode())
                : Set.of();
        List<CellView> cells = CellViews.of(run.board, run.run, revealed);
        Cell here = run.board.cell(run.run.x(), run.run.y());
        double lockInCost = here.kind() == CellKind.EFFECT ? config.getSeasoningLockInCost(here.ring()) : 0D;
        Map<GrinderSpice, Integer> consumed = new LinkedHashMap<>();
        session.spices().forEach((item, count) -> consumed.put(GrinderSpice.loose(item), count));
        return new GrinderView(GrinderView.Phase.RUNNING, session.food(), consumed, points, poles,
                run.run.x(), run.run.y(), cells, run.run.effects(), stepsLeft, run.run.canLockIn(), false, event,
                config.getSeasoningMaxSpicesPerKind(), config.getSeasoningMaxSpices(), available, stepCost,
                lockInCost);
    }

    private static GrinderView draftView(SpiceGrinderMenu menu, Event event, IConfigHelper config,
            Map<GrinderSpice, Integer> available) {
        PointsLedger ledger = PointsLedger.start(rawProfile(GrinderSpice.expand(menu.draft()), config),
                SpiceProfiles::effectiveValue);
        List<Double> points = new ArrayList<>();
        List<Integer> poles = new ArrayList<>();
        for (FlavorAxis axis : FlavorAxis.values()) {
            points.add(ledger.points(axis));
            poles.add(ledger.pole(axis));
        }
        return new GrinderView(GrinderView.Phase.DRAFT, menu.food().copy(), new LinkedHashMap<>(menu.draft()),
                points, poles, 0, 0, List.of(), List.of(),
                java.util.Collections.nCopies(Direction.values().length, 0), false, canSeason(menu, available), event,
                config.getSeasoningMaxSpicesPerKind(), config.getSeasoningMaxSpices(), available, 0D, 0D);
    }

    // #region Helpers

    /** A run restored from a session together with its board. */
    private record Run(SeasoningBoard board, SeasoningRun run) {

        private static Run of(SeasoningSession session) {
            IConfigHelper config = Services.CONFIG;
            SeasoningBoard board = AutomaticSeasoning.board(session.boardSeed(), session.layout());
            SeasoningRun run = SeasoningRun.restore(board, AutomaticSeasoning.rules(config),
                    rawProfile(session.spices(), config), SpiceProfiles::effectiveValue, session.state());
            return new Run(board, run);
        }
    }

    private static SpiceProfile rawProfile(Map<Item, Integer> spicesPerFood, IConfigHelper config) {
        Map<Item, Double> amounts = new HashMap<>();
        spicesPerFood.forEach((item, count) -> amounts.put(item, count.doubleValue()));
        return AutomaticSeasoning.rawProfileOf(amounts, config);
    }

    private static Optional<GrinderSpice> spiceOf(GrinderIntentPayload intent) {
        return intent.spice().filter(GrinderSpice::isValid);
    }
}
