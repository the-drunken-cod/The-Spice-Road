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
 * Datapack-configurable criterion firing when a {@code ServerPlayer} takes a
 * crafted Spice Mix out of the crafting grid, optionally restricted to the
 * mix. Registered as {@code spice_road:spice_mix_crafted}.
 */
public class SpiceMixCraftedTrigger extends SimpleCriterionTrigger<SpiceMixCraftedTrigger.TriggerInstance> {

    @Override
    public Codec<TriggerInstance> codec() {
        return TriggerInstance.CODEC;
    }

    /**
     * @param player The player who crafted the mix.
     * @param mix    The crafted Spice Mix.
     */
    public void trigger(ServerPlayer player, ItemStack mix) {
        this.trigger(player, triggerInstance -> triggerInstance.matches(mix));
    }

    public record TriggerInstance(
            Optional<ContextAwarePredicate> player,
            Optional<ItemPredicate> mix) implements SimpleCriterionTrigger.SimpleInstance {

        public static final Codec<TriggerInstance> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                ContextAwarePredicate.CODEC.optionalFieldOf("player").forGetter(TriggerInstance::player),
                ItemPredicate.CODEC.optionalFieldOf("mix").forGetter(TriggerInstance::mix))
                .apply(instance, TriggerInstance::new));

        /**
         * @param mix Restricts what was crafted; empty matches any Spice Mix.
         * @return A datagen-ready criterion for {@code spice_road:spice_mix_crafted}.
         */
        public static Criterion<TriggerInstance> spiceMixCrafted(Optional<ItemPredicate.Builder> mix) {
            return ModCriteriaTriggers.SPICE_MIX_CRAFTED.get().createCriterion(new TriggerInstance(
                    Optional.empty(),
                    mix.map(ItemPredicate.Builder::build)));
        }

        /**
         * @param crafted The crafted Spice Mix.
         * @return Whether this criterion is satisfied.
         */
        public boolean matches(ItemStack crafted) {
            return mix.map(predicate -> predicate.test(crafted)).orElse(true);
        }
    }
}
