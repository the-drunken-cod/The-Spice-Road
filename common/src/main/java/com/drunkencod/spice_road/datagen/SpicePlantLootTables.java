package com.drunkencod.spice_road.datagen;

import net.minecraft.advancements.critereon.StatePropertiesPredicate;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.storage.loot.LootPool;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.entries.LootItem;
import net.minecraft.world.level.storage.loot.functions.SetItemCountFunction;
import net.minecraft.world.level.storage.loot.predicates.LootItemBlockStatePropertyCondition;
import net.minecraft.world.level.storage.loot.providers.number.ConstantValue;

import com.drunkencod.spice_road.loot.SpiceRegionSupportedCondition;
import com.drunkencod.spice_road.spice.Spice;

/**
 * Shared loot table shape for every {@code FLOWER_PATCH}/{@code CROP}/
 * {@code RHIZOME} Spice Plant block. Datagenned per {@link com.drunkencod.spice_road.spice.Spice}
 * enum member by the per-loader loot providers
 * ({@code NeoForgeSpiceLootProvider} / {@code FabricSpiceLootProvider}).
 */
public final class SpicePlantLootTables {

        private SpicePlantLootTables() {
        }

        /**
         * Builds the minimal Spice Plant loot table: the seed item always drops,
         * and the raw Spice product additionally drops at {@code maxAge} where
         * the Spice Region supports it, as far as {@link SpiceBreakHarvest}
         * allows the Spice to be obtained by breaking at all. If the Spice
         * {@link Spice#harvestYieldsPlantingItem() yields its planting item},
         * that harvest also raises the seed item's count to
         * {@code harvestYield}.
         *
         * @param block        The Spice Plant block this loot table is for.
         * @param ageProperty  The block's growth-stage property (its
         *                     {@link net.minecraft.world.level.block.CropBlock#getAgeProperty()}).
         * @param maxAge       The block's configured final growth stage (its
         *                     {@link net.minecraft.world.level.block.CropBlock#getMaxAge()}).
         * @param seedItem     Always-dropped seed item.
         * @param productItem  Raw Spice item, dropped only at {@code maxAge}.
         * @param harvestYield Flat count of {@code productItem} dropped at
         *                     {@code maxAge} (Phase 1: no tier-based scaling
         *                     yet).
         * @param spice        The Spice this plant grows, which decides how a
         *                     break harvest is gated.
         * @return The assembled loot table, ready to pass to a
         *         {@code BlockLootSubProvider}'s {@code add(Block, LootTable.Builder)}.
         */
        public static LootTable.Builder create(
                        Block block,
                        IntegerProperty ageProperty,
                        int maxAge,
                        ItemLike seedItem,
                        ItemLike productItem,
                        int harvestYield,
                        Spice spice) {
                LootPool.Builder seedPool = LootPool.lootPool()
                                .setRolls(ConstantValue.exactly(1.0F))
                                .add(LootItem.lootTableItem(seedItem));
                if (!SpiceBreakHarvest.canYieldSpice(spice))
                        return LootTable.lootTable().withPool(seedPool);

                LootTable.Builder table = LootTable.lootTable().withPool(seedPool)
                                .withPool(SpiceBreakHarvest.gate(spice,
                                                matureHarvestPool(block, ageProperty, maxAge, productItem, harvestYield)));

                // Tops the always-dropped seed item up to the harvest yield, so
                // both drop at the same rate.
                if (spice.harvestYieldsPlantingItem() && harvestYield > 1)
                        table.withPool(SpiceBreakHarvest.gate(spice,
                                        matureHarvestPool(block, ageProperty, maxAge, seedItem, harvestYield - 1)));
                return table;
        }

        /**
         * @return A pool dropping {@code count} of {@code item} from a
         *         {@code maxAge} plant where the Spice Region supports it.
         */
        private static LootPool.Builder matureHarvestPool(Block block, IntegerProperty ageProperty, int maxAge,
                        ItemLike item, int count) {
                return LootPool.lootPool()
                                .setRolls(ConstantValue.exactly(1.0F))
                                .when(LootItemBlockStatePropertyCondition.hasBlockStateProperties(block)
                                                .setProperties(StatePropertiesPredicate.Builder.properties()
                                                                .hasProperty(ageProperty, maxAge)))
                                .when(SpiceRegionSupportedCondition.spiceRegionSupported())
                                .add(LootItem.lootTableItem(item)
                                                .apply(SetItemCountFunction
                                                                .setCount(ConstantValue.exactly(count))));
        }
}
