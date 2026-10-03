package com.drunkencod.spice_road.spice.board;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Deque;
import java.util.List;
import java.util.SplittableRandom;

import com.drunkencod.spice_road.spice.FlavorAxis;
import com.drunkencod.spice_road.spice.board.EffectSlot.SlotKind;
import com.drunkencod.spice_road.spice.board.BoardLayout.BundleSpec;
import com.drunkencod.spice_road.spice.region.SeededHash;

/**
 * Builds a {@link SeasoningBoard} as a pure function of a seed and a
 * {@link BoardLayout}: each zone gets exactly the layout's quota of every cell
 * kind and the seed only picks which cells. A board that leaves a cell
 * unreachable from the centre is rebuilt from the next attempt's seed, and if
 * every attempt fails the board is built without walls, which can't fail.
 */
public final class BoardGenerator {

    /** Seeded attempts made before giving up on walls. */
    static final int MAX_ATTEMPTS = 64;

    private BoardGenerator() {
    }

    /**
     * @param seed   The board seed, see {@link BoardSeed}.
     * @param layout The quotas to build to.
     * @return The board for {@code seed}.
     */
    public static SeasoningBoard generate(long seed, BoardLayout layout) {
        for (int attempt = 0; attempt < MAX_ATTEMPTS; attempt++) {
            SeasoningBoard board = build(SeededHash.hash(seed, attempt, 0), layout, true);
            if (isFullyReachable(board))
                return board;
        }
        return build(SeededHash.hash(seed, MAX_ATTEMPTS, 0), layout, false);
    }

    /**
     * @param board A board.
     * @return Whether the pawn could get from the centre to every cell that
     *         isn't a wall, ignoring points. Only the target of a step blocks it.
     */
    public static boolean isFullyReachable(SeasoningBoard board) {
        int open = 0;
        for (Cell cell : board.cells()) {
            if (cell.kind() != CellKind.WALL)
                open++;
        }
        boolean[] seen = new boolean[board.cells().size()];
        Deque<int[]> queue = new ArrayDeque<>();
        queue.add(new int[] { BoardGeometry.CENTER, BoardGeometry.CENTER });
        seen[BoardGeometry.index(BoardGeometry.CENTER, BoardGeometry.CENTER)] = true;
        int reached = 0;
        while (!queue.isEmpty()) {
            int[] at = queue.poll();
            reached++;
            for (Direction direction : Direction.values()) {
                int x = at[0] + direction.dx();
                int y = at[1] + direction.dy();
                if (!BoardGeometry.inBounds(x, y) || seen[BoardGeometry.index(x, y)]
                        || board.cell(x, y).kind() == CellKind.WALL)
                    continue;
                seen[BoardGeometry.index(x, y)] = true;
                queue.add(new int[] { x, y });
            }
        }
        return reached == open;
    }

    private static SeasoningBoard build(long seed, BoardLayout layout, boolean allowWalls) {
        CellKind[] kinds = new CellKind[BoardGeometry.SIZE * BoardGeometry.SIZE];
        Arrays.fill(kinds, CellKind.BARE);
        List<List<EffectSlot>> slots = new ArrayList<>();
        for (int i = 0; i < kinds.length; i++)
            slots.add(List.of());

        if (allowWalls)
            placeNeutralWalls(kinds, new SplittableRandom(SeededHash.hash(seed, -1, 0)), layout);
        for (FlavorAxis axis : FlavorAxis.values())
            fillAxisZone(axis, kinds, slots, new SplittableRandom(SeededHash.hash(seed, axis.ordinal(), 1)), layout,
                    allowWalls);

        List<Cell> cells = new ArrayList<>(kinds.length);
        for (int y = 0; y < BoardGeometry.SIZE; y++) {
            for (int x = 0; x < BoardGeometry.SIZE; x++) {
                int index = BoardGeometry.index(x, y);
                cells.add(new Cell(kinds[index], BoardGeometry.ring(x, y), BoardGeometry.zoneAxis(x, y),
                        slots.get(index)));
            }
        }
        return new SeasoningBoard(cells);
    }

