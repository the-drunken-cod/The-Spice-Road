package com.drunkencod.spice_road.datagen;

import net.minecraft.advancements.critereon.ItemPredicate;
import net.minecraft.world.level.storage.loot.LootPool;
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
        if (spice.requiresHarvestTool())
            productPool.when(MatchTool
                    .toolMatches(ItemPredicate.Builder.item().of(spice.getHarvestToolTag())));

        if (spice.requiresHandPick())
            productPool.when(ConnectedPlayerCondition.connectedPlayer());

        return productPool;
    }
}
