package com.drunkencod.spice_road.datagen;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.advancements.critereon.ItemPredicate;
import net.minecraft.world.level.storage.loot.LootPool;
import net.minecraft.world.level.storage.loot.predicates.AnyOfCondition;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;
import net.minecraft.world.level.storage.loot.predicates.MatchTool;

import com.drunkencod.spice_road.loot.ConnectedPlayerCondition;
import com.drunkencod.spice_road.spice.Spice;

/**
 * Shared rules for whether, and under which conditions, breaking a mature
 * Spice source drops its Spice instead of only the Spice's usual break drops -
 * the datagen counterpart of
 * {@link com.drunkencod.spice_road.block.SpiceHarvesting}, used by all three
 * {@code *LootTables} classes so every Source Type gates a break harvest
 * identically.
 * <p>
 * Breaking is the clumsy alternative to performing a Spice's Harvest Action
 * directly, so it yields the Spice only when it would have been harvestable
 * anyway:
 * <ul>
 * <li>No Harvest Tool and no Hand-Pick Requirement - always, the plain
 * flower/crop case.</li>
 * <li>A Harvest Tool Requirement - only when broken with that tool, which also
 * costs it a durability point (see
 * {@link com.drunkencod.spice_road.block.SpiceHarvesting#hurtHarvestTool}).</li>
 * <li>A Hand-Pick Requirement - additionally only when a genuinely connected
 * player broke it, never automation.</li>
 * <li>A Hand-Pick Requirement but no Harvest Tool Requirement - never, since
 * such a Spice is obtainable by hand-picking alone.</li>
 * </ul>
 */
public final class SpiceBreakHarvest {

    private SpiceBreakHarvest() {
    }

    /**
     * @param spice The Spice being broken.
     * @return Whether breaking {@code spice}'s source can yield it at all, i.e.
     *         whether a product pool is worth adding to its loot table.
     */
    public static boolean canYieldSpice(Spice spice) {
        return spice.requiresHarvestTool() || !spice.requiresHandPick();
    }

    /**
     * Adds the conditions gating a break harvest of {@code spice} to
     * {@code productPool} - nothing at all for a Spice that just drops when
     * broken.
     *
     * @param spice       The Spice the pool drops.
     * @param productPool The pool dropping the raw Spice item.
     * @return {@code productPool}, for chaining.
     */
    public static LootPool.Builder gate(Spice spice, LootPool.Builder productPool) {
        gateConditions(spice).forEach(productPool::when);
        return productPool;
    }

    /**
     * @param spice The Spice being broken.
     * @return Whether breaking {@code spice}'s source always yields it, with
     *         no condition to fail.
     */
    public static boolean yieldsUnconditionally(Spice spice) {
        return canYieldSpice(spice) && gateConditions(spice).isEmpty();
    }

    /**
     * Adds a condition to {@code pool} that holds exactly when breaking
     * {@code spice}'s source does <em>not</em> yield the Spice, i.e. when
     * the break is the clumsy kind. Does nothing for a Spice that
     * {@linkplain #yieldsUnconditionally always yields} (never holds, so the
     * caller shouldn't add the pool at all) or one that
     * {@linkplain #canYieldSpice never yields} (always holds).
     *
     * @param spice The Spice being broken.
     * @param pool  The pool to restrict to a break that yields no Spice.
     * @return {@code pool}, for chaining.
     */
    public static LootPool.Builder gateNot(Spice spice, LootPool.Builder pool) {
        if (!canYieldSpice(spice))
            return pool;

        List<LootItemCondition.Builder> conditions = gateConditions(spice);
        return pool.when(AnyOfCondition.anyOf(conditions.stream()
                .map(LootItemCondition.Builder::invert)
                .toArray(LootItemCondition.Builder[]::new)));
    }

    /**
     * @return The conditions that must all hold for a break to yield
     *         {@code spice}, empty if none are needed.
     */
    private static List<LootItemCondition.Builder> gateConditions(Spice spice) {
        List<LootItemCondition.Builder> conditions = new ArrayList<>();
        if (spice.requiresHarvestTool())
            conditions.add(MatchTool.toolMatches(ItemPredicate.Builder.item().of(spice.getHarvestToolTag())));

        if (spice.requiresHandPick())
            conditions.add(ConnectedPlayerCondition.connectedPlayer());

        return conditions;
    }
}
