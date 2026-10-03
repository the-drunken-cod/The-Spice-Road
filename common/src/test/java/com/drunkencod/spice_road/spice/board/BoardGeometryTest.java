package com.drunkencod.spice_road.spice.board;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.Test;

import com.drunkencod.spice_road.spice.FlavorAxis;

class BoardGeometryTest {

    @Test
    void zonesFollowFlavorAxisOrderInReadingOrderAroundTheNeutralCentre() {
        assertEquals(FlavorAxis.HEAT_COOLING, BoardGeometry.zoneAxis(0, 0));
        assertEquals(FlavorAxis.SWEET_BITTER, BoardGeometry.zoneAxis(4, 0));
        assertEquals(FlavorAxis.SOUR_MELLOW, BoardGeometry.zoneAxis(8, 0));
        assertEquals(FlavorAxis.EARTHY_FLORAL, BoardGeometry.zoneAxis(0, 4));
        assertNull(BoardGeometry.zoneAxis(4, 4));
        assertEquals(FlavorAxis.WOODY_GREEN, BoardGeometry.zoneAxis(8, 4));
        assertEquals(FlavorAxis.PUNGENT_SOFT, BoardGeometry.zoneAxis(0, 8));
        assertEquals(FlavorAxis.RESINOUS_CLEAN, BoardGeometry.zoneAxis(4, 8));
        assertEquals(FlavorAxis.SAVORY_DELICATE, BoardGeometry.zoneAxis(8, 8));
    }

    @Test
    void everyDirectionIsBoundToTheAxisOfTheZoneInThatDirection() {
        for (Direction direction : Direction.values()) {
            int x = BoardGeometry.CENTER + direction.dx() * 4;
            int y = BoardGeometry.CENTER + direction.dy() * 4;
            assertEquals(BoardGeometry.zoneAxis(x, y), direction.axis(), direction.name());
            assertEquals(direction, Direction.of(direction.axis()));
        }
    }

    @Test
    void oppositeDirectionsAreTheRetreatPairs() {
        assertEquals(Direction.DOWN_RIGHT, Direction.UP_LEFT.opposite());
        assertEquals(FlavorAxis.SAVORY_DELICATE, Direction.UP_LEFT.opposite().axis());
        assertEquals(Direction.DOWN, Direction.UP.opposite());
        assertEquals(Direction.DOWN_LEFT, Direction.UP_RIGHT.opposite());
        assertEquals(Direction.RIGHT, Direction.LEFT.opposite());
        for (Direction direction : Direction.values())
            assertEquals(direction, direction.opposite().opposite());
    }

    @Test
    void orthogonalZonesHaveThreeCellsPerOuterRingAndDiagonalOnesOneThreeFive() {
        Map<FlavorAxis, int[]> rings = new HashMap<>();
        for (int y = 0; y < BoardGeometry.SIZE; y++) {
            for (int x = 0; x < BoardGeometry.SIZE; x++) {
                FlavorAxis axis = BoardGeometry.zoneAxis(x, y);
                if (axis != null)
                    rings.computeIfAbsent(axis, a -> new int[BoardGeometry.MAX_RING + 1])[BoardGeometry.ring(x, y)]++;
            }
        }
        int[] orthogonal = { 0, 0, 3, 3, 3 };
        int[] diagonal = { 0, 0, 1, 3, 5 };
        for (FlavorAxis axis : new FlavorAxis[] { FlavorAxis.SWEET_BITTER, FlavorAxis.EARTHY_FLORAL,
                FlavorAxis.WOODY_GREEN, FlavorAxis.RESINOUS_CLEAN })
            assertArrayEquals(orthogonal, rings.get(axis), axis.name());
        for (FlavorAxis axis : new FlavorAxis[] { FlavorAxis.HEAT_COOLING, FlavorAxis.SOUR_MELLOW,
                FlavorAxis.PUNGENT_SOFT, FlavorAxis.SAVORY_DELICATE })
            assertArrayEquals(diagonal, rings.get(axis), axis.name());
    }
}
