package com.drunkencod.spice_road.advancement;

import java.util.Optional;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.advancements.Criterion;
import net.minecraft.advancements.critereon.ContextAwarePredicate;
import net.minecraft.advancements.critereon.SimpleCriterionTrigger;
import net.minecraft.server.level.ServerPlayer;

/**
 * Datapack-configurable criterion firing when every raw Spice in
 * {@code #spice_road:spices/raw}, datapack-added ones included, has been in a
 * {@code ServerPlayer}'s inventory at least once. Registered as
 * {@code spice_road:all_spices_found}.
 */
public class AllSpicesFoundTrigger extends SimpleCriterionTrigger<AllSpicesFoundTrigger.TriggerInstance> {

    @Override
    public Codec<TriggerInstance> codec() {
        return TriggerInstance.CODEC;
    }

    /** @param player The player who has found every Spice. */
    public void trigger(ServerPlayer player) {
        this.trigger(player, triggerInstance -> true);
    }

    /** @param player Restricts who found every Spice. */
    public record TriggerInstance(Optional<ContextAwarePredicate> player)
            implements SimpleCriterionTrigger.SimpleInstance {

        public static final Codec<TriggerInstance> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                ContextAwarePredicate.CODEC.optionalFieldOf("player").forGetter(TriggerInstance::player))
                .apply(instance, TriggerInstance::new));

        /** @return A datagen-ready criterion for {@code spice_road:all_spices_found}. */
        public static Criterion<TriggerInstance> allSpicesFound() {
            return ModCriteriaTriggers.ALL_SPICES_FOUND.get().createCriterion(new TriggerInstance(Optional.empty()));
        }
    }
}
