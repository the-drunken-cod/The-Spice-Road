package com.drunkencod.spice_road.advancement;

import java.util.Optional;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.advancements.Criterion;
import net.minecraft.advancements.critereon.ContextAwarePredicate;
import net.minecraft.advancements.critereon.ItemPredicate;
import net.minecraft.advancements.critereon.SimpleCriterionTrigger;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

/**
 * Datapack-configurable criterion firing when an item a {@code ServerPlayer}
 * put on a Drying Rack finishes drying, optionally restricted to what went in
 * and what came out. Registered as {@code spice_road:item_dried}.
 */
public class ItemDriedTrigger extends SimpleCriterionTrigger<ItemDriedTrigger.TriggerInstance> {

    @Override
    public Codec<TriggerInstance> codec() {
        return TriggerInstance.CODEC;
    }

    /**
     * @param player The player who put the item on the rack.
     * @param input  The item that was dried.
     * @param result The primary result it produced.
     */
    public void trigger(ServerPlayer player, ItemStack input, ItemStack result) {
        this.trigger(player, triggerInstance -> triggerInstance.matches(input, result));
    }

    public record TriggerInstance(
            Optional<ContextAwarePredicate> player,
            Optional<ItemPredicate> input,
            Optional<ItemPredicate> result) implements SimpleCriterionTrigger.SimpleInstance {

        public static final Codec<TriggerInstance> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                ContextAwarePredicate.CODEC.optionalFieldOf("player").forGetter(TriggerInstance::player),
                ItemPredicate.CODEC.optionalFieldOf("input").forGetter(TriggerInstance::input),
                ItemPredicate.CODEC.optionalFieldOf("result").forGetter(TriggerInstance::result))
                .apply(instance, TriggerInstance::new));

        /**
         * @param input  Restricts what was dried; empty matches any item.
         * @param result Restricts what it dried into; empty matches any item.
         * @return A datagen-ready criterion for {@code spice_road:item_dried}.
         */
        public static Criterion<TriggerInstance> itemDried(Optional<ItemPredicate.Builder> input,
                Optional<ItemPredicate.Builder> result) {
            return ModCriteriaTriggers.ITEM_DRIED.get().createCriterion(new TriggerInstance(
                    Optional.empty(),
                    input.map(ItemPredicate.Builder::build),
                    result.map(ItemPredicate.Builder::build)));
        }

        /**
         * @param dried  The item that was dried.
         * @param result The primary result it produced.
         * @return Whether this criterion is satisfied.
         */
        public boolean matches(ItemStack dried, ItemStack result) {
            return input.map(predicate -> predicate.test(dried)).orElse(true)
                    && this.result.map(predicate -> predicate.test(result)).orElse(true);
        }
    }
}
