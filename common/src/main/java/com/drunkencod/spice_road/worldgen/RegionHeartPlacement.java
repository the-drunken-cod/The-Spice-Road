package com.drunkencod.spice_road.worldgen;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.placement.PlacementContext;
import net.minecraft.world.level.levelgen.placement.PlacementModifier;
import net.minecraft.world.level.levelgen.placement.PlacementModifierType;

import com.drunkencod.spice_road.block.SpicePlantBlock;
import com.drunkencod.spice_road.platform.Services;
import com.drunkencod.spice_road.spice.region.RegionHeart;
import com.drunkencod.spice_road.spice.region.RegionHeartSearch;
import com.drunkencod.spice_road.spice.region.SpiceCell;
import com.drunkencod.spice_road.spice.region.SpiceRegionResolver;

/**
 * Placement modifier {@code spice_road:region_heart}: places once per
 * non-barren Region Heart that owns a Heart Grove site in the chunk being
 * decorated, and nowhere else. Used to place Heart Groves.
 * <p>
 * The site itself is picked by
 * {@link RegionHeartSearch#groveSite(ServerLevel, RegionHeart)} from the
 * terrain estimate, so it can sit well away from the heart - in a neighboring
 * chunk if need be - without two chunks ever disagreeing about where the grove
 * goes. A heart with no site is barren and gets no grove.
 * <p>
 * This modifier then refines the site against real blocks, moving to the
 * nearest dry, {@link SpicePlantBlock#SPICE_GROWABLE} surface column within
 * {@code max_shift} blocks that still lies in the heart's own Spice Region, so
 * ponds and other obstacles the estimate can't see don't cost the grove. If
 * there is none, the site is used as-is and the feature gets to try anyway.
 */
public final class RegionHeartPlacement extends PlacementModifier {

    /**
     * How far past the decorated chunk's edges the position may land, in
     * blocks. Keeps the feature's own spread (and tree canopies) within the
     * neighboring chunks worldgen is allowed to write into.
     */
    private static final int CHUNK_MARGIN = 4;

    /**
     * Max possible radius of deviation when direct centered feature placement
     * attempts fail (for example due to other features like lakes being in the way)
     */
    private static final int MAX_SHIFT = 32;

    /** Codec of this modifier's JSON fields. */
    public static final MapCodec<RegionHeartPlacement> CODEC = Codec.intRange(0, MAX_SHIFT)
            .optionalFieldOf("max_shift", MAX_SHIFT)
            .xmap(RegionHeartPlacement::new, placement -> placement.maxShift);

    private final int maxShift;

    /** Column offsets within {@link #maxShift}, nearest first. */
    private final List<int[]> offsetsByDistance;

    private RegionHeartPlacement(int maxShift) {
        this.maxShift = maxShift;
        this.offsetsByDistance = new ArrayList<>();
        for (int dx = -maxShift; dx <= maxShift; dx++) {
            for (int dz = -maxShift; dz <= maxShift; dz++) {
                if ((dx * dx) + (dz * dz) <= maxShift * maxShift)
                    offsetsByDistance.add(new int[] { dx, dz });
            }
        }
        offsetsByDistance.sort(Comparator.comparingInt(offset -> (offset[0] * offset[0]) + (offset[1] * offset[1])));
    }

    @Override
    public Stream<BlockPos> getPositions(PlacementContext context, RandomSource random, BlockPos pos) {
        ServerLevel level = context.getLevel().getLevel();
        ChunkPos chunk = new ChunkPos(pos);
        return RegionHeartSearch.heartsWithGroveIn(level, chunk).stream()
                .map(heart -> RegionHeartSearch.groveSite(level, heart)
                        .map(site -> groveOrigin(context, level, chunk, heart, site)))
                .flatMap(Optional::stream);
    }

    /**
     * @return The nearest suitable surface position around {@code site}, or
     *         {@code site}'s own surface column if there is none.
     */
    private BlockPos groveOrigin(PlacementContext context, ServerLevel level, ChunkPos chunk, RegionHeart heart,
            BlockPos site) {
        int minX = chunk.getMinBlockX() - CHUNK_MARGIN;
        int maxX = chunk.getMaxBlockX() + CHUNK_MARGIN;
        int minZ = chunk.getMinBlockZ() - CHUNK_MARGIN;
        int maxZ = chunk.getMaxBlockZ() + CHUNK_MARGIN;

        for (int[] offset : offsetsByDistance) {
            int x = site.getX() + offset[0];
            int z = site.getZ() + offset[1];
            if (x < minX || x > maxX || z < minZ || z > maxZ)
                continue;

            BlockPos surface = surfaceOf(context, x, z);
            if (!context.getBlockState(surface.below()).is(SpicePlantBlock.SPICE_GROWABLE))
                continue;
            if (!isInRegionOf(level, heart, x, z))
                continue;

            return surface;
        }
        return surfaceOf(context, site.getX(), site.getZ());
    }

    /**
     * @return The surface position of the column at {@code (x, z)}, per the
     *         generating world's heightmap.
     */
    private static BlockPos surfaceOf(PlacementContext context, int x, int z) {
        return new BlockPos(x, context.getHeight(Heightmap.Types.WORLD_SURFACE_WG, x, z), z);
    }

    /**
     * @return Whether {@code (x, z)} lies in the Spice Region {@code heart} belongs
     *         to.
     */
    private static boolean isInRegionOf(ServerLevel level, RegionHeart heart, int x, int z) {
        SpiceCell cell = SpiceRegionResolver.resolveCell(level.getSeed(), Services.CONFIG.getSpiceRegionSalt(),
                Services.CONFIG.getSpiceRegionCellScale(), x, z);
        return cell.gridX() == heart.cell().gridX() && cell.gridZ() == heart.cell().gridZ();
    }

    @Override
    public PlacementModifierType<?> type() {
        return ModPlacementModifiers.REGION_HEART.get();
    }
}
