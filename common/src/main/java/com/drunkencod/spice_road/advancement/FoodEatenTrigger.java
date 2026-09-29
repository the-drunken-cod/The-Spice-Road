package com.drunkencod.spice_road.advancement;

import java.util.Map;
import java.util.Optional;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.advancements.Criterion;
import net.minecraft.advancements.critereon.ContextAwarePredicate;
import net.minecraft.advancements.critereon.ItemPredicate;
import net.minecraft.advancements.critereon.SimpleCriterionTrigger;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

import com.drunkencod.spice_road.platform.Services;
import com.drunkencod.spice_road.spice.FlavorAxis;
import com.drunkencod.spice_road.spice.SpiceProfile;

/**
 * Datapack-configurable criterion firing when a {@code ServerPlayer} finishes
 * eating food matching an optional {@link ItemPredicate}, and optionally
 * reaching a per-{@link FlavorAxis} raw magnitude threshold (Sufficiently
 * Seasoned - see {@link com.drunkencod.spice_road.config.IConfigHelper#getSufficientlySeasoned()})
 * on enough of the listed axes. Registered as {@code spice_road:food_eaten}.
 */
public class FoodEatenTrigger extends SimpleCriterionTrigger<FoodEatenTrigger.TriggerInstance> {

    @Override
    public Codec<TriggerInstance> codec() {
        return TriggerInstance.CODEC;
    }

    /**
     * @param player     The player who finished eating.
     * @param stack      The stack that was eaten.
     * @param rawProfile {@code stack}'s raw, uncapped {@link SpiceProfile}.
     */
    public void trigger(ServerPlayer player, ItemStack stack, SpiceProfile rawProfile) {
        this.trigger(player, triggerInstance -> triggerInstance.matches(stack, rawProfile));
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
            Optional<Integer> minAxesMatched) implements SimpleCriterionTrigger.SimpleInstance {

        public static final Codec<TriggerInstance> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                ContextAwarePredicate.CODEC.optionalFieldOf("player").forGetter(TriggerInstance::player),
                ItemPredicate.CODEC.optionalFieldOf("item").forGetter(TriggerInstance::item),
                Codec.unboundedMap(FlavorAxis.CODEC, AxisThreshold.CODEC).optionalFieldOf("axis_thresholds")
                        .forGetter(TriggerInstance::axisThresholds),
                Codec.INT.optionalFieldOf("min_axes_matched").forGetter(TriggerInstance::minAxesMatched))
                .apply(instance, TriggerInstance::new));

        /**
         * @param item           Restricts which eaten item can trigger this
         *                       criterion; empty matches any food.
         * @param axisThresholds Per-axis magnitude requirements; omitted
         *                       axes aren't checked.
         * @param minAxesMatched How many of {@code axisThresholds}' axes
         *                       must pass; empty requires all of them.
         * @return A datagen-ready criterion for {@code spice_road:food_eaten}.
         */
        public static Criterion<TriggerInstance> foodEaten(Optional<ItemPredicate.Builder> item,
                Map<FlavorAxis, AxisThreshold> axisThresholds, Optional<Integer> minAxesMatched) {
            return ModCriteriaTriggers.FOOD_EATEN.get().createCriterion(new TriggerInstance(
                    Optional.empty(),
                    item.map(ItemPredicate.Builder::build),
                    axisThresholds.isEmpty() ? Optional.empty() : Optional.of(axisThresholds),
                    minAxesMatched));
        }

        /**
         * @param stack      The eaten stack.
         * @param rawProfile {@code stack}'s raw, uncapped {@link SpiceProfile}.
         * @return Whether this criterion is satisfied.
         */
        public boolean matches(ItemStack stack, SpiceProfile rawProfile) {
            if (item.isPresent() && !item.get().test(stack))
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
    }
}
