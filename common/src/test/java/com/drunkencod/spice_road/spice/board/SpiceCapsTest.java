package com.drunkencod.spice_road.spice.board;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.Map;

import org.junit.jupiter.api.Test;


class SpiceCapsTest {

    private static double sum(Map<String, Double> amounts) {
        return amounts.values().stream().mapToDouble(Double::doubleValue).sum();
    }

    @Test
    void eachKindCountsAtMostThePerKindCap() {
        Map<String, Double> capped = SpiceCaps.capAmounts(Map.of("a", 5D, "b", 1.5D), Comparator.naturalOrder(), 3,
                16);
        assertEquals(3D, capped.get("a"));
        assertEquals(1.5D, capped.get("b"));
    }

    @Test
    void excessOverTheTotalIsTrimmedLargestFirstTiesByOrder() {
        Map<String, Double> amounts = new LinkedHashMap<>();
        for (String kind : new String[] { "f", "e", "d", "c", "b", "a" })
            amounts.put(kind, 3D);
        Map<String, Double> capped = SpiceCaps.capAmounts(amounts, Comparator.naturalOrder(), 3, 16);
        assertEquals(16D, sum(capped));
        assertEquals(2D, capped.get("a"));
        assertEquals(2D, capped.get("b"));
        assertEquals(3D, capped.get("c"));
        assertEquals(3D, capped.get("f"));
    }

    @Test
    void trimmingGoesForTheLargestAmountFirst() {
        Map<String, Double> capped = SpiceCaps.capAmounts(Map.of("a", 1D, "b", 3D, "c", 3D, "d", 3D, "e", 3D,
                "f", 3D, "g", 2D), Comparator.naturalOrder(), 3, 16);
        assertEquals(16D, sum(capped));
        assertEquals(1D, capped.get("a"));
        assertEquals(2D, capped.get("g"));
        assertEquals(2D, capped.get("b"));
    }

    @Test
    void fractionalAmountsAreTrimmedWithoutGoingNegative() {
        Map<String, Double> capped = SpiceCaps.capAmounts(Map.of("a", 0.4D, "b", 3D, "c", 3D, "d", 3D, "e", 3D,
                "f", 3D, "g", 2D), Comparator.naturalOrder(), 3, 16);
        assertEquals(16D, sum(capped), 1e-9);
        capped.values().forEach(amount -> assertEquals(true, amount > 0D));
    }

    @Test
    void anAmountUnderTheCapsIsKeptAsIs() {
        Map<String, Double> amounts = Map.of("a", 2.25D, "b", 0.75D);
        assertEquals(amounts, SpiceCaps.capAmounts(amounts, Comparator.naturalOrder(), 3, 16));
    }
}
