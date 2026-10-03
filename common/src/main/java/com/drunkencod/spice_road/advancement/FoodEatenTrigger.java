package com.drunkencod.spice_road.advancement;

import java.util.List;
import java.util.Optional;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.advancements.Criterion;
import net.minecraft.advancements.critereon.ContextAwarePredicate;
import net.minecraft.advancements.critereon.ItemPredicate;
import net.minecraft.advancements.critereon.SimpleCriterionTrigger;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

import com.drunkencod.spice_road.spice.Seasoning;

/**
 * Datapack-configurable criterion firing when a {@code ServerPlayer} finishes
 * eating food matching an optional {@link ItemPredicate}, optionally requiring
 * specific Flavor Contributors to be present and a minimum total contributor
 * amount. Registered as {@code spice_road:food_eaten}.
 */
public class FoodEatenTrigger extends SimpleCriterionTrigger<FoodEatenTrigger.TriggerInstance> {

    @Override
    public Codec<TriggerInstance> codec() {
        return TriggerInstance.CODEC;
    }

    /**
     * @param player    The player who finished eating.
     * @param stack     The stack that was eaten.
     * @param seasoning {@code stack}'s {@link Seasoning}, empty if it has none.
     */
    public void trigger(ServerPlayer player, ItemStack stack, Seasoning seasoning) {
        this.trigger(player, triggerInstance -> triggerInstance.matches(stack, seasoning));
    }

    public record TriggerInstance(
            Optional<ContextAwarePredicate> player,
            Optional<ItemPredicate> item,
            Optional<List<ItemPredicate>> contributors,
            Optional<Double> minContributorAmount) implements SimpleCriterionTrigger.SimpleInstance {

        public static final Codec<TriggerInstance> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                ContextAwarePredicate.CODEC.optionalFieldOf("player").forGetter(TriggerInstance::player),
                ItemPredicate.CODEC.optionalFieldOf("item").forGetter(TriggerInstance::item),
                ItemPredicate.CODEC.listOf().optionalFieldOf("contributors").forGetter(TriggerInstance::contributors),
                Codec.DOUBLE.optionalFieldOf("min_contributor_amount")
                        .forGetter(TriggerInstance::minContributorAmount))
                .apply(instance, TriggerInstance::new));

        /**
         * @param item                 Restricts which eaten item can trigger
         *                             this criterion; empty matches any food.
         * @param contributors         Flavor Contributors that must each match
         *                             at least one of the eaten stack's actual
         *                             contributors; other contributors may also
         *                             be present. Empty matches any (or no)
         *                             Flavor Contributors.
         * @param minContributorAmount The least total contributor amount the
         *                             eaten stack must carry; empty requires
         *                             none.
         * @return A datagen-ready criterion for {@code spice_road:food_eaten}.
         */
        public static Criterion<TriggerInstance> foodEaten(Optional<ItemPredicate.Builder> item,
                List<ItemPredicate.Builder> contributors, Optional<Double> minContributorAmount) {
            return ModCriteriaTriggers.FOOD_EATEN.get().createCriterion(new TriggerInstance(
                    Optional.empty(),
                    item.map(ItemPredicate.Builder::build),
                    contributors.isEmpty() ? Optional.empty()
                            : Optional.of(contributors.stream().map(ItemPredicate.Builder::build).toList()),
                    minContributorAmount));
        }

        /**
         * @param stack     The eaten stack.
         * @param seasoning {@code stack}'s {@link Seasoning}, empty if it has
         *                  none.
         * @return Whether this criterion is satisfied.
         */
        public boolean matches(ItemStack stack, Seasoning seasoning) {
            if (item.isPresent() && !item.get().test(stack))
                return false;
            if (minContributorAmount.isPresent() && seasoning.totalAmount() < minContributorAmount.get())
                return false;
            if (contributors.isEmpty())
                return true;
            List<ItemStack> stacks = seasoning.contributors().keySet().stream().map(ItemStack::new).toList();
            for (ItemPredicate required : contributors.get()) {
                if (stacks.stream().noneMatch(required::test))
                    return false;
            }
            return true;
        }
    }
}
