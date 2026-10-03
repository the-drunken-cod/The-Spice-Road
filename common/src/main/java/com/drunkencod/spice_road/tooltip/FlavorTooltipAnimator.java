package com.drunkencod.spice_road.tooltip;

import java.util.HashMap;
import java.util.Map;

import com.drunkencod.spice_road.spice.SpiceProfile;

/**
 * Drives the count-up of Flavor Axis tooltips. Tooltips are rebuilt every
 * frame, so the start of each animation is remembered per shown
 * {@link SpiceProfile}, and forgotten once its tooltip hasn't been requested
 * for {@link #GAP_MS}, so it replays the next time it appears.
 * <p>
 * Values advance in whole steps along an ease-out curve, so ticks come
 * quickly at first and the gaps between them grow towards the end.
 */
final class FlavorTooltipAnimator {

    /**
     * Time without a request after which an animation counts as over, in
     * milliseconds.
     */
    static final long GAP_MS = 100L;

    /** Number of steps a bar's full scale is divided into. */
    static final int STEPS_PER_SCALE = 10;

    /**
     * Used to tweak the easing function. Lower = more "front-loaded".
     * Recommended 1.0-3.0
     */
    static final double EASE_FN_EXPONENT = 1.75D;

    /** Which tooltip an animation belongs to. */
    private record Key(SpiceProfile profile, double barScale) {
    }

    /**
     * Animation state of one {@link Key}.
     *
     * @param startMs    Time the animation started at
     * @param lastSeenMs Time the tooltip was last requested at
     */
    private record Run(long startMs, long lastSeenMs) {
    }

    private static final Map<Key, Run> RUNS = new HashMap<>();

    private FlavorTooltipAnimator() {
    }

    /**
     * Registers a request for a tooltip and returns how far its animation is.
     * Starts a new animation if the tooltip wasn't shown recently.
     *
     * @param profile    The Spice Profile being shown
     * @param barScale   Score magnitude at which a bar is full
     * @param durationMs Total duration of the animation, in milliseconds
     * @param nowMs      The current time, in milliseconds
     * @return The linear progress between {@code 0} and {@code 1}
     */
    static double progress(SpiceProfile profile, double barScale, int durationMs, long nowMs) {
        RUNS.values().removeIf(run -> nowMs - run.lastSeenMs() > GAP_MS);
        Key key = new Key(profile, barScale);
        Run run = RUNS.get(key);
        long start = run == null ? nowMs : run.startMs();
        RUNS.put(key, new Run(start, nowMs));
        return durationMs <= 0 ? 1D : Math.min(1D, (nowMs - start) / (double) durationMs);
    }

    /**
     * @param value    The final score
     * @param barScale Score magnitude at which a bar is full, of which one
     *                 {@link #STEPS_PER_SCALE}th is the step size
     * @param progress Linear progress between {@code 0} and {@code 1}
     * @return The score to show: a whole multiple of the step size with the
     *         sign of {@code value} (so the pole never changes mid-animation),
     *         or exactly {@code value} once every step is done
     */
    static double displayed(double value, double barScale, double progress) {
        double step = barScale / STEPS_PER_SCALE;
        int totalSteps = (int) Math.ceil(Math.abs(value) / step - 1e-9);
        int shownSteps = (int) Math.floor(easeOut(progress) * totalSteps);
        if (shownSteps >= totalSteps)
            return value;
        return Math.copySign(Math.round(shownSteps * step * 1e6) / 1e6, value);
    }

    /**
     * @param x Linear progress between {@code 0} and {@code 1}
     * @return The cubic ease-out of {@code x}
     */
    static double easeOut(double x) {
        double rest = 1D - Math.min(Math.max(x, 0D), 1D);
        return 1D - Math.pow(rest, EASE_FN_EXPONENT);
    }
}
