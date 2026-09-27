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
 * non-barren Region Heart within the chunk being decorated, and nowhere in
 * chunks without one. Used to place Heart Groves.
 * <p>
 * The position is the nearest dry, {@link SpicePlantBlock#SPICE_GROWABLE}
 * surface column within {@code max_shift} blocks of the heart that still lies
 * in the heart's own Spice Region, so lakes and other obstacles right at the
 * heart don't prevent its grove. A heart without such a column gets no grove.
 */
public final class RegionHeartPlacement extends PlacementModifier {

    /**
     * How far past the decorated chunk's edges the position may land, in
     * blocks. Keeps the feature's own spread (and tree canopies) within the
     * neighboring chunks worldgen is allowed to write into.
     */
    private static final int CHUNK_MARGIN = 4;

    /** Codec of this modifier's JSON fields. */
    public static final MapCodec<RegionHeartPlacement> CODEC = Codec.intRange(0, 16)
            .optionalFieldOf("max_shift", 16)
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
        return RegionHeartSearch.heartsInChunk(level, chunk).stream()
                .map(heart -> findGroveOrigin(context, level, chunk, heart))
                .flatMap(Optional::stream);
    }

    /** @return The nearest suitable surface position around {@code heart}, if any. */
    private Optional<BlockPos> findGroveOrigin(PlacementContext context, ServerLevel level, ChunkPos chunk,
            RegionHeart heart) {
        int minX = chunk.getMinBlockX() - CHUNK_MARGIN;
        int maxX = chunk.getMaxBlockX() + CHUNK_MARGIN;
        int minZ = chunk.getMinBlockZ() - CHUNK_MARGIN;
        int maxZ = chunk.getMaxBlockZ() + CHUNK_MARGIN;

        for (int[] offset : offsetsByDistance) {
            int x = heart.pos().getX() + offset[0];
            int z = heart.pos().getZ() + offset[1];
            if (x < minX || x > maxX || z < minZ || z > maxZ)
                continue;

            BlockPos surface = new BlockPos(x, context.getHeight(Heightmap.Types.WORLD_SURFACE_WG, x, z), z);
            if (!context.getBlockState(surface.below()).is(SpicePlantBlock.SPICE_GROWABLE))
                continue;
            if (!isInRegionOf(level, heart, x, z))
                continue;

            return Optional.of(surface);
        }
        return Optional.empty();
    }

    /** @return Whether {@code (x, z)} lies in the Spice Region {@code heart} belongs to. */
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
