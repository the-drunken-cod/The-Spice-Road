package com.drunkencod.spice_road.loot;

import java.util.List;
import java.util.Set;

import com.google.common.collect.ImmutableSet;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.functions.LootItemConditionalFunction;
import net.minecraft.world.level.storage.loot.functions.LootItemFunction;
import net.minecraft.world.level.storage.loot.functions.LootItemFunctionType;
import net.minecraft.world.level.storage.loot.parameters.LootContextParam;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;

import com.drunkencod.spice_road.block.HarvestLuck;
import com.drunkencod.spice_road.registry.ModLootFunctions;

/**
 * Loot function setting a stack's count to a Spice's harvest yield: one item,
 * changed by the breaking player's Harvest Luck (see
 * {@link HarvestLuck#rollYield}). Evaluated whenever loot is rolled, so config
 * changes need no {@code /reload}. Drops nothing if Bad Luck rolls the yield to
 * zero.
 */
public class HarvestYieldFunction extends LootItemConditionalFunction {

    /** Codec of this function's JSON fields. */
    public static final MapCodec<HarvestYieldFunction> CODEC = RecordCodecBuilder.mapCodec(
            instance -> commonFields(instance).apply(instance, HarvestYieldFunction::new));

    private HarvestYieldFunction(List<LootItemCondition> conditions) {
        super(conditions);
    }

    @Override
    public LootItemFunctionType<HarvestYieldFunction> getType() {
        return ModLootFunctions.HARVEST_YIELD.get();
    }

    @Override
    public Set<LootContextParam<?>> getReferencedContextParams() {
        return ImmutableSet.of(LootContextParams.THIS_ENTITY);
    }

    @Override
    protected ItemStack run(ItemStack stack, LootContext context) {
        int count = HarvestLuck.rollYield(context.getRandom(), context.getParamOrNull(LootContextParams.THIS_ENTITY));
        if (count <= 0)
            return ItemStack.EMPTY;

        stack.setCount(count);
        return stack;
    }

    /**
     * @return A builder for this function, ready to pass to
     *         {@code LootPoolSingletonContainer.Builder#apply}.
     */
    public static LootItemFunction.Builder harvestYield() {
        return simpleBuilder(HarvestYieldFunction::new);
    }
}
