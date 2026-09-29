package com.drunkencod.spice_road.advancement;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.advancements.Criterion;
import net.minecraft.advancements.critereon.ContextAwarePredicate;
import net.minecraft.advancements.critereon.ItemPredicate;
import net.minecraft.advancements.critereon.SimpleCriterionTrigger;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import com.drunkencod.spice_road.platform.Services;
import com.drunkencod.spice_road.spice.FlavorAxis;
import com.drunkencod.spice_road.spice.SpiceProfile;

/**
 * Datapack-configurable criterion firing when a {@code ServerPlayer} finishes
 * eating food matching an optional {@link ItemPredicate}, optionally
 * reaching a per-{@link FlavorAxis} raw magnitude threshold (Sufficiently
 * Seasoned - see
 * {@link com.drunkencod.spice_road.config.IConfigHelper#getSufficientlySeasoned()})
 * on enough of the listed axes, and optionally requiring specific Flavor
 * Contributors to be present. Registered as {@code spice_road:food_eaten}.
 */
public class FoodEatenTrigger extends SimpleCriterionTrigger<FoodEatenTrigger.TriggerInstance> {

    @Override
    public Codec<TriggerInstance> codec() {
        return TriggerInstance.CODEC;
    }

    /**
     * @param player       The player who finished eating.
     * @param stack        The stack that was eaten.
     * @param rawProfile   {@code stack}'s raw, uncapped {@link SpiceProfile}.
     * @param contributors {@code stack}'s Flavor Contributors.
     */
    public void trigger(ServerPlayer player, ItemStack stack, SpiceProfile rawProfile, Set<Item> contributors) {
        this.trigger(player, triggerInstance -> triggerInstance.matches(stack, rawProfile, contributors));
    }

    /**
     * @param minMagnitude The raw axis magnitude required, in either
     *                     direction; empty means fall back to the live
     *                     {@code sufficientlySeasoned} config value at
     *                     check time.
     */
    public record AxisThreshold(Optional<Double> minMagnitude) {

        public static final Codec<AxisThreshold> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                Codec.DOUBLE.optionalFieldOf("min_magnitude").forGetter(AxisThreshold::minMagnitude))
                .apply(instance, AxisThreshold::new));

        private double resolve() {
            return minMagnitude.orElseGet(Services.CONFIG::getSufficientlySeasoned);
        }
    }

    public record TriggerInstance(
            Optional<ContextAwarePredicate> player,
            Optional<ItemPredicate> item,
            Optional<Map<FlavorAxis, AxisThreshold>> axisThresholds,
            Optional<Integer> minAxesMatched,
            Optional<List<ItemPredicate>> contributors) implements SimpleCriterionTrigger.SimpleInstance {

        public static final Codec<TriggerInstance> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                ContextAwarePredicate.CODEC.optionalFieldOf("player").forGetter(TriggerInstance::player),
                ItemPredicate.CODEC.optionalFieldOf("item").forGetter(TriggerInstance::item),
                Codec.unboundedMap(FlavorAxis.CODEC, AxisThreshold.CODEC).optionalFieldOf("axis_thresholds")
                        .forGetter(TriggerInstance::axisThresholds),
                Codec.INT.optionalFieldOf("min_axes_matched").forGetter(TriggerInstance::minAxesMatched),
                ItemPredicate.CODEC.listOf().optionalFieldOf("contributors").forGetter(TriggerInstance::contributors))
                .apply(instance, TriggerInstance::new));

        /**
         * @param item           Restricts which eaten item can trigger this
         *                       criterion; empty matches any food.
         * @param axisThresholds Per-axis magnitude requirements; omitted
         *                       axes aren't checked.
         * @param minAxesMatched How many of {@code axisThresholds}' axes
         *                       must pass; empty requires all of them.
         * @param contributors   Flavor Contributors that must each match at
         *                       least one of the eaten stack's actual
         *                       contributors; other contributors may also be
         *                       present. Empty matches any (or no) Flavor
         *                       Contributors.
         * @return A datagen-ready criterion for {@code spice_road:food_eaten}.
         */
        public static Criterion<TriggerInstance> foodEaten(Optional<ItemPredicate.Builder> item,
                Map<FlavorAxis, AxisThreshold> axisThresholds, Optional<Integer> minAxesMatched,
                List<ItemPredicate.Builder> contributors) {
            return ModCriteriaTriggers.FOOD_EATEN.get().createCriterion(new TriggerInstance(
                    Optional.empty(),
                    item.map(ItemPredicate.Builder::build),
                    axisThresholds.isEmpty() ? Optional.empty() : Optional.of(axisThresholds),
                    minAxesMatched,
                    contributors.isEmpty() ? Optional.empty()
                            : Optional.of(contributors.stream().map(ItemPredicate.Builder::build).toList())));
        }

        /**
         * @param stack             The eaten stack.
         * @param rawProfile        {@code stack}'s raw, uncapped {@link SpiceProfile}.
         * @param stackContributors {@code stack}'s Flavor Contributors.
         * @return Whether this criterion is satisfied.
         */
        public boolean matches(ItemStack stack, SpiceProfile rawProfile, Set<Item> stackContributors) {
            if (item.isPresent() && !item.get().test(stack))
                return false;
            if (!matchesContributors(stackContributors))
                return false;
            if (axisThresholds.isEmpty())
                return true;

            Map<FlavorAxis, AxisThreshold> thresholds = axisThresholds.get();
            int matchedCount = 0;
            for (Map.Entry<FlavorAxis, AxisThreshold> entry : thresholds.entrySet()) {
                if (Math.abs(rawProfile.get(entry.getKey())) >= entry.getValue().resolve())
                    matchedCount++;
            }
            return matchedCount >= minAxesMatched.orElse(thresholds.size());
        }

        private boolean matchesContributors(Set<Item> stackContributors) {
            if (contributors.isEmpty())
                return true;
            List<ItemStack> stacks = stackContributors.stream().map(ItemStack::new).toList();
            for (ItemPredicate required : contributors.get()) {
                if (stacks.stream().noneMatch(required::test))
                    return false;
            }
            return true;
        }
    }
}
