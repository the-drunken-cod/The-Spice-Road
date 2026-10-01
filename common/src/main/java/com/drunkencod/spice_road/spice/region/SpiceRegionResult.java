package com.drunkencod.spice_road.spice.region;

import java.util.Optional;

import com.drunkencod.spice_road.spice.Climate;
import com.drunkencod.spice_road.spice.Spice;

/**
 * The full outcome of resolving a Spice Region query at one world position:
 * the {@link SpiceCell} it fell into, the {@link Climate} Bucket that was
 * rolled against, and the concrete {@link Spice} picked (if any).
 *
 * @param cell    The resolved {@link SpiceCell}.
 * @param climate The {@link Climate} the pick was rolled against.
 * @param spice   The resolved {@link Spice}, or empty if the region is
 *                spiceless or no {@link Spice} belongs to {@code climate}'s
 *                Climate Bucket.
 * @param spiceless Whether the cell is a Spiceless Region, see
 *                {@link SpiceRegionResolver#isSpiceless}.
 */
public record SpiceRegionResult(SpiceCell cell, Climate climate, Optional<Spice> spice, boolean spiceless) {
}
