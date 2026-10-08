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

    /** {@code spice_road:item_dried} - see {@link ItemDriedTrigger}. */
    public static final Supplier<ItemDriedTrigger> ITEM_DRIED = Services.REGISTRY
            .registerCriterionTrigger("item_dried", ItemDriedTrigger::new);

    /** {@code spice_road:spice_rack_filled} - see {@link SpiceRackFilledTrigger}. */
    public static final Supplier<SpiceRackFilledTrigger> SPICE_RACK_FILLED = Services.REGISTRY
            .registerCriterionTrigger("spice_rack_filled", SpiceRackFilledTrigger::new);

    /** {@code spice_road:spice_mix_crafted} - see {@link SpiceMixCraftedTrigger}. */
    public static final Supplier<SpiceMixCraftedTrigger> SPICE_MIX_CRAFTED = Services.REGISTRY
            .registerCriterionTrigger("spice_mix_crafted", SpiceMixCraftedTrigger::new);

    private ModCriteriaTriggers() {
    }

    /**
     * No-op other than forcing this class (and therefore its static
     * initializers) to load.
     */
    public static void register() {
    }
}
