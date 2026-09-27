package com.drunkencod.spice_road.registry;

import java.util.function.Supplier;

import net.minecraft.world.level.storage.loot.functions.LootItemFunctionType;

import com.drunkencod.spice_road.loot.SpiceMapFunction;
import com.drunkencod.spice_road.platform.Services;

/**
 * Custom {@link LootItemFunctionType}s, via {@link Services#REGISTRY}.
 */
public final class ModLootFunctions {

    /** {@code spice_road:spice_map} - see {@link SpiceMapFunction}. */
    public static final Supplier<LootItemFunctionType<SpiceMapFunction>> SPICE_MAP = Services.REGISTRY
            .registerLootFunctionType("spice_map", () -> new LootItemFunctionType<>(SpiceMapFunction.CODEC));

    private ModLootFunctions() {
    }

    /**
     * No-op other than forcing this class (and therefore
     * {@link #SPICE_MAP}'s static initializer) to load.
     */
    public static void register() {
    }
}
