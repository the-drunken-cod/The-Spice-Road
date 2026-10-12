package com.drunkencod.spice_road.spice.region;

import java.util.Optional;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.biome.Biome;

import com.drunkencod.spice_road.platform.Services;
import com.drunkencod.spice_road.spice.Climate;
import com.drunkencod.spice_road.spice.Spice;
import com.drunkencod.spice_road.spice.dimension.SpiceDimension;
import com.drunkencod.spice_road.spice.dimension.SpiceDimensions;

/**
 * Everything needed to place and resolve the Spice Regions of one dimension:
 * its {@link SpiceDimension}, the world seed, and the region salt and cell
 * scale that dimension ends up with once the global config and its own
 * settings are combined.
 *
 * @param dimension The dimension's Spice Dimension.
 * @param worldSeed The world seed.
 * @param salt      The salt mixed into {@code worldSeed}.
 * @param cellScale The approximate edge length of a cell, in blocks.
 */
public record SpiceRegionLayout(SpiceDimension dimension, long worldSeed, long salt, double cellScale) {

    /**
     * @param level The level to resolve in.
     * @return Its layout, or empty if its dimension has no Spice Dimension or
     *         none of its Climate Buckets holds a Spice, so nothing there can
     *         resolve to a Spice.
     */
    public static Optional<SpiceRegionLayout> of(ServerLevel level) {
        return SpiceDimensions.get(level).filter(SpiceDimension::hasSpices)
                .map(dimension -> of(dimension, level.getSeed()));
    }

    /**
     * @param dimension The dimension's Spice Dimension.
     * @param worldSeed The world seed.
     * @return The layout, using the current config.
     */
    public static SpiceRegionLayout of(SpiceDimension dimension, long worldSeed) {
        return new SpiceRegionLayout(dimension, worldSeed, dimension.salt(Services.CONFIG.getSpiceRegionSalt()),
                dimension.cellScale(Services.CONFIG.getSpiceRegionCellScale()));
    }

    /**
     * @param x World-space X coordinate.
     * @param z World-space Z coordinate.
     * @return The cell {@code (x, z)} falls into, see
     *         {@link SpiceRegionResolver#resolveCell}.
     */
    public SpiceCell cell(int x, int z) {
        return SpiceRegionResolver.resolveCell(worldSeed, salt, cellScale, x, z);
    }

    /**
     * @param gridX Grid-space X coordinate of the cell.
     * @param gridZ Grid-space Z coordinate of the cell.
     * @param x     World-space X coordinate its distance is measured from.
     * @param z     World-space Z coordinate its distance is measured from.
     * @return The cell at {@code (gridX, gridZ)}, see
     *         {@link SpiceRegionResolver#cellAtGrid}.
     */
    public SpiceCell cellAtGrid(int gridX, int gridZ, int x, int z) {
        return SpiceRegionResolver.cellAtGrid(worldSeed, salt, cellScale, gridX, gridZ, x, z);
    }

    /**
     * Resolves the Spice Region at {@code pos}, using the Climate its biome has
     * in this dimension.
     *
     * @param biome The biome at {@code pos}.
     * @param pos   The position.
     * @return The full result.
     */
    public SpiceRegionResult resolve(Holder<Biome> biome, BlockPos pos) {
        Climate climate = dimension.climateOf(biome, pos).orElse(null);
        return SpiceRegionResolver.resolve(this, Services.CONFIG.getSpiceRegionClusteringStrength(), climate,
                pos.getX(), pos.getZ());
    }

    /**
     * @param biome The biome at {@code pos}.
     * @param pos   The position.
     * @return The Spice Region's Spice at {@code pos}, using the Climate found
     *         there.
     */
    public Optional<Spice> resolveSpice(Holder<Biome> biome, BlockPos pos) {
        return resolve(biome, pos).spice();
    }
}
