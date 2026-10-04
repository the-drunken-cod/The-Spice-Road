package com.drunkencod.spice_road.spice.effect;

import java.util.HashMap;
import java.util.Map;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import com.drunkencod.spice_road.platform.Services;
import com.drunkencod.spice_road.registry.ModDataComponents;
import com.drunkencod.spice_road.spice.Seasoning;
import com.drunkencod.spice_road.spice.board.DiversityAward;
import com.drunkencod.spice_road.spice.board.ParticipationAward;

/**
 * The bonus saturation a seasoned food grants on top of its own: in relation to
 * the food's spice variety, see {@link DiversityAward}, and, if the food has no
 * effects at all, a participation award, see {@link ParticipationAward}. Shared
 * by eating the food and by anything that previews it.
 */
public final class SeasoningSaturation {

    private SeasoningSaturation() {
    }

    /**
     * @param food      The seasoned food item.
     * @param seasoning The food's Seasoning.
     * @return The bonus saturation eating the food grants, before the cap, see
     *         {@link SaturationCap}.
     */
    public static double bonus(Item food, Seasoning seasoning) {
        double bonus = DiversityAward.saturation(seasoning.contributors().size(),
                Services.CONFIG.getSeasoningDiversityMinSaturation(),
                Services.CONFIG.getSeasoningDiversityMaxSaturation(),
                Services.CONFIG.getSeasoningDiversityFullDiversity());
        if (seasoning.effects().isEmpty())
            bonus += participation(food, seasoning);
        return bonus;
    }

    /**
     * @param stack The food stack to preview.
     * @param food  The food's own properties.
     * @return {@code food} with the stack's bonus saturation added, or
     *         {@code food} itself if the stack isn't seasoned or earns no bonus.
     */
    public static FoodProperties withBonus(ItemStack stack, FoodProperties food) {
        Seasoning seasoning = stack.get(ModDataComponents.SEASONING.get());
        if (seasoning == null)
            return food;
        double bonus = bonus(stack.getItem(), seasoning);
        if (bonus <= 0D)
            return food;
        return new FoodProperties(food.nutrition(), food.saturation() + (float) bonus, food.canAlwaysEat(),
                food.eatSeconds(), food.usingConvertsTo(), food.effects());
    }

    private static double participation(Item food, Seasoning seasoning) {
        Map<String, Double> contributors = new HashMap<>();
        seasoning.contributors().forEach(
                (item, amount) -> contributors.put(BuiltInRegistries.ITEM.getKey(item).toString(), amount));
        long seed = ParticipationAward.seedOf(BuiltInRegistries.ITEM.getKey(food).toString(), contributors);
        return ParticipationAward.saturation(seed, seasoning.totalAmount(),
                Services.CONFIG.getSeasoningParticipationMinSaturation(),
                Services.CONFIG.getSeasoningParticipationMaxSaturation(),
                Services.CONFIG.getSeasoningParticipationFullDose());
    }
}
