package com.drunkencod.spice_road.registry;

import java.util.function.Supplier;

import net.minecraft.world.level.storage.loot.predicates.LootItemConditionType;

import com.drunkencod.spice_road.loot.SpiceRegionSupportedCondition;
import com.drunkencod.spice_road.platform.Services;

/**
 * Custom {@link LootItemConditionType}s, via {@link Services#REGISTRY}.
 */
public final class ModLootConditions {

    public static final Supplier<LootItemConditionType> SPICE_REGION_SUPPORTED = Services.REGISTRY
            .registerLootConditionType("spice_region_supported",
                    () -> new LootItemConditionType(SpiceRegionSupportedCondition.CODEC));

    private ModLootConditions() {
    }

    /**
     * No-op other than forcing this class (and therefore
     * {@link #SPICE_REGION_SUPPORTED}'s static initializer) to load.
     */
    public static void register() {
    }
}
