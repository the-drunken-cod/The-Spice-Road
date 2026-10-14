package com.drunkencod.spice_road.client;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;

import com.drunkencod.spice_road.Constants;
import com.drunkencod.spice_road.compat.patchouli.FlavorFolio;
import com.drunkencod.spice_road.platform.Services;

/**
 * The {@code [?]} label shared by the mod's GUIs. Hovering it explains the GUI,
 * refers to the Flavor Folio and, without Patchouli, says that the Folio is
 * unavailable. It can be hidden with the client config.
 */
public final class GuiHelpLabel {

    private static final String KEY_PREFIX = Constants.MOD_ID + ".gui.help.";
    private static final Component TEXT = Component.literal("[?]");
    private static final int COLOR_LIGHT = 0xFF404040;
    private static final int COLOR_DARK = 0xFFE0E0E0;
    /** Widest the tooltip's text may be before it wraps. */
    private static final int TOOLTIP_WIDTH = 220;
    /** How far the hover area extends past the text to the left and right. */
    private static final int HOVER_PADDING_X = 2;
    /** How far the hover area is shifted up from the text. */
    private static final int HOVER_OFFSET_Y = 2;

    private GuiHelpLabel() {
    }

    /**
     * Draws the label, right-aligned, and its tooltip while it is hovered. Does
     * nothing if the client config hides help labels. Call after everything else
     * is drawn, so the tooltip ends up on top.
     *
     * @param graphics    The graphics to draw with.
     * @param font        The font to draw with.
     * @param rightX      Right edge of the label on screen.
     * @param y           Top edge of the label on screen.
     * @param mouseX      Mouse X on screen.
     * @param mouseY      Mouse Y on screen.
     * @param description What the GUI does, shown at the top of the tooltip.
     */
    public static void render(GuiGraphics graphics, Font font, int rightX, int y, int mouseX, int mouseY,
            Component description) {
        if (!Services.CONFIG.isShowHelpLabels())
            return;
        int width = font.width(TEXT);
        int x = rightX - width;
        graphics.drawString(font, TEXT, x, y, Services.CONFIG.isDarkMode() ? COLOR_DARK : COLOR_LIGHT, false);
        if (mouseX >= x - HOVER_PADDING_X && mouseX < x + width + HOVER_PADDING_X && mouseY >= y - HOVER_OFFSET_Y
                && mouseY < y - HOVER_OFFSET_Y + font.lineHeight)
            graphics.renderTooltip(font, tooltip(font, description), mouseX, mouseY);
    }

    /**
     * @return The wrapped tooltip: the description, a pointer to the Flavor
     *         Folio, and a notice if Patchouli, which the Folio needs, isn't
     *         installed.
     */
    private static List<FormattedCharSequence> tooltip(Font font, Component description) {
        Component folio = Component.translatable(Constants.MOD_ID + ".guide.name").withStyle(ChatFormatting.GOLD);
        List<FormattedCharSequence> lines = new ArrayList<>(font.split(description, TOOLTIP_WIDTH));
        lines.add(FormattedCharSequence.EMPTY);
        lines.addAll(font.split(Component.translatable(KEY_PREFIX + "folio", folio).withStyle(ChatFormatting.GRAY),
                TOOLTIP_WIDTH));
        if (!Services.PLATFORM.isModLoaded(FlavorFolio.PATCHOULI_MOD_ID))
            lines.addAll(font.split(
                    Component.translatable(KEY_PREFIX + "no_patchouli", folio).withStyle(ChatFormatting.RED),
                    TOOLTIP_WIDTH));
        return lines;
    }
}
