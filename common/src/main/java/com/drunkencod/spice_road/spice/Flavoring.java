package com.drunkencod.spice_road.spice;

import java.util.List;

import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import com.drunkencod.spice_road.item.SpiceItemTags;
import com.drunkencod.spice_road.platform.Services;
import com.drunkencod.spice_road.registry.ModDataComponents;
import com.drunkencod.spice_road.spice.region.SeededHash;

/**
 * Carries the flavor of a recipe's ingredients over to its outputs. Every
 * crafting/cooking hook (vanilla recipe classes and mod compat) goes through
 * here, so the rules are the same everywhere:
 * <ul>
 * <li>Each consumed ingredient (one item per slot) contributes its Spice
 * Profile: its Profile Override if present, otherwise its item's Default
 * Profile, otherwise nothing.</li>
 * <li>The sum is split evenly across every Flavor Carrier output item, so
 * flavor is conserved (crafting 9 into 1 and back yields the original
 * flavor). Non-carrier outputs simply lose it.</li>
 * <li>When cooking, each axis of an output's share is multiplied by a
 * reproducible pseudo-random factor within the configured variance, seeded by
 * the share and the output item.</li>
 * <li>The share is truncated towards zero to
 * {@value #DECIMAL_PLACES} decimal places and added to the output's own
 * profile. Stored values are never capped - diminishing returns only apply
 * when reading an Effective Profile.</li>
 * <li>An output ending up with its item's Default Profile carries no
 * Profile Override, so it keeps stacking with plain stacks.</li>
 * </ul>
 */
public final class Flavoring {

    /** Decimal places stored Spice Profile shares are truncated to. */
    public static final int DECIMAL_PLACES = 2;

    private static final double PRECISION = Math.pow(10, DECIMAL_PLACES);

    /**
     * Absorbs floating point error before truncating, so e.g.
     * {@code 0.8999999999} truncates to {@code 0.9} rather than {@code 0.89}.
     */
    private static final double TRUNCATION_EPSILON = 1e-7;

    private Flavoring() {
    }

    /**
     * Applies the ingredients' flavor to a single recipe output.
     *
     * @param result  The freshly assembled output, modified in place.
     * @param inputs  Every consumed ingredient stack, one item consumed per
     *                non-empty stack. Empty stacks are ignored.
     * @param cooking Whether the recipe cooks its ingredients, applying the
     *                cooking variance.
     * @return {@code result}, for chaining.
     */
    public static ItemStack apply(ItemStack result, Iterable<ItemStack> inputs, boolean cooking) {
        applyAll(List.of(result), inputs, cooking);
        return result;
    }

    /**
     * Applies the ingredients' flavor to several outputs of one recipe (e.g.
     * a cutting board yielding multiple stacks), splitting it across all
     * Flavor Carrier output items.
     *
     * @param results The freshly assembled outputs, modified in place.
     * @param inputs  Every consumed ingredient stack, one item consumed per
     *                non-empty stack. Empty stacks are ignored.
     * @param cooking Whether the recipe cooks its ingredients, applying the
     *                cooking variance.
     */
    public static void applyAll(List<ItemStack> results, Iterable<ItemStack> inputs, boolean cooking) {
        SpiceProfile inherited = inheritedFlavor(inputs);
        if (inherited.isZero())
            return;
        int carrierCount = 0;
        for (ItemStack result : results) {
            if (isFlavorCarrier(result))
                carrierCount += result.getCount();
        }
        if (carrierCount == 0)
            return;
        SpiceProfile share = truncate(inherited.scale(1D / carrierCount));
        for (ItemStack result : results) {
            if (isFlavorCarrier(result))
                addFlavor(result, cooking ? truncate(share.scale(cookingVariance(share, result.getItem()))) : share);
        }
    }

    /**
     * A Flavor Carrier is an item that may hold a Profile Override: anything
     * edible or in {@link SpiceItemTags#RETAINS_FLAVOR}, except for anything
     * in {@link SpiceItemTags#UNSEASONABLE} and non-edible Spice Items (a
     * recipe producing one authors a new spice with its own Default Profile).
     *
     * @param stack The stack to check.
     * @return Whether {@code stack} is a Flavor Carrier.
     */
    public static boolean isFlavorCarrier(ItemStack stack) {
        if (stack.isEmpty() || stack.is(SpiceItemTags.UNSEASONABLE))
            return false;
        if (stack.has(DataComponents.FOOD))
            return true;
        if (SpiceProfileRegistry.getDefault(stack.getItem()).isPresent())
            return false;
        return stack.is(SpiceItemTags.RETAINS_FLAVOR);
    }

    /**
     * @param profile The profile to truncate.
     * @return {@code profile} with every axis truncated towards zero to
     *         {@value #DECIMAL_PLACES} decimal places.
     */
    public static SpiceProfile truncate(SpiceProfile profile) {
        return profile.map(value -> Math.signum(value)
                * Math.floor(Math.abs(value) * PRECISION + TRUNCATION_EPSILON) / PRECISION);
    }

    private static SpiceProfile inheritedFlavor(Iterable<ItemStack> inputs) {
        SpiceProfile sum = SpiceProfile.ZERO;
        for (ItemStack input : inputs) {
            if (!input.isEmpty())
                sum = SpiceProfiles.get(input).map(sum::add).orElse(sum);
        }
        return sum;
    }

    /**
     * Adds a share of inherited flavor to a stack's own profile, storing the
     * result as a Profile Override unless it equals the item's Default
     * Profile.
     */
    private static void addFlavor(ItemStack result, SpiceProfile share) {
        if (share.isZero())
            return;
        SpiceProfile flavored = SpiceProfiles.get(result).orElse(SpiceProfile.ZERO).add(share);
        SpiceProfile itemDefault = SpiceProfileRegistry.getDefault(result.getItem()).orElse(SpiceProfile.ZERO);
        if (flavored.equals(itemDefault))
            result.remove(ModDataComponents.SPICE_PROFILE.get());
        else
            result.set(ModDataComponents.SPICE_PROFILE.get(), flavored);
    }

    /**
     * @return One multiplier per {@link FlavorAxis}, each within the
     *         configured cooking variance, deterministically derived from
     *         {@code share} and {@code output}.
     */
    private static double[] cookingVariance(SpiceProfile share, Item output) {
        double min = Services.CONFIG.getCookingVarianceMin();
        double max = Services.CONFIG.getCookingVarianceMax();
        if (min > max) {
            double swap = min;
            min = max;
            max = swap;
        }
        FlavorAxis[] axes = FlavorAxis.values();
        long seed = BuiltInRegistries.ITEM.getKey(output).toString().hashCode();
        for (FlavorAxis axis : axes)
            seed = SeededHash.hash(seed, Double.doubleToLongBits(share.get(axis)), axis.ordinal());
        double[] factors = new double[axes.length];
        for (int i = 0; i < factors.length; i++)
            factors[i] = min + (max - min) * SeededHash.toUnitDouble(SeededHash.hash(seed, i, 0));
        return factors;
    }
}
