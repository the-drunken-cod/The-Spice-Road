package com.drunkencod.spice_road.spice.region;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.WeakHashMap;
import java.util.function.Function;
import java.util.function.Predicate;

import org.jetbrains.annotations.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.QuartPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.RandomState;

import com.drunkencod.spice_road.Constants;
import com.drunkencod.spice_road.block.SpicePlants;
import com.drunkencod.spice_road.block.SpiceTrees;
import com.drunkencod.spice_road.block.SpiceVines;
import com.drunkencod.spice_road.platform.Services;
import com.drunkencod.spice_road.spice.Climate;
import com.drunkencod.spice_road.spice.Spice;

/**
 * Finds {@link RegionHeart}s by walking the Spice Region grid outward from a
 * position, ring by ring. Works in chunks that haven't been generated yet,
 * since surface height and biome are estimated straight from the chunk
 * generator.
 * <p>
 * Only the Overworld has Spice Regions. Hearts in biomes tagged
 * {@link #NO_REGION_HEART} are barren and never returned, unless an Aquatic
 * Heart Spice lifts that for {@link #AQUATIC_REGION_HEART} biomes, and neither are
 * hearts whose Heart Spice has no worldgen plant, since no Heart Grove could
 * generate there.
 * <p>
 * This class also picks each heart's Heart Grove site (see
 * {@link #groveSite}), which is what makes the grove's position independent of
 * the chunk being decorated: every chunk resolves the same site for a given
 * heart, so the grove generates exactly once, in whichever chunk that site
 * falls into.
 */
public final class RegionHeartSearch {

    /** Biomes whose Region Hearts are barren, i.e. never located, mapped or otherwise targeted. */
    public static final TagKey<Biome> NO_REGION_HEART = TagKey.create(Registries.BIOME,
            ResourceLocation.fromNamespaceAndPath(Constants.MOD_ID, "no_region_heart"));

    /**
     * {@link #NO_REGION_HEART} biomes whose Region Hearts, and Heart Grove
     * sites, are allowed anyway if the Heart Spice is an Aquatic Spice.
     */
    public static final TagKey<Biome> AQUATIC_REGION_HEART = TagKey.create(Registries.BIOME,
            ResourceLocation.fromNamespaceAndPath(Constants.MOD_ID, "aquatic_region_heart"));

    /** Maximum distance, in blocks, a Heart Grove site may sit from its heart. */
    public static final int GROVE_SEARCH_RADIUS = 48;

    /** Spacing of the columns the grove site search samples, in blocks. */
    private static final int GROVE_SEARCH_STEP = 8;

    /** Offsets sampled around a heart when looking for a grove site, nearest first. */
    private static final int[][] GROVE_SEARCH_OFFSETS = groveSearchOffsets();

    /** Maximum number of cached heart samples per chunk generator. */
    private static final int SAMPLE_CACHE_SIZE = 16_384;

    /** Maximum number of cached grove sites per chunk generator. */
    private static final int GROVE_SITE_CACHE_SIZE = 1024;

    /**
     * Surface height and biome per heart position, per chunk generator. Both
     * are pure functions of the generator and the position, and by far the
     * most expensive part of a search.
     */
    private static final Map<ChunkGenerator, Map<Long, HeartSample>> SAMPLE_CACHE = new WeakHashMap<>();

    /**
     * Grove site per heart position, per chunk generator. Resolved by many
     * chunks around a heart, and a miss can cost a full
     * {@link #GROVE_SEARCH_OFFSETS} scan.
     */
    private static final Map<ChunkGenerator, Map<Long, Optional<BlockPos>>> GROVE_SITE_CACHE = new WeakHashMap<>();

    private RegionHeartSearch() {
    }

    /**
     * Resolves the heart of the Spice Region cell at the given grid
     * coordinates.
     *
     * @param level  The level to resolve in.
     * @param gridX  Grid-space X coordinate of the cell.
     * @param gridZ  Grid-space Z coordinate of the cell.
     * @param origin The position {@link SpiceCell#distance()} is measured from.
     * @return The heart, or empty if it's barren, has no Heart Spice, or
     *         {@code level} has no Spice Regions.
     */
    public static Optional<RegionHeart> heartAt(ServerLevel level, int gridX, int gridZ, BlockPos origin) {
        if (!hasSpiceRegions(level))
            return Optional.empty();

        SpiceCell cell = SpiceRegionResolver.cellAtGrid(level.getSeed(), Services.CONFIG.getSpiceRegionSalt(),
                Services.CONFIG.getSpiceRegionCellScale(), gridX, gridZ, origin.getX(), origin.getZ());
        return heartOf(level, cell);
    }

