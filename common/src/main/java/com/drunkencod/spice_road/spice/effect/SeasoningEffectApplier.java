package com.drunkencod.spice_road.spice.effect;

import java.util.HashMap;
import java.util.Map;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.food.FoodData;
import net.minecraft.world.item.Item;

import com.drunkencod.spice_road.event.FoodEatenListeners;
import com.drunkencod.spice_road.platform.Services;
import com.drunkencod.spice_road.registry.ModDataComponents;
import com.drunkencod.spice_road.spice.Seasoning;
import com.drunkencod.spice_road.spice.board.ParticipationAward;

/**
 * Applies a seasoned food's Seasoning Effects to whoever eats it, through
 * {@link FoodEatenListeners}. A seasoned food without any effects gives a player
 * bonus saturation instead, see {@link ParticipationAward}.
 */
public final class SeasoningEffectApplier {

    private SeasoningEffectApplier() {
    }

    /** Registers this listener with {@link FoodEatenListeners}. */
    public static void register() {
        FoodEatenListeners.register(event -> {
            if (event.level().isClientSide())
                return;
            Seasoning seasoning = event.stack().get(ModDataComponents.SEASONING.get());
            if (seasoning == null)
                return;
            if (!seasoning.effects().isEmpty())
                SeasoningEffects.apply(event.entity(), seasoning);
            else if (event.entity() instanceof Player player)
                awardParticipation(player, event.stack().getItem(), seasoning);
        });
    }

    private static void awardParticipation(Player player, Item food, Seasoning seasoning) {
        Map<String, Double> contributors = new HashMap<>();
        seasoning.contributors().forEach(
                (item, amount) -> contributors.put(BuiltInRegistries.ITEM.getKey(item).toString(), amount));
        long seed = ParticipationAward.seedOf(BuiltInRegistries.ITEM.getKey(food).toString(), contributors);
        double bonus = ParticipationAward.saturation(seed, seasoning.totalAmount(),
                Services.CONFIG.getSeasoningParticipationMinSaturation(),
                Services.CONFIG.getSeasoningParticipationMaxSaturation(),
                Services.CONFIG.getSeasoningParticipationFullDose());
        FoodData foodData = player.getFoodData();
        foodData.setSaturation((float) Math.min(foodData.getFoodLevel(), foodData.getSaturationLevel() + bonus));
    }
}
