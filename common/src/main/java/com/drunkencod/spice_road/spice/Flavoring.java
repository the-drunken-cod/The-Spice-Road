package com.drunkencod.spice_road.spice;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import com.drunkencod.spice_road.item.SpiceItemTags;
import com.drunkencod.spice_road.registry.ModDataComponents;

/**
 * Carries the spices of a recipe's ingredients over to its outputs as counted
 * Flavor Contributors. Every crafting/cooking hook (vanilla recipe classes and
 * mod compat) goes through here, so the rules are the same everywhere:
 * <ul>
 * <li>Each consumed ingredient (one item per slot) contributes one of itself if
 * it is a Spice Item, or its own {@link Seasoning} if it is seasoned food.
 * Anything else contributes nothing.</li>
 * <li>The summed amounts are split evenly across every Flavor Carrier output
 * item, so spices are conserved (crafting 9 into 1 and back yields the
 * original amounts) and never duplicated. Non-carrier outputs simply lose
 * them.</li>
 * <li>Each share is truncated towards zero to {@value #DECIMAL_PLACES}
 * decimal places and added to the output's own {@link Seasoning}. A share that
 * truncates to nothing seasons nothing.</li>
 * <li>Inputs' old effects don't carry over: effects are re-solved from the new
 * total.</li>
 * </ul>
 */
public final class Flavoring {

    /** Decimal places contributor amounts are kept to. */
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
     * Applies the ingredients' spices to a single recipe output.
     *
     * @param result The freshly assembled output, modified in place.
     * @param inputs Every consumed ingredient stack, one item consumed per
     *               non-empty stack. Empty stacks are ignored.
     * @return {@code result}, for chaining.
     */
    public static ItemStack apply(ItemStack result, Iterable<ItemStack> inputs) {
        applyAll(List.of(result), inputs);
        return result;
    }

    /**
     * Applies the ingredients' spices to several outputs of one recipe (e.g. a
     * cutting board yielding multiple stacks), splitting them across all Flavor
     * Carrier output items.
     *
     * @param results The freshly assembled outputs, modified in place.
     * @param inputs  Every consumed ingredient stack, one item consumed per
     *                non-empty stack. Empty stacks are ignored.
     */
    public static void applyAll(List<ItemStack> results, Iterable<ItemStack> inputs) {
        Seasoning inherited = inheritedSeasoning(inputs);
        if (inherited.contributors().isEmpty())
            return;
        int carrierCount = 0;
        for (ItemStack result : results) {
            if (isFlavorCarrier(result))
                carrierCount += result.getCount();
        }
        if (carrierCount == 0)
            return;
        Map<Item, Double> share = new LinkedHashMap<>();
        for (Map.Entry<Item, Double> entry : inherited.contributors().entrySet())
            share.put(entry.getKey(), truncate(entry.getValue() / carrierCount));
        Seasoning shareSeasoning = new Seasoning(share);
        if (shareSeasoning.contributors().isEmpty())
            return;
        for (ItemStack result : results) {
            if (isFlavorCarrier(result))
                addSeasoning(result, shareSeasoning);
        }
    }

    /**
     * A Flavor Carrier is an item that may hold a {@link Seasoning}: anything
     * edible or in {@link SpiceItemTags#RETAINS_FLAVOR}, except for anything in
     * {@link SpiceItemTags#UNSEASONABLE} and Spice Items (a recipe producing one
     * authors a new spice with its own Default Profile).
     *
     * @param stack The stack to check.
     * @return Whether {@code stack} is a Flavor Carrier.
     */
    public static boolean isFlavorCarrier(ItemStack stack) {
        if (stack.isEmpty() || stack.is(SpiceItemTags.UNSEASONABLE))
            return false;
        if (SpiceProfileRegistry.getDefault(stack.getItem()).isPresent())
            return false;
        return stack.has(DataComponents.FOOD) || stack.is(SpiceItemTags.RETAINS_FLAVOR);
    }

    /**
     * @param amount The amount to truncate.
     * @return {@code amount} truncated towards zero to {@value #DECIMAL_PLACES}
     *         decimal places.
     */
    public static double truncate(double amount) {
        return Math.signum(amount) * Math.floor(Math.abs(amount) * PRECISION + TRUNCATION_EPSILON) / PRECISION;
    }

    /**
     * @param amount The amount to round.
     * @return {@code amount} rounded to the nearest {@value #DECIMAL_PLACES}
     *         decimal places.
     */
    public static double round(double amount) {
        return Math.round(amount * PRECISION) / PRECISION;
    }

    /**
     * Sums what every input brings along: a seasoned food's own
     * {@link Seasoning}, or one of the item itself if it is a Spice Item with a
     * non-zero Default Profile.
     */
    private static Seasoning inheritedSeasoning(Iterable<ItemStack> inputs) {
        Map<Item, Double> sums = new LinkedHashMap<>();
        for (ItemStack input : inputs) {
            if (input.isEmpty())
                continue;
            Seasoning existing = input.get(ModDataComponents.SEASONING.get());
            if (existing != null)
                existing.contributors().forEach((item, amount) -> sums.merge(item, amount, Double::sum));
            else if (SpiceProfileRegistry.getDefault(input.getItem()).map(profile -> !profile.isZero()).orElse(false))
                sums.merge(input.getItem(), 1D, Double::sum);
        }
        return new Seasoning(sums);
    }

    /** Adds a share of inherited spices to a stack's own {@link Seasoning}. */
    private static void addSeasoning(ItemStack result, Seasoning share) {
        Seasoning existing = result.get(ModDataComponents.SEASONING.get());
        result.set(ModDataComponents.SEASONING.get(), existing == null ? share : existing.plus(share));
    }
}
