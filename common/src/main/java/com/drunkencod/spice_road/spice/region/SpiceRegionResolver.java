package com.drunkencod.spice_road.spice.region;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import com.drunkencod.spice_road.platform.Services;
import com.drunkencod.spice_road.spice.Climate;
import com.drunkencod.spice_road.spice.Spice;
import com.drunkencod.spice_road.spice.Tier;

/**
 * Pure, loader-agnostic implementation of the Spice Region (cell) system with
 * two responsibilities, kept as separate static entry points (plus
 * {@link #resolve} combining both) so a debug renderer or tests can inspect
 * intermediate results without re-deriving them:
 * <ul>
 * <li>{@link #resolveCell(long, long, double, int, int)} - a jittered,
 * Voronoi-style grid that assigns every world position to a cell with a
 * static, world-seed-dependent identity ({@link SpiceCell#seed()}).</li>
 * <li>{@link #resolveSpice(SpiceCell, Climate, double)} - a deterministic,
 * Tier-weighted pick of one {@link Spice} from the {@link Climate} Bucket
 * applying at a point, driven by that cell's seed.</li>
 * </ul>
 * <p>
 * <b>Call-site TODO:</b> deriving the {@link Climate} at a given position
 * (from the real biome's temperature/humidity, overridable via datapack
 * biome tags such as {@code spice_road:biome/climate/temperate}) is
 * intentionally not implemented here - it needs biome-tag datagen plumbing
 * that is out of scope for this pass. Callers currently must supply
 * {@link Climate} themselves; a
 * {@code Climate.fromBiome(Holder<Biome>)}-shaped helper (plus tag
 * lookup and force-add/force-remove override lists) is the natural home for
 * that once the tags exist.
 */
public final class SpiceRegionResolver {

    private SpiceRegionResolver() {
    }

    /**
     * All {@link Spice}s grouped by {@link Climate}, i.e. the Climate
     * Buckets. Computed once - {@link Spice} is a fixed, non-datapack
     * extensible enum.
     */
    private static final Map<Climate, List<Spice>> CLIMATE_BUCKETS = Arrays.stream(Spice.values())
            .collect(Collectors.groupingBy(Spice::getClimate));

    /** Distinguishes a cell's identity-seed hash from its jitter-offset hash. */
    private static final long CELL_SEED_TAG = 0x9E3779B97F4A7C15L;

    /**
     * Distinguishes a climate-specific Spice-pick hash from other uses of a cell's
     * seed.
     */
    private static final long CLIMATE_PICK_TAG = 0xC2B2AE3D27D4EB4FL;

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
     * Deterministically picks one {@link Spice} from {@code climate}'s
     * Climate Bucket for {@code cell}, weighted so rarer {@link Tier}s occur
     * less often across cells.
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
     * different {@link Climate} (a climate-specific salt is mixed into the
     * pick seed). A region spanning multiple real-world biomes/climates can yield
     * different concrete Spices as climate changes across it, while still being a
     * stable, repeatable pick for any given point.
     *
     * @param cell               The {@link SpiceCell} to pick within (see
     *                           {@link #resolveCell(long, long, double, int, int)}).
     * @param climate            The {@link Climate} to pick from - the
     *                           caller's responsibility to derive, see this
     *                           class's TODO.
     * @param clusteringStrength Exponent applied to each candidate's base
     *                           Tier weight; see the design note above.
     *                           Configurable, see
     *                           {@code IConfigHelper#getSpiceRegionClusteringStrength()}.
     * @return The resolved {@link Spice}, or empty if no {@link Spice}
     *         belongs to {@code climate}'s Climate Bucket.
     */
    public static Optional<Spice> resolveSpice(SpiceCell cell, Climate climate, double clusteringStrength) {
        List<Spice> bucket = CLIMATE_BUCKETS.getOrDefault(climate, List.of());
        if (bucket.isEmpty())
            return Optional.empty();

        long pickSeed = SeededHash.hash(cell.seed(), climate.ordinal(), CLIMATE_PICK_TAG);

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
     * @param worldSeed          The world seed.
     * @param salt               See {@link #resolveCell(long, long, double, int, int)}.
     * @param cellScale          See {@link #resolveCell(long, long, double, int, int)}.
     * @param clusteringStrength See
     *                           {@link #resolveSpice(SpiceCell, Climate, double)}.
     * @param climate            The {@link Climate} to pick from.
     * @param x                  World-space X coordinate to resolve.
     * @param z                  World-space Z coordinate to resolve.
     * @return The full {@link SpiceRegionResult} for this query.
     */
    public static SpiceRegionResult resolve(long worldSeed, long salt, double cellScale, double clusteringStrength,
            Climate climate, int x, int z) {
        SpiceCell cell = resolveCell(worldSeed, salt, cellScale, x, z);
        Optional<Spice> spice = resolveSpice(cell, climate, clusteringStrength);
        return new SpiceRegionResult(cell, climate, spice);
    }
}
