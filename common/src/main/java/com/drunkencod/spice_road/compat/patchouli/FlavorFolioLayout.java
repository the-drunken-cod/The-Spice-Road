package com.drunkencod.spice_road.compat.patchouli;

import com.drunkencod.spice_road.compat.viewer.layout.DryingLayout;
import com.drunkencod.spice_road.compat.viewer.layout.SpiceOriginLayout;
import com.drunkencod.spice_road.compat.viewer.layout.SpiceProfileLayout;

/**
 * The pixel layout of the Flavor Folio's own page types, at GUI scale 1 and
 * relative to the page's top-left corner.
 * <p>
 * The {@code spice_info}, {@code spice_profile}, {@code mix_preset} and
 * {@code drying_recipe} pages draw the same entries as the Recipe Viewer
 * categories, so their content is laid out by {@link SpiceOriginLayout},
 * {@link SpiceProfileLayout} and {@link DryingLayout}. This class only holds
 * what the book adds around them, and the {@code block_tag} page.
 */
final class FlavorFolioLayout {

    // #region Viewer entry pages

    /** Height of the title row, including the separator below it. */
    static final int TITLE_HEIGHT = 16;
    /** Top edge of the title's text. */
    static final int TITLE_Y = 0;
    /** Top edge of the separator below the title. */
    static final int TITLE_SEPARATOR_Y = 10;
    /** Space between the entries, and between the entries and the text. */
    static final int GAP = 6;
    /** Extra space between the last entry and the footer lines. */
    static final int FOOTER_GAP = 2;
    /** Color of an item slot's frame, as {@code 0xAARRGGBB}. */
    static final int SLOT_FRAME_COLOR = 0x66000000;
    /** Color of an item slot's fill, as {@code 0xAARRGGBB}. */
    static final int SLOT_FILL_COLOR = 0x22000000;
    /** Color of the footer lines, as {@code 0xRRGGBB}. */
    static final int FOOTER_COLOR = 0x404040;
    /** Color of the message shown where a page's subject can't be found. */
    static final int MISSING_COLOR = 0xAA0000;

    // #region Block tag page

    /** Most rows of slots the {@code block_tag} page shows. */
    static final int BLOCK_TAG_MAX_ROWS = 6;
    /** Gap between the last row of slots and the "and N more" line. */
    static final int BLOCK_TAG_MORE_GAP = 2;

    private FlavorFolioLayout() {
        // noop
    }
}
