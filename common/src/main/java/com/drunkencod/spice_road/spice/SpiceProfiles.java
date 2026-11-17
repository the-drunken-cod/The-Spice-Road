package com.drunkencod.spice_road.spice;

import java.util.Map;
import java.util.Optional;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import com.drunkencod.spice_road.mix.SpiceMix;
import com.drunkencod.spice_road.platform.Services;
import com.drunkencod.spice_road.registry.ModDataComponents;

/**
 * Single choke point for reading an {@link ItemStack}'s {@link SpiceProfile}.
 * Only Spice Items have one: the datapack-registered default for their item
 * ({@link SpiceProfileRegistry}). Seasoned food carries counted contributors
 * instead (see {@link Seasoning}).
 * <p>
 * Every place that needs a stack's flavor axis values (tooltips, the Spice
 * Grinder) should go through this rather than reading the registry directly,
 * so a datapack-registered spice behaves identically everywhere.
 */
public final class SpiceProfiles {

    private SpiceProfiles() {
    }

    /**
     * @param stack The stack to look up.
     * @return The datapack-registered {@link SpiceProfile} of
     *         {@code stack.getItem()}. Empty if {@code stack} isn't a Spice Item.
     */
    public static Optional<SpiceProfile> get(ItemStack stack) {
        return SpiceProfileRegistry.getDefault(stack.getItem());
    }

    /**
     * @param stack The stack to look up.
     * @return The Effective Profile of {@code stack}'s {@link #get profile},
     *         which is what effects and tooltips should use.
     */
    public static Optional<SpiceProfile> getEffective(ItemStack stack) {
        return get(stack).map(SpiceProfiles::effective);
    }

    /**
     * @param stack The stack to look up.
     * @return The Effective Profile of a filled Spice Mix: the average of the
     *         Default Profiles of the spices it holds, weighted by their counts,
     *         so it reads as the character of one spoonful. Opposing spices
     *         are softened (see {@link #mixSum}). Empty if {@code stack} isn't a
     *         filled Spice Mix or none of its spices has a non-zero Default
     *         Profile.
     */
    public static Optional<SpiceProfile> getEffectiveMix(ItemStack stack) {
        SpiceMix mix = stack.get(ModDataComponents.SPICE_MIX.get());
        if (mix == null || mix.total() <= 0)
            return Optional.empty();
        SpiceProfile sum = mixSum(mix);
        return sum.isZero() ? Optional.empty() : Optional.of(effective(sum.scale(1D / mix.total())));
    }

    /**
     * @param stack The stack to look up.
     * @return The {@link #getEffectiveMix Effective Profile} of a filled Spice
     *         Mix multiplied by its spice count, so its numbers aren't scaled
     *         down by the averaging. Divide by {@code mix.total()} to get the
     *         range the bars are scaled to. Empty under the same conditions as
     *         {@link #getEffectiveMix}.
     */
    public static Optional<SpiceProfile> getEffectiveMixTotal(ItemStack stack) {
        SpiceMix mix = stack.get(ModDataComponents.SPICE_MIX.get());
        if (mix == null)
            return Optional.empty();
        return getEffectiveMix(stack).map(average -> average.scale(mix.total()));
    }

    /**
     * Sums the Default Profiles of a Spice Mix's spices, weighted by their
     * counts. On each axis, the weaker of the positive and negative
     * contributions is first multiplied by the configured opposing factor, so
     * mixes with components that oppose each other are less contradictory.
     *
     * @param mix The Spice Mix.
     * @return The softened sum, not yet averaged or saturated.
     */
    private static SpiceProfile mixSum(SpiceMix mix) {
        FlavorAxis[] axes = FlavorAxis.values();
        double[] positive = new double[axes.length];
        double[] negative = new double[axes.length];
        for (Map.Entry<Item, Integer> spice : mix.spices().entrySet()) {
            Optional<SpiceProfile> profile = SpiceProfileRegistry.getDefault(spice.getKey());
            if (profile.isEmpty())
                continue;
            for (FlavorAxis axis : axes) {
                double weighted = profile.get().get(axis) * spice.getValue();
                if (weighted > 0D)
                    positive[axis.ordinal()] += weighted;
                else
                    negative[axis.ordinal()] += weighted;
            }
        }
        double factor = Services.CONFIG.getFlavorMixOpposingFactor();
        double[] sum = new double[axes.length];
        for (int i = 0; i < sum.length; i++) {
            if (positive[i] > -negative[i])
                sum[i] = positive[i] + negative[i] * factor;
            else
                sum[i] = positive[i] * factor + negative[i];
        }
        return new SpiceProfile(sum);
    }

    /**
     * Derives an Effective Profile: each axis saturates towards the
     * configured soft cap ({@code cap * tanh(value / cap)}), giving
     * diminishing returns, and any non-zero axis counts as at least the
     * configured minimum magnitude. Never stored, so it can't feed back into
     * crafting.
     *
     * @param profile A stored profile.
     * @return The corresponding Effective Profile.
     */
    public static SpiceProfile effective(SpiceProfile profile) {
        return profile.map(value -> Math.copySign(effectiveValue(Math.abs(value)), value));
    }

    /**
     * The saturation of {@link #effective(SpiceProfile)} for one magnitude.
     *
     * @param magnitude A raw, non-negative value of one axis.
     * @return Its effective value: {@code 0} for {@code 0}, otherwise
     *         saturated towards the soft cap but at least the minimum.
     */
    public static double effectiveValue(double magnitude) {
        if (magnitude <= 0D)
            return 0D;
        double cap = Services.CONFIG.getFlavorSoftCap();
        double minimum = Services.CONFIG.getFlavorMinimumAxisValue();
        return Math.max(cap * Math.tanh(magnitude / cap), minimum);
    }
}