    /**
     * Resolves the heart of the Spice Region cell {@code pos} falls into.
     *
     * @param level The level to resolve in.
     * @param pos   Any position within the cell.
     * @return The heart, or empty if it's barren, has no Heart Spice, or
     *         {@code level} has no Spice Regions.
     */
    public static Optional<RegionHeart> heartAt(ServerLevel level, BlockPos pos) {
        if (!hasSpiceRegions(level))
            return Optional.empty();

        SpiceCell cell = SpiceRegionResolver.resolveCell(level.getSeed(), Services.CONFIG.getSpiceRegionSalt(),
                Services.CONFIG.getSpiceRegionCellScale(), pos.getX(), pos.getZ());
        return heartOf(level, cell);
    }

    /**
     * Resolves every Region Heart whose {@link #groveSite} lies within
     * {@code chunk}, i.e. every heart whose Heart Grove belongs to this chunk.
     * Cheap for chunks without one, which is almost all of them.
     *
     * @param level The level to resolve in.
     * @param chunk The chunk to check.
     * @return The hearts whose grove belongs in {@code chunk}; usually none.
     */
    public static List<RegionHeart> heartsWithGroveIn(ServerLevel level, ChunkPos chunk) {
        List<RegionHeart> hearts = new ArrayList<>();
        if (!hasSpiceRegions(level))
            return hearts;

        long worldSeed = level.getSeed();
        long salt = Services.CONFIG.getSpiceRegionSalt();
        double cellScale = Services.CONFIG.getSpiceRegionCellScale();
        int minX = chunk.getMinBlockX();
        int minZ = chunk.getMinBlockZ();
        int maxX = chunk.getMaxBlockX();
        int maxZ = chunk.getMaxBlockZ();

        // A site never sits farther than the search radius from its heart, so
        // only cells whose feature point is within that reach of the chunk can
        // own a grove here - and a feature point never leaves its own square.
        int minGridX = (int) Math.floor((minX - GROVE_SEARCH_RADIUS) / cellScale);
        int maxGridX = (int) Math.floor((maxX + GROVE_SEARCH_RADIUS) / cellScale);
        int minGridZ = (int) Math.floor((minZ - GROVE_SEARCH_RADIUS) / cellScale);
        int maxGridZ = (int) Math.floor((maxZ + GROVE_SEARCH_RADIUS) / cellScale);

        for (int gridX = minGridX; gridX <= maxGridX; gridX++) {
            for (int gridZ = minGridZ; gridZ <= maxGridZ; gridZ++) {
                SpiceCell cell = SpiceRegionResolver.cellAtGrid(worldSeed, salt, cellScale, gridX, gridZ, minX, minZ);
                Optional<RegionHeart> heart = heartOf(level, cell);
                if (heart.isEmpty())
                    continue;

                BlockPos site = groveSite(level, heart.get()).orElse(null);
                if (site == null)
                    continue;
                if (site.getX() >= minX && site.getX() <= maxX && site.getZ() >= minZ && site.getZ() <= maxZ)
                    hearts.add(heart.get());
            }
        }
        return hearts;
    }

    /**
     * Picks where {@code heart}'s Heart Grove grows: the nearest column within
     * {@link #GROVE_SEARCH_RADIUS} that is dry, not in a
     * {@link #NO_REGION_HEART} biome (see {@link #AQUATIC_REGION_HEART} for
     * the exception), and still in the heart's own Spice
     * Region. Based purely on the terrain estimate, so the answer is the same
     * from every chunk and available before any of them generate; the grove's
     * placement refines it against real blocks.
     *
     * @param level The level to resolve in.
     * @param heart The heart whose grove site to pick.
     * @return The site, at the estimated surface height, or empty if the heart
     *         has no ground within reach - which makes it barren, since a heart
     *         that can't host a grove would lead the player nowhere.
     */
    public static Optional<BlockPos> groveSite(ServerLevel level, RegionHeart heart) {
        ChunkGenerator generator = level.getChunkSource().getGenerator();
        long key = ChunkPos.asLong(heart.pos().getX(), heart.pos().getZ());
        synchronized (GROVE_SITE_CACHE) {
            Optional<BlockPos> cached = GROVE_SITE_CACHE
                    .computeIfAbsent(generator, g -> newLruMap(GROVE_SITE_CACHE_SIZE)).get(key);
            if (cached != null)
                return cached;
        }

        Optional<BlockPos> site = searchGroveSite(level, heart);
        synchronized (GROVE_SITE_CACHE) {
            GROVE_SITE_CACHE.computeIfAbsent(generator, g -> newLruMap(GROVE_SITE_CACHE_SIZE)).put(key, site);
        }
        return site;
    }

