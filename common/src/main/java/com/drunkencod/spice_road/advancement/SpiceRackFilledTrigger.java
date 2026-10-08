package com.drunkencod.spice_road.advancement;

import java.util.Optional;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.advancements.Criterion;
import net.minecraft.advancements.critereon.ContextAwarePredicate;
import net.minecraft.advancements.critereon.SimpleCriterionTrigger;
import net.minecraft.server.level.ServerPlayer;

/**
 * Datapack-configurable criterion firing when a {@code ServerPlayer} fills
 * every slot of a Spice Rack with a full stack. Registered as
 * {@code spice_road:spice_rack_filled}.
 */
public class SpiceRackFilledTrigger extends SimpleCriterionTrigger<SpiceRackFilledTrigger.TriggerInstance> {

    @Override
    public Codec<TriggerInstance> codec() {
        return TriggerInstance.CODEC;
    }

    /** @param player The player who filled the rack. */
    public void trigger(ServerPlayer player) {
        this.trigger(player, triggerInstance -> true);
    }

    /** @param player Restricts who filled the rack. */
    public record TriggerInstance(Optional<ContextAwarePredicate> player)
            implements SimpleCriterionTrigger.SimpleInstance {

        public static final Codec<TriggerInstance> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                ContextAwarePredicate.CODEC.optionalFieldOf("player").forGetter(TriggerInstance::player))
                .apply(instance, TriggerInstance::new));

        /** @return A datagen-ready criterion for {@code spice_road:spice_rack_filled}. */
        public static Criterion<TriggerInstance> spiceRackFilled() {
            return ModCriteriaTriggers.SPICE_RACK_FILLED.get().createCriterion(new TriggerInstance(Optional.empty()));
        }
    }
}
