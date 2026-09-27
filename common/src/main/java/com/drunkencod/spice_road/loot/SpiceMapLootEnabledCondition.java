package com.drunkencod.spice_road.loot;

import com.mojang.serialization.MapCodec;

import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;
import net.minecraft.world.level.storage.loot.predicates.LootItemConditionType;

import com.drunkencod.spice_road.platform.Services;
import com.drunkencod.spice_road.registry.ModLootConditions;

/**
 * Loot condition passing while Spice Map loot is enabled in the server config
 * (see {@code IConfigHelper#isSpiceMapLootEnabled()}). Evaluated whenever
 * loot is rolled, so toggling the config needs no {@code /reload}.
 */
public enum SpiceMapLootEnabledCondition implements LootItemCondition {

    INSTANCE;

    /** Codec of this parameterless condition. */
    public static final MapCodec<SpiceMapLootEnabledCondition> CODEC = MapCodec.unit(INSTANCE);

    @Override
    public LootItemConditionType getType() {
        return ModLootConditions.SPICE_MAP_LOOT_ENABLED.get();
    }

    @Override
    public boolean test(LootContext lootContext) {
        return Services.CONFIG.isSpiceMapLootEnabled();
    }
}
