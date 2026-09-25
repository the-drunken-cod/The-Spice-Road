package com.drunkencod.spice_road.spice.region;

/**
 * The Spice Region cell a given world position resolves to. Produced by a
 * jittered, Voronoi-style grid keyed off the world seed, so the same position
 * always resolves to the same cell for a given seed and cell scale.
 *
 * @param gridX    Grid-space X coordinate of the winning cell - not a world
 *                 coordinate; it identifies which square of the jittered
 *                 grid this cell is, independent of the cell scale used to
 *                 produce it.
 * @param gridZ    Grid-space Z coordinate of the winning cell.
 * @param seed     This cell's static, world-seed-dependent identity. Every
 *                 position resolving to the same cell shares this value; it
 *                 is the "one static seed" a Spice Region rolls its Spice
 *                 pick from.
 * @param centerX  World-space X of this cell's jittered feature point.
 * @param centerZ  World-space Z of this cell's jittered feature point.
 * @param distance Distance from the queried position to
 *                 ({@link #centerX}, {@link #centerZ}). Exposed for
 *                 debug/tuning purposes (e.g. the planned F3 renderer), not
 *                 consumed by the resolution algorithm itself.
 */
public record SpiceCell(int gridX, int gridZ, long seed, double centerX, double centerZ, double distance) {
}