    /**
     * Converts a search radius in cells into blocks, using the configured
     * cell scale. Radii are configured in cells, so changing the region size
     * doesn't change how many hearts are within reach.
     *
     * @param cells The radius, in cells.
     * @return The radius, in blocks.
     */
    public static int cellsToBlocks(int cells) {
        return (int) Math.min(Integer.MAX_VALUE, Math.round(cells * Services.CONFIG.getSpiceRegionCellScale()));
    }

    /**
     * Finds the nearest Region Heart matching {@code matcher}.
     *
     * @param level   The level to search in.
     * @param origin  The position to search from.
     * @param radius  Maximum horizontal distance, in blocks, from
     *                {@code origin} to the heart.
     * @param matcher Which hearts count.
     * @return The nearest matching heart, or empty if there is none within
     *         {@code radius}.
     */
    public static Optional<RegionHeart> findNearest(ServerLevel level, BlockPos origin, int radius,
            Predicate<RegionHeart> matcher) {
        return Optional.ofNullable(
                findNearestPerKey(level, origin, radius, heart -> matcher.test(heart) ? Boolean.TRUE : null, 1)
                        .get(Boolean.TRUE));
    }

    /**
     * Finds the nearest Region Heart whose Heart Spice is {@code spice}.
     *
     * @param level  The level to search in.
     * @param origin The position to search from.
     * @param radius Maximum horizontal distance, in blocks.
     * @param spice  The Heart Spice to look for.
     * @return The nearest heart of {@code spice}, or empty if there is none
     *         within {@code radius}.
     */
    public static Optional<RegionHeart> findNearest(ServerLevel level, BlockPos origin, int radius, Spice spice) {
        return findNearest(level, origin, radius, heart -> heart.spice() == spice);
    }

    /**
     * Finds the nearest Region Heart of each of the given Spices in a single
     * walk, which is much cheaper than searching for each one separately.
     *
     * @param level  The level to search in.
     * @param origin The position to search from.
     * @param radius Maximum horizontal distance, in blocks.
     * @param spices The Heart Spices to look for.
     * @return The nearest heart per Spice. Spices without a heart within
     *         {@code radius} are absent.
     */
    public static Map<Spice, RegionHeart> findNearestOfEach(ServerLevel level, BlockPos origin, int radius,
            Collection<Spice> spices) {
        Set<Spice> wanted = Set.copyOf(spices);
        return findNearestPerKey(level, origin, radius,
                heart -> wanted.contains(heart.spice()) ? heart.spice() : null, wanted.size());
    }

    /**
     * @param level The level to check.
     * @return Whether {@code level} has Spice Regions at all.
     */
    public static boolean hasSpiceRegions(ServerLevel level) {
        return level.dimension() == Level.OVERWORLD;
    }

