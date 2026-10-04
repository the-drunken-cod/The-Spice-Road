package com.drunkencod.spice_road.spice.board;

import java.util.List;
import java.util.function.DoubleUnaryOperator;

import com.drunkencod.spice_road.spice.FlavorAxis;
import com.drunkencod.spice_road.spice.SpiceProfile;

/**
 * What a session can still spend on each {@link FlavorAxis}. It keeps the
 * running raw sum of every spice added so far and what was already spent. The
 * pole of each axis is fixed when the ledger is {@link #start started}, so an
 * axis' points are its effective value measured along that pole, never below
 * zero, minus what was spent; an axis without a pole never has points. The same
 * rule serves the draft's preview, the start of a run and every later spice.
 */
public final class PointsLedger {

    private final int[] poles;
    private final double[] spent;
    private final DoubleUnaryOperator effective;
    private SpiceProfile raw;

    /**
     * @param poles     {@code 1}, {@code -1} or {@code 0} per axis, in {@link FlavorAxis} order.
     * @param raw       The raw sum of every spice so far.
     * @param spent     The points already spent per axis, in {@link FlavorAxis} order.
     * @param effective Turns a raw, non-negative value along a pole into its effective value.
     */
    public PointsLedger(int[] poles, SpiceProfile raw, double[] spent, DoubleUnaryOperator effective) {
        this.poles = poles.clone();
        this.raw = raw;
        this.spent = spent.clone();
        this.effective = effective;
    }

    /**
     * Starts a ledger: the poles are the signs of {@code raw} and nothing is spent.
     *
     * @param raw       The raw sum of the spices at the start of a run.
     * @param effective Turns a raw, non-negative value along a pole into its effective value.
     * @return The new ledger.
     */
    public static PointsLedger start(SpiceProfile raw, DoubleUnaryOperator effective) {
        int[] poles = new int[FlavorAxis.values().length];
        for (FlavorAxis axis : FlavorAxis.values())
            poles[axis.ordinal()] = (int) Math.signum(raw.get(axis));
        return new PointsLedger(poles, raw, new double[poles.length], effective);
    }

    /**
     * @param axis A Flavor Axis.
     * @return The points left on {@code axis}.
     */
    public double points(FlavorAxis axis) {
        int pole = poles[axis.ordinal()];
        if (pole == 0)
            return 0D;
        double along = Math.max(0D, pole * raw.get(axis));
        return Math.max(0D, effective.applyAsDouble(along) - spent[axis.ordinal()]);
    }

    /**
     * @param axis A Flavor Axis.
     * @return {@code 1} or {@code -1} for the pole fixed at the start, {@code 0} if the axis has none.
     */
    public int pole(FlavorAxis axis) {
        return poles[axis.ordinal()];
    }

    /**
     * @param axis A Flavor Axis.
     * @return The points spent on {@code axis} so far.
     */
    public double spent(FlavorAxis axis) {
        return spent[axis.ordinal()];
    }

    /**
     * @param axis   A Flavor Axis.
     * @param amount The points to spend.
     */
    public void spend(FlavorAxis axis, double amount) {
        spent[axis.ordinal()] += amount;
    }

    /** @return The raw sum of every spice so far. */
    public SpiceProfile raw() {
        return raw;
    }

    /**
     * @param newRaw The new raw sum, after a spice was added.
     */
    public void setRaw(SpiceProfile newRaw) {
        raw = newRaw;
    }

    /** @return The poles in {@link FlavorAxis} order, as a list. */
    public List<Integer> poles() {
        return java.util.Arrays.stream(poles).boxed().toList();
    }

    /** @return The points spent per axis in {@link FlavorAxis} order, as a list. */
    public List<Double> spentPoints() {
        return java.util.Arrays.stream(spent).boxed().toList();
    }
}
