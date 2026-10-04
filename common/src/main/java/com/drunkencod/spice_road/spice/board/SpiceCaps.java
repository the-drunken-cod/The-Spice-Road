package com.drunkencod.spice_road.spice.board;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * The caps on how many spices count towards one food's points.
 */
public final class SpiceCaps {

    private SpiceCaps() {
    }

    /**
     * Applies the spice caps to a food's true amounts. Each kind counts at most
     * {@code maxPerKind}; if the total is still above {@code maxTotal}, the
     * excess is trimmed off the kinds one unit at a time, largest amount first
     * and ties by {@code tieBreak}, so the result is deterministic.
     *
     * @param amounts    The food's true amount of each kind, all above zero.
     * @param tieBreak   Orders kinds with equal amounts; the lowest is trimmed first.
     * @param maxPerKind The most one kind counts for.
     * @param maxTotal   The most all kinds count for together.
     * @param <K>        The kind of spice, normally an item.
     * @return The amounts that count, kinds trimmed to nothing left out.
     */
    public static <K> Map<K, Double> capAmounts(Map<K, Double> amounts, Comparator<K> tieBreak, int maxPerKind,
            int maxTotal) {
        Map<K, Double> capped = new LinkedHashMap<>();
        amounts.forEach((kind, amount) -> capped.put(kind, round(Math.min(amount, maxPerKind))));
        double excess = round(capped.values().stream().mapToDouble(Double::doubleValue).sum() - maxTotal);
        Comparator<Map.Entry<K, Double>> order = Map.Entry.<K, Double>comparingByValue().reversed()
                .thenComparing(Map.Entry.comparingByKey(tieBreak));
        while (excess > 0D) {
            List<Map.Entry<K, Double>> entries = new ArrayList<>(capped.entrySet());
            entries.sort(order);
            Map.Entry<K, Double> largest = entries.get(0);
            double cut = Math.min(Math.min(excess, 1D), largest.getValue());
            capped.put(largest.getKey(), round(largest.getValue() - cut));
            excess = round(excess - cut);
        }
        capped.values().removeIf(amount -> amount <= 0D);
        return capped;
    }

    private static double round(double value) {
        return Math.round(value * 100D) / 100D;
    }
}
