package com.drunkencod.spice_road.spice.region;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import org.jetbrains.annotations.Nullable;

import com.drunkencod.spice_road.platform.Services;
import com.drunkencod.spice_road.spice.Climate;
import com.drunkencod.spice_road.spice.Spice;
import com.drunkencod.spice_road.spice.Tier;
import com.drunkencod.spice_road.spice.dimension.SpiceDimension;

/**
 * Pure, loader-agnostic implementation of the Spice Region (cell) system with
 * two responsibilities, kept as separate static entry points (plus
 * {@link #resolve} combining both) so a debug renderer or tests can inspect
 * intermediate results without re-deriving them:
 * <ul>
 * <li>{@link #resolveCell(long, long, double, int, int)} - a jittered,
 * Voronoi-style grid that assigns every world position to a cell with a
 * static, world-seed-dependent identity ({@link SpiceCell#seed()}).</li>
 * <li>{@link #resolveSpice(SpiceCell, SpiceDimension, Climate, double)} - a
 * deterministic, Tier-weighted pick of one {@link Spice} from a
 * {@link Climate}'s bucket in a {@link SpiceDimension}, driven by that cell's
 * seed.</li>
 * </ul>
 * <p>
 * Deriving the {@link Climate} at a position is up to the caller, see
 * {@link SpiceDimension#climateOf}.
 */
public final class SpiceRegionResolver {

    private SpiceRegionResolver() {
    }

    /** Distinguishes a cell's identity-seed hash from its jitter-offset hash. */
    private static final long CELL_SEED_TAG = 0x9E3779B97F4A7C15L;

    /**
     * Distinguishes a climate-specific Spice-pick hash from other uses of a cell's
     * seed.
     */
    private static final long CLIMATE_PICK_TAG = 0xC2B2AE3D27D4EB4FL;

    /** Distinguishes a cell's spiceless roll from other uses of its seed. */
    private static final long SPICELESS_TAG = 0x165667B19E3779F9L;

