package com.drunkencod.spice_road.compat.viewer.layout;

import com.drunkencod.spice_road.compat.viewer.ViewerLayout;
import com.drunkencod.spice_road.compat.viewer.ViewerText;

/**
 * The pixel layout of a drying recipe, shared by the Drying viewer category and
 * the Flavor Folio's {@code drying_recipe} page. Coordinates are relative to
 * the entry's top-left corner.
 */
public final class DryingLayout {

    // #region Size

    /** Width of the viewer category. */
    public static final int WIDTH = 120;
    /** Height of the viewer category. */
    public static final int HEIGHT = 60;

    // #region Recipe row

    /** Top edge of the slots in the recipe row. */
    public static final int SLOT_Y = 4;
    /** Left edge of the input slot. */
    public static final int INPUT_X = 26;
    /** Left edge of the arrow sprite. */
    public static final int ARROW_X = INPUT_X + 22;
    /** Top edge of the arrow sprite. */
    public static final int ARROW_Y = SLOT_Y + 1;
    /** Width of the arrow sprite. */
    public static final int ARROW_WIDTH = 24;
    /** Height of the arrow sprite. */
    public static final int ARROW_HEIGHT = 16;
    /** Left edge of the main result's slot. */
    public static final int RESULT_X = INPUT_X + 50;
    /** Left edge of the secondary result's slot. */
    public static final int SECONDARY_X = RESULT_X + ViewerLayout.SLOT_SIZE + 4;
    /** Top edge of the chance text, which is centered below a result's slot. */
    public static final int CHANCE_Y = SLOT_Y + ViewerLayout.SLOT_SIZE + 2;

    // #region Drying time

    /** Left edge of the drying time line. */
    public static final int TIME_X = 2;
    /** Top edge of the drying time line. */
    public static final int TIME_Y = 32;
    /** Height of the drying time line's tooltip area. */
    public static final int TIME_TOOLTIP_HEIGHT = ViewerText.LINE_HEIGHT - 1;

    // #region Heated drying time

    /** Top edge of the heated drying time's text. */
    public static final int HEATED_Y = TIME_Y + 14;
    /** Left edge of the flame sprite. */
    public static final int FLAME_X = 2;
    /** Top edge of the flame sprite, and of the heated line's tooltip area. */
    public static final int FLAME_Y = HEATED_Y - 3;
    /** Width of the flame sprite. */
    public static final int FLAME_WIDTH = 14;
    /** Height of the flame sprite, and of the heated line's tooltip area. */
    public static final int FLAME_HEIGHT = 14;
    /** Left edge of the heated drying time's text. */
    public static final int HEATED_TEXT_X = 19;

    private DryingLayout() {
        // noop
    }
}
