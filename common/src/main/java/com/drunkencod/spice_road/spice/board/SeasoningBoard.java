package com.drunkencod.spice_road.spice.board;

import java.util.List;

/**
 * The 9x9 grid of cells the Spice Grinder minigame is played on. Immutable:
 * every cell's kind, position and slots are fixed by {@link BoardGenerator}
 * from the world seed, the configured salt and the food item.
 *
 * @param cells Every cell in row-major order, {@code 81} of them.
 */
public record SeasoningBoard(List<Cell> cells) {

    /**
     * @param cells Every cell in row-major order, copied.
     */
    public SeasoningBoard {
        if (cells.size() != BoardGeometry.SIZE * BoardGeometry.SIZE)
            throw new IllegalArgumentException("A board has exactly 81 cells, got " + cells.size());
        cells = List.copyOf(cells);
    }

    /**
     * @param x A column on the board.
     * @param y A row on the board.
     * @return The cell at {@code (x, y)}.
     */
    public Cell cell(int x, int y) {
        return cells.get(BoardGeometry.index(x, y));
    }
}
