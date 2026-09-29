package com.drunkencod.spice_road.advancement;

import java.util.function.Supplier;

import com.drunkencod.spice_road.platform.Services;

/**
 * Custom {@code CriterionTrigger}s, via {@link Services#REGISTRY}.
 */
public final class ModCriteriaTriggers {

    /** {@code spice_road:food_eaten} - see {@link FoodEatenTrigger}. */
    public static final Supplier<FoodEatenTrigger> FOOD_EATEN = Services.REGISTRY
            .registerCriterionTrigger("food_eaten", FoodEatenTrigger::new);

    private ModCriteriaTriggers() {
    }

    /**
     * No-op other than forcing this class (and therefore
     * {@link #FOOD_EATEN}'s static initializer) to load.
     */
    public static void register() {
    }
}
