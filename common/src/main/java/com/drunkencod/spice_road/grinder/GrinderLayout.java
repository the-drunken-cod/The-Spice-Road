package com.drunkencod.spice_road.grinder;

/**
 * The pixel layout of the Spice Grinder GUI at GUI scale 1, shared by the menu
 * (slot positions) and the screen.
 */
public final class GrinderLayout {

    /** Width of the GUI. */
    public static final int IMAGE_WIDTH = 360;
    /** Height of the GUI. */
    public static final int IMAGE_HEIGHT = 270;

    // #region Left panel (spice list)

    /** Left edge of the spice list panel. */
    public static final int LIST_X = 8;
    /** Top edge of the spice list panel. */
    public static final int LIST_Y = 16;
    /** Width of the spice list panel. */
    public static final int LIST_WIDTH = 140;
    /** Height of the spice list panel. */
    public static final int LIST_HEIGHT = 164;
    /** Spice list group header offset. */
    public static final int LIST_GROUP_HEADER_X = 2;
    /** ItemStack renderer offset. */
    public static final int LIST_ITEM_X = 1;
    /**
     * Left edge of a row's item name, relative to the row's start. Leaves room for
     * the 16 px item icon.
     */
    public static final int LIST_NAME_X = 19;
    /** Height of one row of the spice list. */
    public static final int LIST_ROW_HEIGHT = 16;
    /** Rows visible at once. */
    public static final int LIST_ROWS = 10;
    /** Width of the list's scroll bar, at the right edge of the list panel. */
    public static final int LIST_SCROLLBAR_WIDTH = 4;
    /** Shortest the scroll bar's handle gets, however long the list is. */
    public static final int LIST_SCROLLBAR_MIN_HANDLE = 10;

    // #region Right panel

    /** Left edge of the right panel. */
    public static final int PANEL_X = 152;
    /** Top edge of the right panel. */
    public static final int PANEL_Y = 16;
    /** Width of the right panel. */
    public static final int PANEL_WIDTH = 200;
    /** Height of the right panel. */
    public static final int PANEL_HEIGHT = 164;

    /** Left edge of the food slot's item, 1 px inside its frame. */
    public static final int FOOD_SLOT_X = 331;
    /** Top edge of the food slot's item. */
    public static final int FOOD_SLOT_Y = 21;

    // #region Draft view

    /** Left edge of the draft's flavor rows. */
    public static final int DRAFT_ROWS_X = 156;
    /** Top edge of the first of the draft's flavor rows. */
    public static final int DRAFT_ROWS_Y = 44;
    /** Height of one flavor row. */
    public static final int DRAFT_ROW_HEIGHT = 11;
    /** Width of a flavor row. */
    public static final int DRAFT_ROW_WIDTH = 190;

    // #region Run view

    /** Left edge of the 3x3 direction pad, centered left of the board. */
    public static final int PAD_X = PANEL_X + 16;
    /** Top edge of the 3x3 direction pad. */
    public static final int PAD_Y = PANEL_Y + 8;
    /** Edge length of a pad button. */
    public static final int PAD_BUTTON = 13;
    /** Gap between two pad buttons. */
    public static final int PAD_GAP = 2;
    /** Left edge of the points bars. */
    public static final int POINTS_X = 156;
    /** Top edge of the first points bar. */
    public static final int POINTS_Y = 73;
    /** Height of one points bar row. */
    public static final int POINTS_ROW_HEIGHT = 11;
    /** Height of a points bar, drawn 1 px below the top of its row. */
    public static final int POINTS_BAR_HEIGHT = 9;
    /**
     * Fewest pixels between two notches of a points bar before they are left out.
     */
    public static final int POINTS_NOTCH_MIN_SPACING = 1;
    /** Width of a points bar. */
    public static final int POINTS_WIDTH = 64;
    /** Left edge of the board. */
    public static final int BOARD_X = 226;
    /** Top edge of the board. */
    public static final int BOARD_Y = 20;
    /** Edge length of one board cell. */
    public static final int CELL = 11;
    /** Edge length of the sprites on board cells and pad buttons. */
    public static final int SPRITE_SIZE = 7;
    /** Offset of a cell's sprite from the cell's top left corner. */
    public static final int CELL_SPRITE_OFFSET = (CELL - SPRITE_SIZE) / 2;
    /** Left edge of the line summarizing the effects. */
    public static final int EFFECTS_X = 226;
    /** Top edge of the line summarizing the effects. */
    public static final int EFFECTS_Y = 129;

    // #region Status line (both phases)

    /** Left edge of the status line's divider, which spans the right panel. */
    public static final int STATUS_DIVIDER_X = PANEL_X + 1;
    /** Top edge of the divider between the panel's content and the status line. */
    public static final int STATUS_DIVIDER_Y = PANEL_Y + PANEL_HEIGHT - 17;
    /** Left edge of the status line (event messages and hints). */
    public static final int STATUS_X = PANEL_X + 5;
    /** Top edge of the status line's text. */
    public static final int STATUS_Y = PANEL_Y + PANEL_HEIGHT - 12;
    /** Widest the status line may be before it is cut short. */
    public static final int STATUS_WIDTH = PANEL_WIDTH - 8;

    // #region Buttons (both phases)

    /** Height of a button. */
    public static final int BUTTON_HEIGHT = 14;
    /** Top edge of the buttons, just above the status line's divider. */
    public static final int BUTTONS_Y = STATUS_DIVIDER_Y - BUTTON_HEIGHT - 3;
    /** Left edge of the primary button while drafting (Season). */
    public static final int DRAFT_PRIMARY_X = 230;
    /** Top edge of the primary button while drafting. */
    public static final int DRAFT_PRIMARY_Y = BUTTONS_Y;
    /** Width of the primary button while drafting. */
    public static final int DRAFT_PRIMARY_WIDTH = 118;
    /** Left edge of the primary button during a run (Accept). */
    public static final int RUN_PRIMARY_X = 226;
    /** Top edge of the primary button during a run. */
    public static final int RUN_PRIMARY_Y = BUTTONS_Y;
    /** Width of the primary button during a run. */
    public static final int RUN_PRIMARY_WIDTH = 100;
    /** Left edge of the Cancel button, drafting only. */
    public static final int CANCEL_X = 156;
    /** Top edge of the Cancel button. */
    public static final int CANCEL_Y = BUTTONS_Y;
    /** Width of the Cancel button. */
    public static final int CANCEL_WIDTH = 70;

    // #region Player inventory

    /** Left edge of the player inventory. */
    public static final int INVENTORY_X = (IMAGE_WIDTH - 176) / 2;
    /** Top edge of the main inventory rows. */
    public static final int INVENTORY_Y = 188;
    /** Top edge of the hotbar. */
    public static final int HOTBAR_Y = 246;

    private GrinderLayout() {
    }
}
