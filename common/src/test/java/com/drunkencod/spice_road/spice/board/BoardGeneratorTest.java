package com.drunkencod.spice_road.spice.board;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import java.util.EnumMap;
import java.util.Map;

import org.junit.jupiter.api.Test;

import com.drunkencod.spice_road.spice.FlavorAxis;
import com.drunkencod.spice_road.spice.board.EffectSlot.SlotKind;

class BoardGeneratorTest {

    private static final BoardLayout LAYOUT = BoardLayout.DEFAULT;

    @Test
    void sameSeedGivesTheSameBoardAndDifferentSeedsDiffer() {
        assertEquals(BoardGenerator.generate(42L, LAYOUT), BoardGenerator.generate(42L, LAYOUT));
        assertNotEquals(BoardGenerator.generate(42L, LAYOUT), BoardGenerator.generate(43L, LAYOUT));
    }

    @Test
    void everyAxisZoneGetsExactlyTheLayoutQuotas() {
        for (long seed = 0; seed < 200; seed++) {
            SeasoningBoard board = BoardGenerator.generate(seed, LAYOUT);
            Map<FlavorAxis, int[]> counts = new EnumMap<>(FlavorAxis.class);
            for (Cell cell : board.cells()) {
                if (cell.zone() == null)
                    continue;
                counts.computeIfAbsent(cell.zone(), a -> new int[CellKind.values().length])[cell.kind().ordinal()]++;
            }
            for (FlavorAxis axis : FlavorAxis.values()) {
                int[] c = counts.get(axis);
                assertEquals(4, c[CellKind.EFFECT.ordinal()], "effects " + axis + " seed " + seed);
                assertEquals(1, c[CellKind.MINE.ordinal()], "mines " + axis + " seed " + seed);
                assertEquals(2, c[CellKind.WALL.ordinal()], "walls " + axis + " seed " + seed);
                assertEquals(2, c[CellKind.BARE.ordinal()], "bare " + axis + " seed " + seed);
            }
        }
    }

    @Test
    void effectCellsFollowTheRingQuotasAndBundleRecipes() {
        for (long seed = 0; seed < 100; seed++) {
            SeasoningBoard board = BoardGenerator.generate(seed, LAYOUT);
            for (FlavorAxis axis : FlavorAxis.values()) {
                int[] perRing = new int[BoardGeometry.MAX_RING + 1];
                for (Cell cell : board.cells()) {
                    if (cell.zone() != axis || cell.kind() != CellKind.EFFECT)
                        continue;
                    perRing[cell.ring()]++;
                    long boons = cell.slots().stream().filter(s -> s.kind() == SlotKind.BOON).count();
                    long banes = cell.slots().stream().filter(s -> s.kind() == SlotKind.BANE).count();
                    long random = cell.slots().stream().filter(s -> s.kind() == SlotKind.RANDOM).count();
                    switch (cell.ring()) {
                        case 2 -> {
                            assertEquals(1, boons);
                            assertEquals(1, banes);
                            assertEquals(0, random);
                            cell.slots().forEach(s -> assertEquals(1, s.level()));
                        }
                        case 3 -> {
                            assertTrue(boons >= 1 && boons <= 2);
                            assertEquals(0, banes);
                            assertEquals(1, random);
                            cell.slots().forEach(s -> assertEquals(2, s.level()));
                        }
                        default -> {
                            assertTrue(boons >= 1 && boons <= 2);
                            assertEquals(0, banes + random);
                            cell.slots().forEach(s -> assertEquals(2, s.level()));
                        }
                    }
                }
                assertEquals(1, perRing[2]);
                assertEquals(1, perRing[3]);
                assertEquals(2, perRing[4]);
            }
        }
    }

    @Test
    void neutralZoneHasOnlyWallsAndBareCellsAndTheCentreIsFree() {
        for (long seed = 0; seed < 200; seed++) {
            SeasoningBoard board = BoardGenerator.generate(seed, LAYOUT);
            int walls = 0;
            for (Cell cell : board.cells()) {
                if (cell.zone() != null)
                    continue;
                assertTrue(cell.kind() == CellKind.BARE || cell.kind() == CellKind.WALL);
                if (cell.kind() == CellKind.WALL)
                    walls++;
            }
            assertEquals(CellKind.BARE, board.cell(BoardGeometry.CENTER, BoardGeometry.CENTER).kind());
            assertTrue(walls <= 2, "seed " + seed);
        }
    }

    @Test
    void minesLeanTowardsTheOuterRings() {
        int[] perRing = new int[BoardGeometry.MAX_RING + 1];
        for (long seed = 0; seed < 500; seed++) {
            for (Cell cell : BoardGenerator.generate(seed, LAYOUT).cells()) {
                if (cell.kind() == CellKind.MINE) {
                    perRing[cell.ring()]++;
                    assertEquals(1, cell.slots().size());
                    assertEquals(SlotKind.BANE, cell.slots().get(0).kind());
                }
            }
        }
        assertTrue(perRing[4] > perRing[3] && perRing[3] > perRing[2], Arrays.toString(perRing));
    }

    @Test
    void everyGeneratedBoardIsFullyReachable() {
        for (long seed = 0; seed < 2000; seed++)
            assertTrue(BoardGenerator.isFullyReachable(BoardGenerator.generate(seed, LAYOUT)), "seed " + seed);
    }

    @Test
    void aLayoutThatCanNeverBeReachableFallsBackToNoWalls() {
        // Walls on all 8 cells around the centre always seal it off.
        BoardLayout sealed = new BoardLayout(8, LAYOUT.effectCells(), 0, LAYOUT.mineWeights(), 0, LAYOUT.bundles());
        SeasoningBoard board = BoardGenerator.generate(1L, sealed);
        assertTrue(BoardGenerator.isFullyReachable(board));
        for (Cell cell : board.cells())
            assertNotEquals(CellKind.WALL, cell.kind());
    }
}
