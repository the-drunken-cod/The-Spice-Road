package com.drunkencod.spice_road.compat.viewer.layout;

import com.drunkencod.spice_road.compat.viewer.ViewerDrawing;
import com.drunkencod.spice_road.compat.viewer.ViewerLayout;

/**
 * The pixel layout of a Spice Item's or Mix Preset's profile, shared by the
 * Spice Profile viewer category and the Flavor Folio's {@code spice_profile}
 * and {@code mix_preset} pages. Coordinates are relative to the entry's
 * top-left corner.
 */
public final class SpiceProfileLayout {

    // #region Size

    /** Width of the viewer category. */
    public static final int WIDTH = 160;
    /** Height of the viewer category, sized for {@link #HEADER_MAX_ROWS}. */
    public static final int HEIGHT = 135;

    // #region Header

    /** Most header rows a preset's slots take. */
    public static final int HEADER_MAX_ROWS = 2;
    /**
     * Width at the end of a preset's header row that its slots leave free for
     * the jar the preset makes, plus a gap.
     */
    public static final int HEADER_RESERVED_WIDTH = 2 * ViewerLayout.SLOT_SIZE - 4;
    /** Left edge of a Spice Item's name, after its slot. */
    public static final int ITEM_NAME_X = ViewerLayout.SLOT_SIZE + 4;
    /** Top edge of a Spice Item's name, centered on its slot. */
    public static final int ITEM_NAME_Y = 5;

    // #region Flavor panel

    /** Left edge of the panel's content, and the gap to its right edge. */
    public static final int PANEL_X = ViewerDrawing.PANEL_BORDER;
    /** Gap between the header and the panel's border. */
    public static final int PANEL_GAP = 2;
    /** Score magnitude at which a flavor bar is full, as on item tooltips. */
    public static final double BAR_SCALE = 1D;

    private SpiceProfileLayout() {
        // noop
    }

    /**
     * @param width Width the entry is laid out for.
     * @return How many preset slots fit in one header row, leaving room for the
     *         jar the preset makes.
     */
    public static int slotsPerRow(int width) {
        return Math.max(1, (width - HEADER_RESERVED_WIDTH) / ViewerLayout.SLOT_SIZE);
    }

    /**
     * @param headerHeight Height of the header rows above the panel.
     * @return The top edge of the panel's content.
     */
    public static int panelY(int headerHeight) {
        return headerHeight + PANEL_GAP + ViewerDrawing.PANEL_BORDER;
    }
}
