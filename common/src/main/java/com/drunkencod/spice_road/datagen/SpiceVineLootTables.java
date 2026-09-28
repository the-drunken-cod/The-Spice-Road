package com.drunkencod.spice_road.datagen;

import net.minecraft.advancements.critereon.StatePropertiesPredicate;
import net.minecraft.world.level.storage.loot.LootPool;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.entries.LootItem;
import net.minecraft.world.level.storage.loot.functions.SetItemCountFunction;
import net.minecraft.world.level.storage.loot.predicates.LootItemBlockStatePropertyCondition;
import net.minecraft.world.level.storage.loot.providers.number.ConstantValue;

import com.drunkencod.spice_road.Constants;
import com.drunkencod.spice_road.block.SpiceVineBlock;
import com.drunkencod.spice_road.block.SpiceVines;

/**
 * Shared loot table additions for Spice Vine blocks, used by the per-loader
 * loot providers ({@code NeoForgeSpiceLootProvider} /
 * {@code FabricSpiceLootProvider}).
 */
public final class SpiceVineLootTables {

    private SpiceVineLootTables() {
    }

    /**
     * Adds a pool to {@code shearsDrops} that drops the vine's Spice when a
     * ripe segment is broken, unless the Spice has a Hand-Pick Requirement -
     * then breaking never yields it, and it can only be harvested by
     * right-clicking.
     * <p>
     * Bakes in {@link Constants#DEFAULT_SPICE_PLANT_HARVEST_YIELD_MULTIPLIER},
     * since config isn't loaded during {@code runData}.
     *
     * @param vine        The registered Spice Vine.
     * @param shearsDrops The vine's base loot table, e.g. vanilla's
     *                    {@code createShearsOnlyDrop}.
     * @return {@code shearsDrops}, with the ripe harvest pool added if
     *         applicable.
     */
    public static LootTable.Builder withRipeVineHarvest(SpiceVines.RegisteredSpiceVine vine,
            LootTable.Builder shearsDrops) {
        if (vine.spice().requiresHandPick())
            return shearsDrops;

        int harvestYield = (int) Math.floor(
                vine.spice().getDropAmount() * Constants.DEFAULT_SPICE_PLANT_HARVEST_YIELD_MULTIPLIER);

        return shearsDrops.withPool(LootPool.lootPool()
                .setRolls(ConstantValue.exactly(1.0F))
                .when(LootItemBlockStatePropertyCondition.hasBlockStateProperties(vine.block().get())
                        .setProperties(StatePropertiesPredicate.Builder.properties()
                                .hasProperty(SpiceVineBlock.AGE, Constants.SPICE_VINE_GROWTH_STAGES)))
                .add(LootItem.lootTableItem(vine.productItem().get())
                        .apply(SetItemCountFunction.setCount(ConstantValue.exactly(harvestYield)))));
    }
}