    /**
     * Walks the grid ring by ring, keeping the nearest heart per key. Stops
     * once every wanted key is found and no unvisited cell can be nearer, or
     * once {@code radius} is exceeded.
     * <p>
     * {@code origin} is projected out of a Sable sub-level first (see
     * {@link SublevelPositions}), so searching from a moving contraption
     * walks the grid from its real location, not its stored coordinates.
     *
     * @param keyOf      Key a heart counts towards, or {@code null} if it
     *                   doesn't count.
     * @param wantedKeys Number of distinct keys that can be found at most.
     */
    private static <K> Map<K, RegionHeart> findNearestPerKey(ServerLevel level, BlockPos origin, int radius,
            Function<RegionHeart, @Nullable K> keyOf, int wantedKeys) {
        Map<K, RegionHeart> nearest = new HashMap<>();
        if (!hasSpiceRegions(level) || wantedKeys <= 0)
            return nearest;

        BlockPos effectiveOrigin = SublevelPositions.projectOutOfSubLevel(level, origin);
        long worldSeed = level.getSeed();
        long salt = Services.CONFIG.getSpiceRegionSalt();
        double cellScale = Services.CONFIG.getSpiceRegionCellScale();
        int originX = effectiveOrigin.getX();
        int originZ = effectiveOrigin.getZ();
        int originGridX = (int) Math.floor(originX / cellScale);
        int originGridZ = (int) Math.floor(originZ / cellScale);
        int maxRing = (int) Math.ceil(radius / cellScale) + 1;

        for (int ring = 0; ring <= maxRing; ring++) {
            // Feature points never leave their own square, so every cell of
            // this ring is at least (ring - 1) full squares away.
            double ringMinDistance = (ring - 1) * cellScale;
            if (ringMinDistance > radius)
                break;
            if (nearest.size() >= wantedKeys && ringMinDistance > farthestOf(nearest))
                break;

            for (int[] offset : ringOffsets(ring)) {
                SpiceCell cell = SpiceRegionResolver.cellAtGrid(worldSeed, salt, cellScale,
                        originGridX + offset[0], originGridZ + offset[1], originX, originZ);
                if (cell.distance() > radius)
                    continue;
                if (nearest.size() >= wantedKeys && cell.distance() >= farthestOf(nearest))
                    continue;

                Optional<RegionHeart> heart = heartOf(level, cell);
                if (heart.isEmpty())
                    continue;

                K key = keyOf.apply(heart.get());
                if (key == null || groveSite(level, heart.get()).isEmpty())
                    continue;

                RegionHeart previous = nearest.get(key);
                if (previous == null || cell.distance() < previous.cell().distance())
                    nearest.put(key, heart.get());
            }
        }
        return nearest;
    }

    /** Resolves the heart of {@code cell}, see {@link #heartAt}. */
    private static Optional<RegionHeart> heartOf(ServerLevel level, SpiceCell cell) {
        int x = (int) Math.floor(cell.centerX());
        int z = (int) Math.floor(cell.centerZ());
        HeartSample sample = sample(level, x, z);
        boolean heartless = sample.biome().is(NO_REGION_HEART);
        if (heartless && !sample.biome().is(AQUATIC_REGION_HEART))
            return Optional.empty();

        BlockPos pos = new BlockPos(x, sample.surfaceY(), z);
        Climate climate = Climate.fromBiome(sample.biome(), pos);
        return SpiceRegionResolver.resolveSpice(cell, climate, Services.CONFIG.getSpiceRegionClusteringStrength())
                .filter(RegionHeartSearch::hasWorldgenPlant)
                .filter(spice -> !heartless || spice.isAquatic())
                .map(spice -> new RegionHeart(cell, pos, climate, spice));
    }

    /**
     * @return Whether {@code biome} may host a Region Heart or Heart Grove
     *         site of {@code spice} - see {@link #NO_REGION_HEART} and
     *         {@link #AQUATIC_REGION_HEART}.
     */
    private static boolean allowsHeartOf(Holder<Biome> biome, Spice spice) {
        return !biome.is(NO_REGION_HEART) || (spice.isAquatic() && biome.is(AQUATIC_REGION_HEART));
    }

    /**
     * @return Whether {@code spice} has a plant, tree or Host Tree worldgen can
     *         place. Spices without one would yield a Heart that leads nowhere.
     */
    private static boolean hasWorldgenPlant(Spice spice) {
        return SpicePlants.getRegistered().containsKey(spice) || SpiceTrees.getRegistered().containsKey(spice)
                || SpiceVines.getRegistered().containsKey(spice);
    }

    /** Scans {@link #GROVE_SEARCH_OFFSETS} around {@code heart}, see {@link #groveSite}. */
    private static Optional<BlockPos> searchGroveSite(ServerLevel level, RegionHeart heart) {
        long worldSeed = level.getSeed();
        long salt = Services.CONFIG.getSpiceRegionSalt();
        double cellScale = Services.CONFIG.getSpiceRegionCellScale();

        for (int[] offset : GROVE_SEARCH_OFFSETS) {
            int x = heart.pos().getX() + offset[0];
            int z = heart.pos().getZ() + offset[1];
            HeartSample sample = sample(level, x, z);
            if (!sample.isDry() || !allowsHeartOf(sample.biome(), heart.spice()))
                continue;

            SpiceCell cell = SpiceRegionResolver.resolveCell(worldSeed, salt, cellScale, x, z);
            if (cell.gridX() != heart.cell().gridX() || cell.gridZ() != heart.cell().gridZ())
                continue;

            return Optional.of(new BlockPos(x, sample.surfaceY(), z));
        }
        return Optional.empty();
    }

