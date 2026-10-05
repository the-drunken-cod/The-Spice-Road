package com.drunkencod.spice_road.loot;

import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.function.DoubleSupplier;

import com.google.common.collect.ImmutableSet;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.util.StringRepresentable;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.functions.LootItemConditionalFunction;
import net.minecraft.world.level.storage.loot.functions.LootItemFunction;
import net.minecraft.world.level.storage.loot.functions.LootItemFunctionType;
import net.minecraft.world.level.storage.loot.parameters.LootContextParam;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;

import com.drunkencod.spice_road.block.HarvestLuck;
import com.drunkencod.spice_road.platform.Services;
import com.drunkencod.spice_road.registry.ModLootFunctions;

/**
 * Loot function setting a stack's count to a Spice's harvest yield: its base
 * drop amount scaled by the live harvest yield multiplier of its {@link Source}
 * and the breaking player's Harvest Luck. Evaluated whenever loot is rolled, so
 * config changes need no {@code /reload}. Drops nothing if the yield rolls to
 * zero.
 */
public class HarvestYieldFunction extends LootItemConditionalFunction {

    /** Codec of this function's JSON fields. */
    public static final MapCodec<HarvestYieldFunction> CODEC = RecordCodecBuilder.mapCodec(
            instance -> commonFields(instance).and(instance.group(
                    Codec.intRange(0, 1024).fieldOf("base").forGetter(function -> function.base),
                    Source.CODEC.fieldOf("source").forGetter(function -> function.source)))
                    .apply(instance, HarvestYieldFunction::new));

    private final int base;
    private final Source source;

    private HarvestYieldFunction(List<LootItemCondition> conditions, int base, Source source) {
        super(conditions);
        this.base = base;
        this.source = source;
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
        int count = HarvestLuck.rollYield(context.getRandom(), base, source.multiplier.getAsDouble(),
                context.getParamOrNull(LootContextParams.THIS_ENTITY));
        if (count <= 0)
            return ItemStack.EMPTY;

        stack.setCount(count);
        return stack;
    }

    /**
     * @param base   The Spice's unscaled drop amount.
     * @param source Which harvest yield multiplier applies.
     * @return A builder for this function, ready to pass to
     *         {@code LootPoolSingletonContainer.Builder#apply}.
     */
    public static LootItemFunction.Builder harvestYield(int base, Source source) {
        return simpleBuilder(conditions -> new HarvestYieldFunction(conditions, base, source));
    }

    /** The kind of Spice source a yield comes from, which decides its multiplier config. */
    public enum Source implements StringRepresentable {

        /** Spice Plants and Spice Vines. */
        PLANT(() -> Services.CONFIG.getSpicePlantHarvestYieldMultiplier()),
        /** Spice Trees. */
        TREE(() -> Services.CONFIG.getSpiceTreeHarvestYieldMultiplier());

        /** Codec reading and writing a {@link Source} by its lowercase name. */
        public static final Codec<Source> CODEC = StringRepresentable.fromEnum(Source::values);

        private final DoubleSupplier multiplier;

        Source(DoubleSupplier multiplier) {
            this.multiplier = multiplier;
        }

        @Override
        public String getSerializedName() {
            return name().toLowerCase(Locale.ROOT);
        }
    }
}
