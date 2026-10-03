package com.drunkencod.spice_road.spice.board;

import java.util.List;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.codecs.RecordCodecBuilder;

/**
 * How many cells of each kind a {@link SeasoningBoard} gets, and what its
 * effect bundles hold. Every axis zone gets the same quotas, so all eight are
 * equally rich by construction; the board seed only decides which cells get
 * them. Per-ring lists have one entry for each of the rings
 * {@value BoardGeometry#FIRST_AXIS_RING} to {@value BoardGeometry#MAX_RING}.
 * Loaded from {@code data/<namespace>/seasoning_board/default.json}.
 *
 * @param neutralWalls Walls in the neutral zone, among its eight non-centre cells.
 * @param effectCells  Effect cells per ring in each axis zone.
 * @param mines        Mines in each axis zone.
 * @param mineWeights  Relative likelihood of a mine landing in each ring.
 * @param walls        Walls in each axis zone.
 * @param bundles      What the effect cells (and the mines) of each ring hold.
 */
public record BoardLayout(
        int neutralWalls,
        List<Integer> effectCells,
        int mines,
        List<Integer> mineWeights,
        int walls,
        List<BundleSpec> bundles) {

    /** Number of rings that belong to axis zones. */
    public static final int AXIS_RINGS = BoardGeometry.MAX_RING - BoardGeometry.FIRST_AXIS_RING + 1;

    /** The layout the mod ships: 4 effect cells (1 / 1 / 2), 1 mine, 2 walls and 2 bare cells per axis zone. */
    public static final BoardLayout DEFAULT = new BoardLayout(
            2,
            List.of(1, 1, 2),
            1,
            List.of(1, 2, 4),
            2,
            List.of(
                    new BundleSpec(1, 1, 1, 0, 1),
                    new BundleSpec(1, 2, 0, 1, 2),
                    new BundleSpec(1, 2, 0, 0, 2)));

    /** Codec reading layout files. */
    public static final Codec<BoardLayout> CODEC = RecordCodecBuilder.<BoardLayout>create(instance -> instance.group(
            Codec.intRange(0, 8).fieldOf("neutral_walls").forGetter(BoardLayout::neutralWalls),
            Codec.intRange(0, 9).listOf().fieldOf("effect_cells").forGetter(BoardLayout::effectCells),
            Codec.intRange(0, 9).fieldOf("mines").forGetter(BoardLayout::mines),
            Codec.intRange(0, Integer.MAX_VALUE).listOf().fieldOf("mine_weights")
                    .forGetter(BoardLayout::mineWeights),
            Codec.intRange(0, 9).fieldOf("walls").forGetter(BoardLayout::walls),
            BundleSpec.CODEC.listOf().fieldOf("bundles").forGetter(BoardLayout::bundles))
            .apply(instance, BoardLayout::new))
            .validate(BoardLayout::validate);

    /**
     * @param neutralWalls Walls in the neutral zone.
     * @param effectCells  Effect cells per axis ring.
     * @param mines        Mines per axis zone.
     * @param mineWeights  Mine likelihood per axis ring.
     * @param walls        Walls per axis zone.
     * @param bundles      Bundle contents per axis ring.
     */
    public BoardLayout {
        effectCells = List.copyOf(effectCells);
        mineWeights = List.copyOf(mineWeights);
        bundles = List.copyOf(bundles);
    }

    private static DataResult<BoardLayout> validate(BoardLayout layout) {
        if (layout.effectCells.size() != AXIS_RINGS || layout.mineWeights.size() != AXIS_RINGS
                || layout.bundles.size() != AXIS_RINGS)
            return DataResult.error(() -> "effect_cells, mine_weights and bundles need exactly " + AXIS_RINGS
                    + " entries each, one per ring from " + BoardGeometry.FIRST_AXIS_RING);
        if (layout.mines > 0 && layout.mineWeights.stream().mapToInt(Integer::intValue).sum() == 0)
            return DataResult.error(() -> "mine_weights can't all be 0 while there are mines");
        return DataResult.success(layout);
    }

    /**
     * What the effect cells of one ring hold.
     *
     * @param minBoons Fewest boons in a bundle.
     * @param maxBoons Most boons in a bundle.
     * @param banes    Banes in a bundle.
     * @param randoms  Vanilla-random effects in a bundle.
     * @param level    Level of every effect of the ring, also of its mines.
     */
    public record BundleSpec(int minBoons, int maxBoons, int banes, int randoms, int level) {

        /** Codec reading one bundle. */
        public static final Codec<BundleSpec> CODEC = RecordCodecBuilder.<BundleSpec>create(instance -> instance
                .group(
                        Codec.intRange(0, 3).fieldOf("min_boons").forGetter(BundleSpec::minBoons),
                        Codec.intRange(0, 3).fieldOf("max_boons").forGetter(BundleSpec::maxBoons),
                        Codec.intRange(0, 3).fieldOf("banes").forGetter(BundleSpec::banes),
                        Codec.intRange(0, 3).fieldOf("random").forGetter(BundleSpec::randoms),
                        Codec.intRange(1, 10).fieldOf("level").forGetter(BundleSpec::level))
                .apply(instance, BundleSpec::new))
                .validate(spec -> spec.minBoons > spec.maxBoons
                        ? DataResult.error(() -> "min_boons can't be above max_boons")
                        : spec.maxBoons + spec.banes + spec.randoms == 0
                                ? DataResult.error(() -> "A bundle needs at least one effect")
                                : DataResult.success(spec));
    }
}
