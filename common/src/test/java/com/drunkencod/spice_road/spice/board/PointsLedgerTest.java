package com.drunkencod.spice_road.spice.board;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.function.DoubleUnaryOperator;

import org.junit.jupiter.api.Test;

import com.drunkencod.spice_road.spice.FlavorAxis;
import com.drunkencod.spice_road.spice.SpiceProfile;

class PointsLedgerTest {

    private static final DoubleUnaryOperator HALVE = value -> value * 0.5;

    private static SpiceProfile profile(Object... pairs) {
        double[] raw = new double[FlavorAxis.values().length];
        for (int i = 0; i < pairs.length; i += 2)
            raw[((FlavorAxis) pairs[i]).ordinal()] = ((Number) pairs[i + 1]).doubleValue();
        return new SpiceProfile(raw);
    }

    @Test
    void pointsAreTheEffectiveMagnitudeAndThePoleIsTheRawSign() {
        PointsLedger ledger = PointsLedger.start(
                profile(FlavorAxis.HEAT_COOLING, 4, FlavorAxis.SWEET_BITTER, -2), HALVE);
        assertEquals(2D, ledger.points(FlavorAxis.HEAT_COOLING));
        assertEquals(1, ledger.pole(FlavorAxis.HEAT_COOLING));
        assertEquals(1D, ledger.points(FlavorAxis.SWEET_BITTER));
        assertEquals(-1, ledger.pole(FlavorAxis.SWEET_BITTER));
        assertEquals(0D, ledger.points(FlavorAxis.SOUR_MELLOW));
        assertEquals(0, ledger.pole(FlavorAxis.SOUR_MELLOW));
    }

    @Test
    void spendingLowersPointsAndALaterSpiceRaisesThemAgain() {
        PointsLedger ledger = PointsLedger.start(profile(FlavorAxis.HEAT_COOLING, 2), value -> value);
        ledger.spend(FlavorAxis.HEAT_COOLING, 1.5);
        assertEquals(0.5, ledger.points(FlavorAxis.HEAT_COOLING), 1e-9);
        ledger.setRaw(profile(FlavorAxis.HEAT_COOLING, 3));
        assertEquals(1.5, ledger.points(FlavorAxis.HEAT_COOLING), 1e-9);
    }

    @Test
    void aSpiceAgainstThePoleCancelsPointsButNeverRefundsSpentOnes() {
        PointsLedger ledger = PointsLedger.start(profile(FlavorAxis.HEAT_COOLING, 3), value -> value);
        ledger.spend(FlavorAxis.HEAT_COOLING, 2);
        ledger.setRaw(profile(FlavorAxis.HEAT_COOLING, 1.5));
        assertEquals(0D, ledger.points(FlavorAxis.HEAT_COOLING), "1.5 earned, 2 already spent");
        ledger.setRaw(profile(FlavorAxis.HEAT_COOLING, -4));
        assertEquals(0D, ledger.points(FlavorAxis.HEAT_COOLING), "crossing over to the other side counts as nothing");
        assertEquals(1, ledger.pole(FlavorAxis.HEAT_COOLING), "the pole is fixed at the start");
        assertEquals(2D, ledger.spent(FlavorAxis.HEAT_COOLING));
    }

    @Test
    void anAxisThatStartedWithoutAPoleNeverGainsPoints() {
        PointsLedger ledger = PointsLedger.start(profile(FlavorAxis.HEAT_COOLING, 1), value -> value);
        ledger.setRaw(profile(FlavorAxis.HEAT_COOLING, 1, FlavorAxis.SWEET_BITTER, 5));
        assertEquals(0, ledger.pole(FlavorAxis.SWEET_BITTER));
        assertEquals(0D, ledger.points(FlavorAxis.SWEET_BITTER));
    }
}