    /**
     * @return Offsets on a {@link #GROVE_SEARCH_STEP} grid within
     *         {@link #GROVE_SEARCH_RADIUS}, nearest first, so the heart itself
     *         is tried before anything else and a heart on dry land costs a
     *         single sample.
     */
    private static int[][] groveSearchOffsets() {
        List<int[]> offsets = new ArrayList<>();
        for (int dx = -GROVE_SEARCH_RADIUS; dx <= GROVE_SEARCH_RADIUS; dx += GROVE_SEARCH_STEP) {
            for (int dz = -GROVE_SEARCH_RADIUS; dz <= GROVE_SEARCH_RADIUS; dz += GROVE_SEARCH_STEP) {
                if ((dx * dx) + (dz * dz) <= GROVE_SEARCH_RADIUS * GROVE_SEARCH_RADIUS)
                    offsets.add(new int[] { dx, dz });
            }
        }
        offsets.sort(Comparator.comparingInt(offset -> (offset[0] * offset[0]) + (offset[1] * offset[1])));
        return offsets.toArray(int[][]::new);
    }

    /** Estimates surface height, floor height and biome at {@code (x, z)} without generating chunks. Cached. */
    private static HeartSample sample(ServerLevel level, int x, int z) {
        ChunkGenerator generator = level.getChunkSource().getGenerator();
        long key = ChunkPos.asLong(x, z);
        synchronized (SAMPLE_CACHE) {
            HeartSample cached = SAMPLE_CACHE.computeIfAbsent(generator, g -> newLruMap(SAMPLE_CACHE_SIZE))
                    .get(key);
            if (cached != null)
                return cached;
        }

        RandomState randomState = level.getChunkSource().randomState();
        int y = generator.getBaseHeight(x, z, Heightmap.Types.WORLD_SURFACE_WG, level, randomState);
        int floorY = generator.getBaseHeight(x, z, Heightmap.Types.OCEAN_FLOOR_WG, level, randomState);
        Holder<Biome> biome = generator.getBiomeSource().getNoiseBiome(QuartPos.fromBlock(x), QuartPos.fromBlock(y),
                QuartPos.fromBlock(z), randomState.sampler());
        HeartSample sample = new HeartSample(y, floorY, biome);

        synchronized (SAMPLE_CACHE) {
            SAMPLE_CACHE.computeIfAbsent(generator, g -> newLruMap(SAMPLE_CACHE_SIZE)).put(key, sample);
        }
        return sample;
    }

    /** @return A position-keyed map that evicts its least recently used entry past {@code maxSize}. */
    private static <V> Map<Long, V> newLruMap(int maxSize) {
        return new LinkedHashMap<>(256, 0.75F, true) {
            @Override
            protected boolean removeEldestEntry(Map.Entry<Long, V> eldest) {
                return size() > maxSize;
            }
        };
    }

    /** @return Grid offsets of every cell on the square ring at Chebyshev distance {@code ring}. */
    private static int[][] ringOffsets(int ring) {
        if (ring == 0)
            return new int[][] { { 0, 0 } };

        int[][] offsets = new int[ring * 8][];
        int i = 0;
        for (int d = -ring; d <= ring; d++) {
            offsets[i++] = new int[] { d, -ring };
            offsets[i++] = new int[] { d, ring };
        }
        for (int d = -ring + 1; d <= ring - 1; d++) {
            offsets[i++] = new int[] { -ring, d };
            offsets[i++] = new int[] { ring, d };
        }
        return offsets;
    }

    private static double farthestOf(Map<?, RegionHeart> hearts) {
        double farthest = 0;
        for (RegionHeart heart : hearts.values())
            farthest = Math.max(farthest, heart.cell().distance());
        return farthest;
    }

    /** Surface height (including fluids), floor height (excluding fluids) and biome at a position. */
    private record HeartSample(int surfaceY, int floorY, Holder<Biome> biome) {

        /** @return Whether no fluid sits on top of the ground here. */
        boolean isDry() {
            return floorY >= surfaceY;
        }
    }
}
