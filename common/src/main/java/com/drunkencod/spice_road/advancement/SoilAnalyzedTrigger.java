package com.drunkencod.spice_road.advancement;

import java.util.Optional;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.advancements.Criterion;
import net.minecraft.advancements.critereon.ContextAwarePredicate;
import net.minecraft.advancements.critereon.SimpleCriterionTrigger;
import net.minecraft.server.level.ServerPlayer;

/**
 * Datapack-configurable criterion firing when a {@code ServerPlayer} gets a
 * reading from the Soil Analyzer in a region where a Spice grows, whether or
 * not the reading names it. Registered as {@code spice_road:soil_analyzed}.
 */
public class SoilAnalyzedTrigger extends SimpleCriterionTrigger<SoilAnalyzedTrigger.TriggerInstance> {

    @Override
    public Codec<TriggerInstance> codec() {
        return TriggerInstance.CODEC;
    }

    /** @param player The player who got the reading. */
    public void trigger(ServerPlayer player) {
        this.trigger(player, triggerInstance -> true);
    }

    /** @param player Restricts who got the reading. */
    public record TriggerInstance(Optional<ContextAwarePredicate> player)
            implements SimpleCriterionTrigger.SimpleInstance {

        public static final Codec<TriggerInstance> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                ContextAwarePredicate.CODEC.optionalFieldOf("player").forGetter(TriggerInstance::player))
                .apply(instance, TriggerInstance::new));

        /** @return A datagen-ready criterion for {@code spice_road:soil_analyzed}. */
        public static Criterion<TriggerInstance> soilAnalyzed() {
            return ModCriteriaTriggers.SOIL_ANALYZED.get().createCriterion(new TriggerInstance(Optional.empty()));
        }
    }
}
