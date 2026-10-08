package com.drunkencod.spice_road.compat.viewer;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.tooltip.TooltipRenderUtil;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.RegistryAccess;
import net.minecraft.locale.Language;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.util.FormattedCharSequence;

import com.drunkencod.spice_road.tooltip.SpiceFlavorTooltips;

/**
 * Draws the decorations of a {@link ViewerLayout} (everything but its slots),
 * shared by every Recipe Viewer adapter so the entries look the same in each.
 * Client-only.
 */
public final class ViewerDrawing {

    /**
     * How far a {@link ViewerLayout.Panel}'s tooltip-style border reaches past
     * its content area, on every side.
     */
    public static final int PANEL_BORDER = 4;

    /** Appended to a line shortened to its {@link ViewerLayout.Text#maxWidth()}. */
    private static final Component ELLIPSIS = Component.literal("...");

    private ViewerDrawing() {
    }

    /**
     * Draws the panels, then the sprites, then the text of {@code layout}.
     *
     * @param graphics The graphics to draw with, translated to the entry's
     *                 top-left corner.
     * @param layout   The layout to draw.
     */
    public static void draw(GuiGraphics graphics, ViewerLayout layout) {
        for (ViewerLayout.Panel panel : layout.panels())
            TooltipRenderUtil.renderTooltipBackground(graphics, panel.x(), panel.y(), panel.width(), panel.height(), 0);
        for (ViewerLayout.Sprite sprite : layout.sprites())
            graphics.blitSprite(sprite.id(), sprite.x(), sprite.y(), sprite.width(), sprite.height());
        Font font = Minecraft.getInstance().font;
        for (ViewerLayout.Text text : layout.texts()) {
            graphics.pose().pushPose();
            graphics.pose().translate(text.x(), text.y(), 0F);
            graphics.pose().scale(text.scale(), text.scale(), 1F);
            graphics.drawString(font, visibleText(font, text), 0, 0, text.color(), text.shadow());
            graphics.pose().popPose();
        }
    }

    /**
     * @param font The font to measure with.
     * @param text A line of text.
     * @return The line, shortened with an {@link #ELLIPSIS} if it overflows.
     */
    private static FormattedCharSequence visibleText(Font font, ViewerLayout.Text text) {
        if (!text.overflows(font::width))
            return text.text().getVisualOrderText();
        int unscaledWidth = (int) (text.maxWidth() / text.scale()) - font.width(ELLIPSIS);
        FormattedText head = font.substrByWidth(text.text(), Math.max(0, unscaledWidth));
        return Language.getInstance().getVisualOrder(FormattedText.composite(head, ELLIPSIS));
    }

    /**
     * @return The registries of the world the client is in, for
     *         {@link ViewerEntry#layout}; empty ones outside of a world.
     */
    public static HolderLookup.Provider registries() {
        var level = Minecraft.getInstance().level;
        return level != null ? level.registryAccess() : RegistryAccess.EMPTY;
    }

    /**
     * @param layout The layout being hovered.
     * @param mouseX Mouse X, relative to the entry.
     * @param mouseY Mouse Y, relative to the entry.
     * @return The full text of a shortened line under the mouse, if any,
     *         followed by the lines of the topmost (last added) tooltip area
     *         under it; empty if there is neither.
     */
    public static List<Component> tooltipAt(ViewerLayout layout, double mouseX, double mouseY) {
        List<Component> lines = new ArrayList<>();
        for (ViewerLayout.Text text : layout.texts()) {
            boolean hovered = mouseX >= text.x() && mouseX < text.x() + text.maxWidth()
                    && mouseY >= text.y() && mouseY < text.y() + text.height();
            if (hovered && text.overflows(SpiceFlavorTooltips::measureWidth))
                lines.add(text.text());
        }
        List<ViewerLayout.Tooltip> tooltips = layout.tooltips();
        for (int i = tooltips.size() - 1; i >= 0; i--) {
            if (tooltips.get(i).contains(mouseX, mouseY)) {
                lines.addAll(tooltips.get(i).lines());
                break;
            }
        }
        return lines;
    }
}
