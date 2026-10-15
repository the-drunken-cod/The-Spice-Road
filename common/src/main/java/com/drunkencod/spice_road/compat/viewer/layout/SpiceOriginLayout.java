package com.drunkencod.spice_road.compat.viewer.layout;

import com.drunkencod.spice_road.compat.viewer.ViewerDrawing;
import com.drunkencod.spice_road.compat.viewer.ViewerLayout;

/**
 * The pixel layout of a Spice's origin, shared by the Spice Origin viewer
 * category and the Flavor Folio's {@code spice_info} page. Coordinates are
 * relative to the entry's top-left corner.
 */
public final class SpiceOriginLayout {

    // #region Size

    /** Width of the viewer category. */
    public static final int WIDTH = 160;
    /** Height of the viewer category. */
    public static final int HEIGHT = 97;

    // #region Header row

    /** Top edge of the slots and the arrow in the header row. */
    public static final int HEADER_Y = 0;
    /** Left edge of the planting item's slot. */
    public static final int PLANTING_X = 0;
    /** Left edge of the arrow sprite. */
    public static final int ARROW_X = 22;
    /** Top edge of the arrow sprite. */
    public static final int ARROW_Y = HEADER_Y + 1;
    /** Width of the arrow sprite. */
    public static final int ARROW_WIDTH = 24;
    /** Height of the arrow sprite. */
    public static final int ARROW_HEIGHT = 16;
    /** Left edge of the raw item's slot. */
    public static final int RAW_X = 50;
    /**
     * Gap between the harvest tool's slot and the right edge of the entry; the
     * slot is right-aligned.
     */
    public static final int TOOL_RIGHT_INSET = 0;

    // #region Info panel

    /** Left edge of the panel's content, and the gap to its right edge. */
    public static final int PANEL_X = ViewerDrawing.PANEL_BORDER;
    /** Gap between the header row and the panel's border. */
    public static final int PANEL_GAP = 2;
    /** Top edge of the panel's content. */
    public static final int PANEL_Y = HEADER_Y + ViewerLayout.SLOT_SIZE + PANEL_GAP + ViewerDrawing.PANEL_BORDER;

    private SpiceOriginLayout() {
        // noop
    }
}