    /**
     * Resolves the {@link SpiceCell} a world position falls into.
     * <p>
     * Algorithm: a jittered grid, the standard game-dev approximation of
     * Voronoi/cellular noise (chosen over smooth Perlin because Spice Regions need
     * clean, contiguous boundaries rather than a smoothly-thresholded gradient).
     * Space is divided into {@code cellScale}-sized squares; each square gets one
     * "feature point", pseudo-randomly offset within its own bounds and
     * seeded off {@code worldSeed} plus the square's grid coordinates, so it
     * is stable forever for a given seed and scale. A queried position
     * belongs to whichever feature point is nearest to it. Only the 3x3
     * neighborhood of squares around the position's own square is searched;
     * since a square's feature point never leaves that square's own bounds,
     * this neighborhood is always guaranteed to contain the true nearest
     * point.
     *
     * @param worldSeed The world seed, so cell layout is unique per world.
     * @param salt      Salt mixed into {@code worldSeed}; {@code 0} leaves it
     *                  unchanged. Configurable; see
     *                  {@code IConfigHelper#getSpiceRegionSalt()}.
     * @param cellScale The (approximate) edge length of a cell, in blocks.
     *                  Configurable; see
     *                  {@code IConfigHelper#getSpiceRegionCellScale()}.
     * @param x         World-space X coordinate to resolve.
     * @param z         World-space Z coordinate to resolve.
     * @throws IllegalArgumentException If {@code cellScale} is not positive.
     */
    public static SpiceCell resolveCell(long worldSeed, long salt, double cellScale, int x, int z) {
        if (cellScale <= 0)
            throw new IllegalArgumentException("cellScale must be positive, got " + cellScale);

        int originGridX = (int) Math.floor(x / cellScale);
        int originGridZ = (int) Math.floor(z / cellScale);

        SpiceCell best = null;
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                SpiceCell candidate = cellAtGrid(worldSeed, salt, cellScale, originGridX + dx, originGridZ + dz, x, z);
                if (best == null || candidate.distance() < best.distance())
                    best = candidate;
            }
        }
        return best;
    }

    /**
     * Resolves the {@link SpiceCell} of one specific square of the jittered
     * grid, regardless of whether {@code (x, z)} actually falls into it.
     * Lets callers walk the grid cell by cell (e.g. when searching for a
     * Region Heart) instead of sampling world positions.
     *
     * @param worldSeed The world seed.
     * @param salt      See {@link #resolveCell(long, long, double, int, int)}.
     * @param cellScale See {@link #resolveCell(long, long, double, int, int)}.
     * @param gridX     Grid-space X coordinate of the cell.
     * @param gridZ     Grid-space Z coordinate of the cell.
     * @param x         World-space X coordinate {@link SpiceCell#distance()} is
     *                  measured from.
     * @param z         World-space Z coordinate {@link SpiceCell#distance()} is
     *                  measured from.
     * @return The cell at {@code (gridX, gridZ)}.
     * @throws IllegalArgumentException If {@code cellScale} is not positive.
     */
    public static SpiceCell cellAtGrid(long worldSeed, long salt, double cellScale, int gridX, int gridZ, int x,
            int z) {
        if (cellScale <= 0)
            throw new IllegalArgumentException("cellScale must be positive, got " + cellScale);

        // mix64(0) == 0, so a salt of 0 keeps the unsalted layout
        worldSeed ^= SeededHash.mix64(salt);

        long pointSeed = SeededHash.hash(worldSeed, gridX, gridZ);
        double jitterX = SeededHash.toUnitDouble(pointSeed);
        double jitterZ = SeededHash.toUnitDouble(SeededHash.mix64(pointSeed));

        double centerX = (gridX + jitterX) * cellScale;
        double centerZ = (gridZ + jitterZ) * cellScale;

        double deltaX = centerX - x;
        double deltaZ = centerZ - z;

        long cellSeed = SeededHash.hash(worldSeed ^ CELL_SEED_TAG, gridX, gridZ);
        return new SpiceCell(gridX, gridZ, cellSeed, centerX, centerZ,
                Math.sqrt((deltaX * deltaX) + (deltaZ * deltaZ)));
    }

    /**
     * Checks whether {@code cell} is a Spiceless Region, containing no Spices in
     * any {@link Climate}. Each cell has one stable roll derived from its seed,
     * compared against the configured chance, so raising the chance only turns
     * additional cells spiceless and never reshuffles the others.
     *
     * @param cell The {@link SpiceCell} to check.
     * @return Whether {@code cell} is spiceless. Configurable, see
     *         {@code IConfigHelper#getSpiceRegionSpicelessChance()}.
     */
    public static boolean isSpiceless(SpiceCell cell) {
        double roll = SeededHash.toUnitDouble(SeededHash.mix64(cell.seed() ^ SPICELESS_TAG));
        return roll < Services.CONFIG.getSpiceRegionSpicelessChance();
    }

    /**
     * Deterministically picks one {@link Spice} from {@code climate}'s Climate
     * Bucket in {@code dimension} for {@code cell}, weighted so rarer
     * {@link Tier}s occur less often across cells.
     * <p>
     * <b>Design note (tier spacing).</b> Each pick is an independent weighted
     * random draw where a {@link Tier}'s base weight roughly halves per rarity
     * step, and {@code clusteringStrength} is applied as an exponent over
     * those weights. {@code clusteringStrength > 1} widens the
     * gap between common and rare further (a stronger travel/trade
     * incentive - the dedicated-server default); {@code clusteringStrength
     * < 1} flattens it back towards a uniform pick (the singleplayer
     * default).
     * <p>
     * The same cell can resolve to a different {@link Spice} for a
     * different {@link Climate} (a climate-specific salt, hashed from the
     * Climate's ID, is mixed into the pick seed). A region spanning multiple
     * real-world biomes/climates can yield different concrete Spices as climate
     * changes across it, while still being a stable, repeatable pick for any
     * given point.
     *
     * @param cell               The {@link SpiceCell} to pick within (see
     *                           {@link #resolveCell(long, long, double, int, int)}).
     * @param dimension          The dimension whose Climate Buckets to pick from.
     * @param climate            The {@link Climate} to pick from, or {@code null}
     *                           if the biome has none.
     * @param clusteringStrength Exponent applied to each candidate's base
     *                           Tier weight; see the design note above.
     *                           Configurable, see
     *                           {@code IConfigHelper#getSpiceRegionClusteringStrength()}.
     * @return The resolved {@link Spice}, or empty if {@code cell} is
     *         {@linkplain #isSpiceless spiceless}, there is no {@code climate},
     *         or no {@link Spice} belongs to {@code climate}'s Climate Bucket.
     */
    public static Optional<Spice> resolveSpice(SpiceCell cell, SpiceDimension dimension, @Nullable Climate climate,
            double clusteringStrength) {
        if (climate == null || isSpiceless(cell))
            return Optional.empty();

        List<Spice> bucket = dimension.bucket(climate);
        if (bucket.isEmpty())
            return Optional.empty();

        long pickSeed = SeededHash.hash(cell.seed(), climate.seedKey(), CLIMATE_PICK_TAG);

        double[] weights = new double[bucket.size()];
        double totalWeight = 0;
        for (int i = 0; i < bucket.size(); i++) {
            double weight = Math.pow(Services.CONFIG.getSpiceRegionTierWeight(bucket.get(i).getTier()),
                    clusteringStrength);
            weights[i] = weight;
            totalWeight += weight;
        }
        if (totalWeight <= 0) {
            // Every tier in this bucket is weighted zero: pick uniformly instead.
            Arrays.fill(weights, 1.0);
            totalWeight = weights.length;
        }

        double roll = SeededHash.toUnitDouble(pickSeed) * totalWeight;
        double cumulative = 0;
        for (int i = 0; i < bucket.size(); i++) {
            cumulative += weights[i];
            if (roll < cumulative)
                return Optional.of(bucket.get(i));
        }

        // Floating-point edge case (roll landed exactly on totalWeight): fall
        // back to the last candidate rather than Optional.empty().
        return Optional.of(bucket.get(bucket.size() - 1));
    }

    /**
     * Convenience wrapper combining {@link #resolveCell} and
     * {@link #resolveSpice} into the single {@link SpiceRegionResult} shape
     * the F3 debug renderer needs.
     *
     * @param layout             The dimension's region layout.
     * @param clusteringStrength See
     *                           {@link #resolveSpice(SpiceCell, SpiceDimension, Climate, double)}.
     * @param climate            The {@link Climate} to pick from, or {@code null}
     *                           if the biome has none.
     * @param x                  World-space X coordinate to resolve.
     * @param z                  World-space Z coordinate to resolve.
     * @return The full {@link SpiceRegionResult} for this query.
     */
    public static SpiceRegionResult resolve(SpiceRegionLayout layout, double clusteringStrength,
            @Nullable Climate climate, int x, int z) {
        SpiceCell cell = layout.cell(x, z);
        Optional<Spice> spice = resolveSpice(cell, layout.dimension(), climate, clusteringStrength);
        return new SpiceRegionResult(cell, Optional.ofNullable(climate), spice, isSpiceless(cell));
    }
}
