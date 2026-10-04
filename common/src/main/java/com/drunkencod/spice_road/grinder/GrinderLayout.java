package com.drunkencod.spice_road.grinder;

/**
 * The pixel layout of the Spice Grinder GUI at GUI scale 1, shared by the menu
 * (slot positions) and the screen. {@code dev/grinder_gui_mockup.html} draws
 * the same numbers.
 */
public final class GrinderLayout {

    /** Width of the GUI. */
    public static final int IMAGE_WIDTH = 280;
    /** Height of the GUI. */
    public static final int IMAGE_HEIGHT = 248;

    // #region Left panel (spice list)

    /** Left edge of the spice list panel. */
    public static final int LIST_X = 8;
    /** Top edge of the spice list panel. */
    public static final int LIST_Y = 16;
    /** Width of the spice list panel. */
    public static final int LIST_WIDTH = 100;
    /** Height of the spice list panel. */
    public static final int LIST_HEIGHT = 136;
    /** Height of one row of the spice list. */
    public static final int LIST_ROW_HEIGHT = 12;
    /** Rows visible at once. */
    public static final int LIST_ROWS = 11;

    // #region Right panel

    /** Left edge of the right panel. */
    public static final int PANEL_X = 112;
    /** Top edge of the right panel. */
    public static final int PANEL_Y = 16;
    /** Width of the right panel. */
    public static final int PANEL_WIDTH = 160;
    /** Height of the right panel. */
    public static final int PANEL_HEIGHT = 136;

    /** Left edge of the food slot's item, 1 px inside its frame. */
    public static final int FOOD_SLOT_X = 251;
    /** Top edge of the food slot's item. */
    public static final int FOOD_SLOT_Y = 19;

    // #region Draft view

    /** Left edge of the draft's flavor rows. */
    public static final int DRAFT_ROWS_X = 116;
    /** Top edge of the first of the draft's flavor rows. */
    public static final int DRAFT_ROWS_Y = 40;
    /** Height of one flavor row. */
    public static final int DRAFT_ROW_HEIGHT = 11;
    /** Width of a flavor row. */
    public static final int DRAFT_ROW_WIDTH = 150;

    // #region Run view

    /** Left edge of the 3x3 direction pad. */
    public static final int PAD_X = 116;
    /** Top edge of the 3x3 direction pad. */
    public static final int PAD_Y = 38;
    /** Edge length of a pad button. */
    public static final int PAD_BUTTON = 11;
    /** Left edge of the points bars. */
    public static final int POINTS_X = 116;
    /** Top edge of the first points bar. */
    public static final int POINTS_Y = 76;
    /** Height of one points bar row. */
    public static final int POINTS_ROW_HEIGHT = 7;
    /** Width of a points bar. */
    public static final int POINTS_WIDTH = 34;
    /** Left edge of the board. */
    public static final int BOARD_X = 160;
    /** Top edge of the board. */
    public static final int BOARD_Y = 34;
    /** Edge length of one board cell. */
    public static final int CELL = 10;
    /** Left edge of the line summarizing the effects. */
    public static final int EFFECTS_X = 160;
    /** Top edge of the line summarizing the effects. */
    public static final int EFFECTS_Y = 126;

    // #region Buttons (both phases)

    /** Left edge of the primary button (Season, Accept). */
    public static final int PRIMARY_X = 190;
    /** Top edge of the primary button. */
    public static final int PRIMARY_Y = 136;
    /** Width of the primary button. */
    public static final int PRIMARY_WIDTH = 78;
    /** Left edge of the Cancel button, drafting only. */
    public static final int CANCEL_X = 116;
    /** Top edge of the Cancel button. */
    public static final int CANCEL_Y = 136;
    /** Width of the Cancel button. */
    public static final int CANCEL_WIDTH = 70;
    /** Height of a button. */
    public static final int BUTTON_HEIGHT = 12;

    // #region Player inventory

    /** Left edge of the player inventory. */
    public static final int INVENTORY_X = (IMAGE_WIDTH - 176) / 2;
    /** Top edge of the main inventory rows. */
    public static final int INVENTORY_Y = 166;
    /** Top edge of the hotbar. */
    public static final int HOTBAR_Y = 224;

    private GrinderLayout() {
    }
}
