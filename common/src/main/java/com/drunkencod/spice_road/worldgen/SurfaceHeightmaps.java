package com.drunkencod.spice_road.worldgen;

import net.minecraft.server.level.WorldGenRegion;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.levelgen.Heightmap;

/**
 * Picks the heightmap types a feature reads, depending on where it runs. The
 * {@code _WG} types only exist while a chunk generates, so a feature placed
 * into a finished chunk (e.g. with {@code /place feature}) would find no
 * ground with them; it reads their live equivalents instead.
 */
public final class SurfaceHeightmaps {

    private SurfaceHeightmaps() {
    }

    /**
     * @param level The level the feature is placed in.
     * @return The topmost non-air block's heightmap, fluids included:
     *         {@link Heightmap.Types#WORLD_SURFACE_WG} during worldgen,
     *         {@link Heightmap.Types#WORLD_SURFACE} otherwise.
     */
    public static Heightmap.Types surface(LevelAccessor level) {
        return level instanceof WorldGenRegion ? Heightmap.Types.WORLD_SURFACE_WG : Heightmap.Types.WORLD_SURFACE;
    }

    /**
     * @param level The level the feature is placed in.
     * @return The topmost solid ground's heightmap, fluids excluded:
     *         {@link Heightmap.Types#OCEAN_FLOOR_WG} during worldgen,
     *         {@link Heightmap.Types#OCEAN_FLOOR} otherwise.
     */
    public static Heightmap.Types floor(LevelAccessor level) {
        return level instanceof WorldGenRegion ? Heightmap.Types.OCEAN_FLOOR_WG : Heightmap.Types.OCEAN_FLOOR;
    }
}
