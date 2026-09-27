package com.drunkencod.spice_road.spice.region;

import net.minecraft.core.BlockPos;

import com.drunkencod.spice_road.spice.Climate;
import com.drunkencod.spice_road.spice.Spice;

/**
 * A non-barren Region Heart: the representative point of a Spice Region,
 * along with the Heart Spice it resolves to.
 *
 * @param cell    The Spice Region cell this is the heart of. Its
 *                {@link SpiceCell#distance()} is measured from whatever
 *                position the heart was looked up from.
 * @param pos     The heart's world position, at the estimated surface height.
 * @param climate The {@link Climate} found at {@code pos}.
 * @param spice   The Heart Spice.
 */
public record RegionHeart(SpiceCell cell, BlockPos pos, Climate climate, Spice spice) {
}
