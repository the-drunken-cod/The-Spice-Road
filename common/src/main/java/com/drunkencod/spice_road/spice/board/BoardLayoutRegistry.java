package com.drunkencod.spice_road.spice.board;

/**
 * The {@link BoardLayout} boards are built to. Only the logical server needs
 * it, since clients are sent the board a session is played on.
 * {@link BoardLayoutReloadListener} fills it from the datapacks.
 */
public final class BoardLayoutRegistry {

    private static volatile BoardLayout layout = BoardLayout.DEFAULT;

    private BoardLayoutRegistry() {
    }

    /** @return The current layout. */
    public static BoardLayout get() {
        return layout;
    }

    /**
     * @param newLayout The layout to build boards to from now on.
     */
    static void set(BoardLayout newLayout) {
        layout = newLayout;
    }
}