    private static void placeNeutralWalls(CellKind[] kinds, SplittableRandom random, BoardLayout layout) {
        List<Integer> candidates = new ArrayList<>();
        for (int y = 0; y < BoardGeometry.SIZE; y++) {
            for (int x = 0; x < BoardGeometry.SIZE; x++) {
                if (BoardGeometry.zoneAxis(x, y) == null && BoardGeometry.ring(x, y) > 0)
                    candidates.add(BoardGeometry.index(x, y));
            }
        }
        shuffle(candidates, random);
        for (int i = 0; i < Math.min(layout.neutralWalls(), candidates.size()); i++)
            kinds[candidates.get(i)] = CellKind.WALL;
    }

    private static void fillAxisZone(FlavorAxis axis, CellKind[] kinds, List<List<EffectSlot>> slots,
            SplittableRandom random, BoardLayout layout, boolean allowWalls) {
        List<List<Integer>> byRing = new ArrayList<>();
        for (int i = 0; i < BoardLayout.AXIS_RINGS; i++)
            byRing.add(new ArrayList<>());
        for (int y = 0; y < BoardGeometry.SIZE; y++) {
            for (int x = 0; x < BoardGeometry.SIZE; x++) {
                if (BoardGeometry.zoneAxis(x, y) == axis)
                    byRing.get(BoardGeometry.ring(x, y) - BoardGeometry.FIRST_AXIS_RING)
                            .add(BoardGeometry.index(x, y));
            }
        }

        for (int ring = 0; ring < BoardLayout.AXIS_RINGS; ring++) {
            List<Integer> available = byRing.get(ring);
            shuffle(available, random);
            BundleSpec bundle = layout.bundles().get(ring);
            for (int i = 0; i < Math.min(layout.effectCells().get(ring), available.size()); i++) {
                int index = available.remove(0);
                kinds[index] = CellKind.EFFECT;
                slots.set(index, bundleSlots(bundle, random));
            }
        }

        for (int i = 0; i < layout.mines(); i++) {
            int ring = pickWeightedRing(byRing, layout.mineWeights(), random);
            if (ring < 0)
                break;
            int index = byRing.get(ring).remove(random.nextInt(byRing.get(ring).size()));
            kinds[index] = CellKind.MINE;
            slots.set(index, List.of(new EffectSlot(SlotKind.BANE, layout.bundles().get(ring).level(),
                    random.nextLong())));
        }

        if (!allowWalls)
            return;
        List<Integer> rest = new ArrayList<>();
        byRing.forEach(rest::addAll);
        shuffle(rest, random);
        for (int i = 0; i < Math.min(layout.walls(), rest.size()); i++)
            kinds[rest.get(i)] = CellKind.WALL;
    }

    private static List<EffectSlot> bundleSlots(BundleSpec bundle, SplittableRandom random) {
        List<EffectSlot> slots = new ArrayList<>();
        int boons = bundle.minBoons() + random.nextInt(bundle.maxBoons() - bundle.minBoons() + 1);
        for (int i = 0; i < boons; i++)
            slots.add(new EffectSlot(SlotKind.BOON, bundle.level(), random.nextLong()));
        for (int i = 0; i < bundle.banes(); i++)
            slots.add(new EffectSlot(SlotKind.BANE, bundle.level(), random.nextLong()));
        for (int i = 0; i < bundle.randoms(); i++)
            slots.add(new EffectSlot(SlotKind.RANDOM, bundle.level(), random.nextLong()));
        return slots;
    }

    /** @return The index of a ring with free cells picked by weight, {@code -1} if none can take a mine. */
    private static int pickWeightedRing(List<List<Integer>> byRing, List<Integer> weights, SplittableRandom random) {
        long total = 0;
        for (int ring = 0; ring < byRing.size(); ring++) {
            if (!byRing.get(ring).isEmpty())
                total += weights.get(ring);
        }
        if (total <= 0)
            return -1;
        long roll = random.nextLong(total);
        for (int ring = 0; ring < byRing.size(); ring++) {
            if (byRing.get(ring).isEmpty())
                continue;
            roll -= weights.get(ring);
            if (roll < 0)
                return ring;
        }
        return -1;
    }

    private static void shuffle(List<Integer> list, SplittableRandom random) {
        for (int i = list.size() - 1; i > 0; i--) {
            int j = random.nextInt(i + 1);
            int swap = list.get(i);
            list.set(i, list.get(j));
            list.set(j, swap);
        }
    }
}
