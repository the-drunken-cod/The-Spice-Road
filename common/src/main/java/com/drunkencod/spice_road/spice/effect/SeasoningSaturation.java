package com.drunkencod.spice_road.spice.effect;

import java.util.HashMap;
import java.util.Map;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import com.drunkencod.spice_road.platform.Services;
import com.drunkencod.spice_road.registry.ModDataComponents;
import com.drunkencod.spice_road.spice.Flavoring;
import com.drunkencod.spice_road.spice.Seasoning;
import com.drunkencod.spice_road.spice.board.DiversityAward;
import com.drunkencod.spice_road.spice.board.SaturationModifier;

/**
 * The saturation a food grants on top of its own, or takes away from it.
 * <p>
 * A seasoned food gets a bonus in relation to its spice variety, see
 * {@link DiversityAward}, and, if it has no effects at all, a saturation
 * modifier that may be negative, see {@link SaturationModifier}. If enabled, a
 * food that could be seasoned but isn't loses a share of its saturation
 * instead. Shared by eating the food and by anything that previews it.
 */
public final class SeasoningSaturation {

    private SeasoningSaturation() {
    }

    /**
     * @param stack The food stack, as it is eaten.
     * @param food  The food's own properties.
     * @return The saturation eating the food adds, or takes away if negative,
     *         before the cap, see {@link SaturationCap}. {@code 0} for a food
     *         that isn't seasoned and isn't penalized.
     */
    public static double modifier(ItemStack stack, FoodProperties food) {
        Seasoning seasoning = stack.get(ModDataComponents.SEASONING.get());
        if (seasoning != null)
            return seasonedModifier(stack.getItem(), seasoning);
        if (Services.CONFIG.getSeasoningUnseasonedEnabled() && Flavoring.isFlavorCarrier(stack))
            return food.saturation() * (Services.CONFIG.getSeasoningUnseasonedSaturationMultiplier() - 1D);
        return 0D;
    }

    /**
     * @param stack The food stack to preview.
     * @param food  The food's own properties.
     * @return {@code food} with the stack's saturation modifier applied, never
     *         below no saturation at all, or {@code food} itself if there is
     *         nothing to apply.
     */
    public static FoodProperties withModifier(ItemStack stack, FoodProperties food) {
        double modifier = modifier(stack, food);
        if (modifier == 0D)
            return food;
        float saturation = Math.max(0F, food.saturation() + (float) modifier);
        if (saturation == food.saturation())
            return food;
        return new FoodProperties(food.nutrition(), saturation, food.canAlwaysEat(), food.eatSeconds(),
                food.usingConvertsTo(), food.effects());
    }

    private static double seasonedModifier(Item food, Seasoning seasoning) {
        double modifier = DiversityAward.saturation(seasoning.contributors().size(),
                Services.CONFIG.getSeasoningDiversityMinSaturation(),
                Services.CONFIG.getSeasoningDiversityMaxSaturation(),
                Services.CONFIG.getSeasoningDiversityFullDiversity());
        if (seasoning.effects().isEmpty())
            modifier += effectlessModifier(food, seasoning);
        return modifier;
    }

    private static double effectlessModifier(Item food, Seasoning seasoning) {
        Map<String, Double> contributors = new HashMap<>();
        seasoning.contributors().forEach(
                (item, amount) -> contributors.put(BuiltInRegistries.ITEM.getKey(item).toString(), amount));
        long seed = SaturationModifier.seedOf(BuiltInRegistries.ITEM.getKey(food).toString(), contributors);
        return SaturationModifier.saturation(seed, seasoning.totalAmount(),
                Services.CONFIG.getSeasoningSaturationModifierMin(),
                Services.CONFIG.getSeasoningSaturationModifierMax(),
                Services.CONFIG.getSeasoningSaturationModifierFullDose());
    }
}
