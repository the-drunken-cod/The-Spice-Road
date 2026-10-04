package com.drunkencod.spice_road.grinder;

import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import com.drunkencod.spice_road.spice.FlavorAxis;
import com.drunkencod.spice_road.spice.board.BoardLayout;
import com.drunkencod.spice_road.spice.board.RunState;

/**
 * A run in progress, stored on the Spice Grinder item: the food stack, the
 * spices already consumed (per food), the board it started on and the state of
 * the run. The board is frozen: its seed and layout are kept here, so nothing
 * that changes later can change the cells under a run.
 *
 * @param food      The food stack being seasoned; every food in it gets the result.
 * @param spices    How many of each spice item were consumed per food.
 * @param boardSeed The seed of the board, fixed when the run started.
 * @param layout    The layout the board was built to, fixed when the run started.
 * @param state     The state of the run.
 */
public record SeasoningSession(ItemStack food, Map<Item, Integer> spices, long boardSeed, BoardLayout layout,
        RunState state) {

    private record Entry(Item item, int count) {

        private static final Codec<Entry> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                BuiltInRegistries.ITEM.byNameCodec().fieldOf("item").forGetter(Entry::item),
                Codec.intRange(1, 1000).fieldOf("count").forGetter(Entry::count))
                .apply(instance, Entry::new));
    }

    private static final Codec<Map<Item, Integer>> SPICES_CODEC = Entry.CODEC.listOf().xmap(
            entries -> {
                Map<Item, Integer> map = new LinkedHashMap<>();
                entries.forEach(entry -> map.merge(entry.item(), entry.count(), Integer::sum));
                return map;
            },
            map -> map.entrySet().stream().map(entry -> new Entry(entry.getKey(), entry.getValue())).toList());

    /** Persistent (NBT) codec. */
    public static final Codec<SeasoningSession> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            ItemStack.CODEC.fieldOf("food").forGetter(SeasoningSession::food),
            SPICES_CODEC.fieldOf("spices").forGetter(SeasoningSession::spices),
            Codec.LONG.fieldOf("board_seed").forGetter(SeasoningSession::boardSeed),
            BoardLayout.CODEC.fieldOf("layout").forGetter(SeasoningSession::layout),
            RunState.CODEC.fieldOf("state").forGetter(SeasoningSession::state))
            .apply(instance, SeasoningSession::new));

    /**
     * What clients are given in place of a session: it carries nothing but the
     * food, since the seed would let them rebuild the hidden board. Clients only
     * learn that a run is in progress, and on what. This one holds a dummy food
     * stack because anything that validates a stack through its persistent codec
     * (like the creative mode slot packet) rejects an empty one; use
     * {@link #isPlaceholder} to tell placeholders apart, and {@link #placeholder}
     * to make one for a food.
     */
    public static final SeasoningSession PLACEHOLDER = new SeasoningSession(new ItemStack(Items.BARRIER), Map.of(), 0L,
            BoardLayout.DEFAULT,
            new RunState(0, 0, Collections.nCopies(FlavorAxis.values().length, 0),
                    Collections.nCopies(FlavorAxis.values().length, 0D), List.of(), List.of(), List.of()));

    /**
     * @param food   The food stack, copied.
     * @param spices The spices per food, kept in registry-ID order.
     */
    public SeasoningSession {
        food = food.copy();
        Map<Item, Integer> sorted = new LinkedHashMap<>();
        spices.entrySet().stream()
                .sorted(Map.Entry.comparingByKey(Comparator.comparing(BuiltInRegistries.ITEM::getKey)))
                .forEach(entry -> sorted.put(entry.getKey(), entry.getValue()));
        spices = Collections.unmodifiableMap(sorted);
    }

    /**
     * @param food The food being seasoned.
     * @return What clients are given instead of the real session: the
     *         {@code food} and nothing else.
     */
    public static SeasoningSession placeholder(ItemStack food) {
        return new SeasoningSession(food, Map.of(), 0L, BoardLayout.DEFAULT, PLACEHOLDER.state);
    }

    /**
     * @param session A session read from an item stack, or {@code null}.
     * @return Whether it is a {@link #placeholder} clients are given instead of
     *         the real session.
     */
    public static boolean isPlaceholder(SeasoningSession session) {
        return session != null && session.state == PLACEHOLDER.state;
    }

    /**
     * @param newState The state of the run after an action.
     * @return This session with the run in {@code newState}.
     */
    public SeasoningSession withState(RunState newState) {
        return new SeasoningSession(food, spices, boardSeed, layout, newState);
    }

    /**
     * @param newSpices The spices per food after one was added.
     * @return This session with {@code newSpices}.
     */
    public SeasoningSession withSpices(Map<Item, Integer> newSpices) {
        return new SeasoningSession(food, newSpices, boardSeed, layout, state);
    }
}
