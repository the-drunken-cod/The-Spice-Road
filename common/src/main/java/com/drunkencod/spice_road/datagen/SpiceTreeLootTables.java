package com.drunkencod.spice_road.datagen;

import net.minecraft.advancements.critereon.StatePropertiesPredicate;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.storage.loot.LootPool;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.entries.LootItem;
import net.minecraft.world.level.storage.loot.functions.SetItemCountFunction;
import net.minecraft.world.level.storage.loot.predicates.LootItemBlockStatePropertyCondition;
import net.minecraft.world.level.storage.loot.providers.number.ConstantValue;

import com.drunkencod.spice_road.Constants;
import com.drunkencod.spice_road.block.FruitingSpiceLeavesBlock;
import com.drunkencod.spice_road.block.SpiceTree;

/**
 * Shared loot table additions for Spice Tree blocks, used by the per-loader
 * loot providers ({@code NeoForgeSpiceLootProvider} /
 * {@code FabricSpiceLootProvider}).
 */
public final class SpiceTreeLootTables {

    private SpiceTreeLootTables() {
    }

    /**
     * Adds a pool to {@code leavesDrops} that drops the tree's Spice when ripe
     * {@link FruitingSpiceLeavesBlock} leaves are broken or decay, regardless
     * of the tool used. Plain (non-fruiting) leaves are returned unchanged.
     * <p>
     * Bakes in {@link Constants#DEFAULT_SPICE_TREE_HARVEST_YIELD_MULTIPLIER},
     * since config isn't loaded during {@code runData}.
     *
     * @param tree        The Spice Tree the leaves belong to.
     * @param leavesDrops The leaves' base loot table, e.g. vanilla's
     *                    {@code createLeavesDrops}.
     * @return {@code leavesDrops}, with the ripe harvest pool added if
     *         applicable.
     */
    public static LootTable.Builder withRipeLeavesHarvest(SpiceTree tree, LootTable.Builder leavesDrops) {
        LeavesBlock leaves = tree.getLeaves().get();
        if (!(leaves instanceof FruitingSpiceLeavesBlock))
            return leavesDrops;

        int harvestYield = (int) Math.floor(
                tree.getSpice().getDropAmount() * Constants.DEFAULT_SPICE_TREE_HARVEST_YIELD_MULTIPLIER);

        return leavesDrops.withPool(LootPool.lootPool()
                .setRolls(ConstantValue.exactly(1.0F))
                .when(LootItemBlockStatePropertyCondition.hasBlockStateProperties(leaves)
                        .setProperties(StatePropertiesPredicate.Builder.properties()
                                .hasProperty(FruitingSpiceLeavesBlock.AGE, Constants.SPICE_TREE_LEAF_GROWTH_STAGES)))
                .add(LootItem.lootTableItem(tree.getProductItem().get())
                        .apply(SetItemCountFunction.setCount(ConstantValue.exactly(harvestYield)))));
    }
}
