package com.drunkencod.spice_road.datagen;

import net.minecraft.advancements.critereon.StatePropertiesPredicate;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.storage.loot.LootPool;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.entries.LootItem;
import net.minecraft.world.level.storage.loot.predicates.LootItemBlockStatePropertyCondition;
import net.minecraft.world.level.storage.loot.providers.number.ConstantValue;

import com.drunkencod.spice_road.Constants;
import com.drunkencod.spice_road.block.FruitingSpiceLeavesBlock;
import com.drunkencod.spice_road.block.SpiceTree;
import com.drunkencod.spice_road.loot.HarvestYieldFunction;
import com.drunkencod.spice_road.spice.Spice;

/**
 * Shared loot table additions for Spice Tree blocks, used by the per-loader
 * loot providers ({@code NeoForgeSpiceLootProvider} /
 * {@code FabricSpiceLootProvider}).
 */
public final class SpiceTreeLootTables {

    private SpiceTreeLootTables() {
    }

    /**
     * Vanilla's {@code NORMAL_LEAVES_SAPLING_CHANCES} are the oak chance
     * {@value Spice#DEFAULT_SAPLING_DROP_CHANCE} times these factors per
     * Fortune level.
     */
    private static final float[] FORTUNE_FACTORS = { 1.0F, 1.25F, 5.0F / 3.0F, 2.0F };

    /**
     * @param spice The Spice of the tree whose leaves are being built.
     * @return The sapling drop chances per Fortune level, scaling the Spice's
     *         {@link Spice#getSaplingDropChance() base chance} like vanilla does
     *         for oak leaves, capped at {@code 1.0}.
     */
    public static float[] saplingChances(Spice spice) {
        float[] chances = new float[FORTUNE_FACTORS.length];
        for (int level = 0; level < chances.length; level++)
            chances[level] = Math.min(1.0F, spice.getSaplingDropChance() * FORTUNE_FACTORS[level]);
        return chances;
    }

    /**
     * Adds a pool to {@code leavesDrops} that drops the tree's Spice when ripe
     * {@link FruitingSpiceLeavesBlock} leaves are broken, as far as
     * {@link SpiceBreakHarvest} allows. Plain (non-fruiting) leaves are
     * returned unchanged.
     * <p>
     * Note that leaves of a Spice with a Harvest Tool Requirement therefore
     * stop yielding it when they decay, since decay involves no tool.
     * <p>
     * The count is scaled live by the tree harvest yield multiplier and
     * Harvest Luck, see {@link HarvestYieldFunction}.
     *
     * @param tree        The Spice Tree the leaves belong to.
     * @param leavesDrops The leaves' base loot table, e.g. vanilla's
     *                    {@code createLeavesDrops}.
     * @return {@code leavesDrops}, with the ripe harvest pool added if
     *         applicable.
     */
    public static LootTable.Builder withRipeLeavesHarvest(SpiceTree tree, LootTable.Builder leavesDrops) {
        LeavesBlock leaves = tree.getLeaves().get();
        Spice spice = tree.getSpice();
        if (!(leaves instanceof FruitingSpiceLeavesBlock) || !SpiceBreakHarvest.canYieldSpice(spice))
            return leavesDrops;

        LootPool.Builder productPool = LootPool.lootPool()
                .setRolls(ConstantValue.exactly(1.0F))
                .when(LootItemBlockStatePropertyCondition.hasBlockStateProperties(leaves)
                        .setProperties(StatePropertiesPredicate.Builder.properties()
                                .hasProperty(FruitingSpiceLeavesBlock.AGE, Constants.SPICE_TREE_LEAF_GROWTH_STAGES)))
                .add(LootItem.lootTableItem(tree.getProductItem().get())
                        .apply(HarvestYieldFunction.harvestYield(spice.getDropAmount(),
                                HarvestYieldFunction.Source.TREE)));

        return leavesDrops.withPool(SpiceBreakHarvest.gate(spice, productPool));
    }
}
