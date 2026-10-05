package com.drunkencod.spice_road.loot;

import java.util.Set;

import com.google.common.collect.ImmutableSet;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.parameters.LootContextParam;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;
import net.minecraft.world.level.storage.loot.predicates.LootItemConditionType;

import com.drunkencod.spice_road.block.HarvestLuck;
import com.drunkencod.spice_road.registry.ModLootConditions;

/**
 * Loot condition passing with a chance of {@code chance}, scaled by the
 * breaking player's Harvest Luck. The Luck-aware counterpart of vanilla's
 * {@code minecraft:random_chance}, which block break loot can't scale with
 * Luck itself.
 */
public record HarvestSeedChanceCondition(float chance) implements LootItemCondition {

    /** Codec of this condition's JSON fields. */
    public static final MapCodec<HarvestSeedChanceCondition> CODEC = RecordCodecBuilder.mapCodec(
            instance -> instance.group(
                    Codec.floatRange(0.0F, 1.0F).fieldOf("chance").forGetter(HarvestSeedChanceCondition::chance))
                    .apply(instance, HarvestSeedChanceCondition::new));

    @Override
    public LootItemConditionType getType() {
        return ModLootConditions.HARVEST_SEED_CHANCE.get();
    }

    @Override
    public Set<LootContextParam<?>> getReferencedContextParams() {
        return ImmutableSet.of(LootContextParams.THIS_ENTITY);
    }

    @Override
    public boolean test(LootContext lootContext) {
        return lootContext.getRandom().nextFloat() < HarvestLuck.seedChance(chance,
                lootContext.getParamOrNull(LootContextParams.THIS_ENTITY));
    }

    /**
     * @param chance The seed drop chance before Luck, from {@code 0.0} to {@code 1.0}.
     * @return A builder for this condition, ready to pass to
     *         {@code LootPool.Builder#when}.
     */
    public static LootItemCondition.Builder harvestSeedChance(float chance) {
        return () -> new HarvestSeedChanceCondition(chance);
    }
}
