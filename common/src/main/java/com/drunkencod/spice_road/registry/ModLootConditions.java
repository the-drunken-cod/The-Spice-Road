package com.drunkencod.spice_road.registry;

import java.util.function.Supplier;

import net.minecraft.world.level.storage.loot.predicates.LootItemConditionType;

import com.drunkencod.spice_road.loot.ConnectedPlayerCondition;
import com.drunkencod.spice_road.loot.SpiceMapLootEnabledCondition;
import com.drunkencod.spice_road.loot.SpiceRegionSupportedCondition;
import com.drunkencod.spice_road.platform.Services;

/**
 * Custom {@link LootItemConditionType}s, via {@link Services#REGISTRY}.
 */
public final class ModLootConditions {

    /** {@code spice_road:spice_region_supported} - see {@link SpiceRegionSupportedCondition}. */
    public static final Supplier<LootItemConditionType> SPICE_REGION_SUPPORTED = Services.REGISTRY
            .registerLootConditionType("spice_region_supported",
                    () -> new LootItemConditionType(SpiceRegionSupportedCondition.CODEC));

    /** {@code spice_road:connected_player} - see {@link ConnectedPlayerCondition}. */
    public static final Supplier<LootItemConditionType> CONNECTED_PLAYER = Services.REGISTRY
            .registerLootConditionType("connected_player",
                    () -> new LootItemConditionType(ConnectedPlayerCondition.CODEC));

    /** {@code spice_road:spice_map_loot_enabled} - see {@link SpiceMapLootEnabledCondition}. */
    public static final Supplier<LootItemConditionType> SPICE_MAP_LOOT_ENABLED = Services.REGISTRY
            .registerLootConditionType("spice_map_loot_enabled",
                    () -> new LootItemConditionType(SpiceMapLootEnabledCondition.CODEC));

    private ModLootConditions() {
    }

    /**
     * No-op other than forcing this class (and therefore
     * {@link #SPICE_REGION_SUPPORTED}'s static initializer) to load.
     */
    public static void register() {
    }
}
