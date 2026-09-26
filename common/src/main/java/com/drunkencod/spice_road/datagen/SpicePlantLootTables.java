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

/**
 * Shared loot table shape for every {@code FLOWER_PATCH}/{@code CROP} Spice
 * Plant block. Datagenned per {@link com.drunkencod.spice_road.spice.Spice}
 * enum member by the per-loader loot providers
 * ({@code NeoForgeSpiceLootProvider} / {@code FabricSpiceLootProvider}).
 */
public final class SpicePlantLootTables {

        private SpicePlantLootTables() {
        }

        /**
         * Builds the minimal Spice Plant loot table: the seed item always drops,
         * and the raw Spice product additionally drops, at the base harvestYield
         * multiplied by the configured harvest yield multiplier.
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
         * @return The assembled loot table, ready to pass to a
         *         {@code BlockLootSubProvider}'s {@code add(Block, LootTable.Builder)}.
         */
        public static LootTable.Builder create(
                        Block block,
                        IntegerProperty ageProperty,
                        int maxAge,
                        ItemLike seedItem,
                        ItemLike productItem,
                        int harvestYield) {
                LootPool.Builder seedPool = LootPool.lootPool()
                                .setRolls(ConstantValue.exactly(1.0F))
                                .add(LootItem.lootTableItem(seedItem));

                LootPool.Builder productPool = LootPool.lootPool()
                                .setRolls(ConstantValue.exactly(1.0F))
                                .when(LootItemBlockStatePropertyCondition.hasBlockStateProperties(block)
                                                .setProperties(StatePropertiesPredicate.Builder.properties()
                                                                .hasProperty(ageProperty, maxAge)))
                                .when(SpiceRegionSupportedCondition.spiceRegionSupported())
                                .add(LootItem.lootTableItem(productItem)
                                                .apply(SetItemCountFunction
                                                                .setCount(ConstantValue.exactly(harvestYield))));

                return LootTable.lootTable().withPool(seedPool).withPool(productPool);
        }
}
