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
import net.minecraft.world.level.storage.loot.predicates.LootItemRandomChanceCondition;
import net.minecraft.world.level.storage.loot.providers.number.ConstantValue;

import com.drunkencod.spice_road.loot.SpiceRegionSupportedCondition;
import com.drunkencod.spice_road.spice.Spice;
import com.drunkencod.spice_road.spice.Tier;

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
         * Builds the minimal Spice Plant loot table: an immature plant always
         * drops its seed item. A mature one drops it only with the
         * {@link Tier#getSeedDropChance() seed drop chance} of the Spice's
         * tier when the break also yields the Spice, and always when it
         * doesn't (e.g. broken without the required harvest tool), so such a
         * break never leaves the player empty-handed. The raw Spice product
         * additionally drops at {@code maxAge} where the Spice Region
         * supports it, as far as {@link SpiceBreakHarvest} allows the Spice
         * to be obtained by breaking at all.
         *
         * @param block        The Spice Plant block this loot table is for.
         * @param ageProperty  The block's growth-stage property (its
         *                     {@link net.minecraft.world.level.block.CropBlock#getAgeProperty()}).
         * @param maxAge       The block's configured final growth stage (its
         *                     {@link net.minecraft.world.level.block.CropBlock#getMaxAge()}).
         * @param seedItem     Seed item, dropped always before {@code maxAge}
         *                     and at it, by chance only if the break also
         *                     yields the Spice.
         * @param productItem  Raw Spice item, dropped only at {@code maxAge}.
         * @param harvestYield Flat count of {@code productItem} dropped at
         *                     {@code maxAge} (Phase 1: no tier-based scaling
         *                     yet).
         * @param spice        The Spice this plant grows, which decides how a
         *                     break harvest is gated and its seed drop chance.
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
                LootItemBlockStatePropertyCondition.Builder isMature = LootItemBlockStatePropertyCondition
                                .hasBlockStateProperties(block)
                                .setProperties(StatePropertiesPredicate.Builder.properties()
                                                .hasProperty(ageProperty, maxAge));
                LootPool.Builder immatureSeedPool = LootPool.lootPool()
                                .setRolls(ConstantValue.exactly(1.0F))
                                .when(isMature.invert())
                                .add(LootItem.lootTableItem(seedItem));
                LootTable.Builder table = LootTable.lootTable().withPool(immatureSeedPool);
                if (SpiceBreakHarvest.canYieldSpice(spice))
                        table.withPool(SpiceBreakHarvest.gate(spice, LootPool.lootPool()
                                        .setRolls(ConstantValue.exactly(1.0F))
                                        .when(isMature)
                                        .when(LootItemRandomChanceCondition
                                                        .randomChance(spice.getTier().getSeedDropChance()))
                                        .add(LootItem.lootTableItem(seedItem))));

                if (!SpiceBreakHarvest.yieldsUnconditionally(spice))
                        table.withPool(SpiceBreakHarvest.gateNot(spice, LootPool.lootPool()
                                        .setRolls(ConstantValue.exactly(1.0F))
                                        .when(isMature)
                                        .add(LootItem.lootTableItem(seedItem))));

                if (!SpiceBreakHarvest.canYieldSpice(spice))
                        return table;

                return table.withPool(SpiceBreakHarvest.gate(spice,
                                matureHarvestPool(block, ageProperty, maxAge, productItem, harvestYield)));
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
