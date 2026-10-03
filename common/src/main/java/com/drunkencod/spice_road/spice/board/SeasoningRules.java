package com.drunkencod.spice_road.spice.board;

import com.drunkencod.spice_road.spice.effect.EffectCatalog;

/**
 * The numbers and the catalog a {@link SeasoningRun} plays by.
 *
 * @param stepCost        Points one step costs, paid from the axis of its direction.
 * @param lockInCostRing2 Points locking in a ring 2 effect cell costs.
 * @param lockInCostRing3 Points locking in a ring 3 effect cell costs.
 * @param lockInCostRing4 Points locking in a ring 4 effect cell costs.
 * @param maxEffects      Most distinct effects the food can hold.
 * @param catalog         The Seasoning Effect catalog.
 */
public record SeasoningRules(double stepCost, double lockInCostRing2, double lockInCostRing3, double lockInCostRing4,
        int maxEffects, EffectCatalog catalog) {

    /**
     * @param ring The ring of an effect cell, {@code 2} to {@code 4}.
     * @return The points locking in a cell of that ring costs.
     */
    public double lockInCost(int ring) {
        return switch (ring) {
            case 2 -> lockInCostRing2;
            case 3 -> lockInCostRing3;
            default -> lockInCostRing4;
        };
    }
}
