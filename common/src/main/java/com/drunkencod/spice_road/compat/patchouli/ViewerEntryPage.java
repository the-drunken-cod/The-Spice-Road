package com.drunkencod.spice_road.compat.patchouli;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

import vazkii.patchouli.client.book.gui.GuiBook;
import vazkii.patchouli.client.book.gui.GuiBookEntry;
import vazkii.patchouli.client.book.page.abstr.PageWithText;

import com.drunkencod.spice_road.compat.viewer.ViewerDrawing;
import com.drunkencod.spice_road.compat.viewer.ViewerEntry;
import com.drunkencod.spice_road.compat.viewer.ViewerLayout;
import com.drunkencod.spice_road.compat.viewer.ViewerText;

/**
 * A Flavor Folio page that draws {@link ViewerLayout layouts} of the kind the
 * Recipe Viewer categories use, stacked below an optional title, with an
 * optional text underneath. The layouts are built at the width of a book page
 * each time the page is shown, so they always reflect the current config and
 * synced data. Subclasses only resolve which layouts the page's JSON names.
 * <p>
 * Optional JSON fields on top of Patchouli's text pages: {@code title} (a
 * replacement for the page's default one, which may be empty) and {@code text}
 * (shown below the entries).
 */
public abstract class ViewerEntryPage extends PageWithText {

    /** Height of the title row, including the separator below it. */
    private static final int TITLE_HEIGHT = 16;

    /** Space between the entries, and between the entries and the text. */
    private static final int GAP = 6;

    private static final int SLOT_FRAME_COLOR = 0x66000000;
    private static final int SLOT_FILL_COLOR = 0x22000000;
    private static final int FOOTER_COLOR = 0x404040;
    private static final int MISSING_COLOR = 0xAA0000;

    private String title;

    private transient Component titleText = Component.empty();
    private transient List<Placed> placed = List.of();
    private transient List<Component> footer = List.of();
    private transient Component missing = Component.empty();
    private transient int titleBottom;
    private transient int footerY;
    private transient int contentBottom;

    /**
     * A layout and where it starts on the page.
     *
     * @param layout The content.
     * @param y      Top edge, relative to the page.
     */
    private record Placed(ViewerLayout layout, int y) {
    }

    /**
     * @param level The client's level.
     * @param width Width of the page, in pixels.
     * @return The layouts to draw, in order; empty if what the page names
     *         can't be found, in which case {@link #missingText()} is shown.
     */
    protected abstract List<ViewerLayout> layouts(Level level, int width);

    /** @return What is shown instead of the layouts if {@link #layouts} finds none. */
    protected abstract Component missingText();

    /**
     * @param entries Viewer entries.
     * @param width   Width to lay them out for.
     * @return Their layouts, in order.
     */
    protected static List<ViewerLayout> layoutAll(List<? extends ViewerEntry> entries, int width) {
        return entries.stream().map(entry -> entry.layout(ViewerDrawing.registries(), width)).toList();
    }

    /** @return The title if the JSON has no {@code title}; empty for none. */
    protected Component defaultTitle() {
        return Component.empty();
    }

    /** @return Lines shown below the layouts, one per row. */
    protected List<Component> footer() {
        return List.of();
    }

    @Override
    public void onDisplayed(GuiBookEntry parent, int left, int top) {
        Level level = parent.getMinecraft().level;
        List<ViewerLayout> layouts = level == null ? List.of() : layouts(level, GuiBook.PAGE_WIDTH);
        titleText = title != null && !title.isEmpty() ? i18nText(title) : defaultTitle();
        titleBottom = titleText.getString().isEmpty() ? 0 : TITLE_HEIGHT;

        int y = titleBottom;
        List<Placed> laidOut = new ArrayList<>();
        for (ViewerLayout layout : layouts) {
            laidOut.add(new Placed(layout, y));
            y += layout.height() + GAP;
        }
        placed = laidOut;
        missing = laidOut.isEmpty() ? missingText() : Component.empty();
        footer = laidOut.isEmpty() ? List.of() : footer();
        footerY = y - GAP + 2;
        contentBottom = laidOut.isEmpty() ? y + ViewerText.LINE_HEIGHT : footerY + footer.size() * ViewerText.LINE_HEIGHT + GAP;
        super.onDisplayed(parent, left, top);
    }

    @Override
    public int getTextHeight() {
        return contentBottom;
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float pticks) {
        if (titleBottom > 0) {
            parent.drawCenteredStringNoShadow(graphics, titleText.getVisualOrderText(), GuiBook.PAGE_WIDTH / 2, 0,
                    book.headerColor);
            GuiBook.drawSeparator(graphics, book, 0, 10);
        }
        if (!missing.getString().isEmpty())
            graphics.drawString(fontRenderer, missing, 0, titleBottom, MISSING_COLOR, false);

        List<Component> hovered = new ArrayList<>();
        for (Placed entry : placed)
            drawEntry(graphics, entry, mouseX, mouseY, hovered);
        for (int i = 0; i < footer.size(); i++)
            graphics.drawString(fontRenderer, footer.get(i), 0, footerY + i * ViewerText.LINE_HEIGHT, FOOTER_COLOR,
                    false);

        if (!hovered.isEmpty())
            parent.setTooltip(hovered);
        super.render(graphics, mouseX, mouseY, pticks);
    }

    /**
     * Draws one layout and collects the tooltip of whatever is under the mouse.
     *
     * @param graphics The page's graphics.
     * @param entry    The layout and its position.
     * @param mouseX   Mouse X, relative to the page.
     * @param mouseY   Mouse Y, relative to the page.
     * @param hovered  Receives the lines of the hovered part's tooltip, if any.
     */
    private void drawEntry(GuiGraphics graphics, Placed entry, int mouseX, int mouseY, List<Component> hovered) {
        ViewerLayout layout = entry.layout();
        graphics.pose().pushPose();
        graphics.pose().translate(0, entry.y(), 0);
        ViewerDrawing.draw(graphics, layout);
        graphics.pose().popPose();

        for (ViewerLayout.Slot slot : layout.slots()) {
            int slotY = entry.y() + slot.y();
            graphics.fill(slot.x(), slotY, slot.x() + ViewerLayout.SLOT_SIZE, slotY + ViewerLayout.SLOT_SIZE,
                    SLOT_FILL_COLOR);
            graphics.renderOutline(slot.x(), slotY, ViewerLayout.SLOT_SIZE, ViewerLayout.SLOT_SIZE, SLOT_FRAME_COLOR);
            ItemStack stack = slot.stacks().get((parent.ticksInBook / 20) % slot.stacks().size());
            graphics.renderItem(stack, slot.x() + 1, slotY + 1);
            graphics.renderItemDecorations(fontRenderer, stack, slot.x() + 1, slotY + 1);
            if (parent.isMouseInRelativeRange(mouseX, mouseY, slot.x() + 1, slotY + 1, 16, 16)) {
                hovered.clear();
                hovered.addAll(Screen.getTooltipFromItem(mc, stack));
                hovered.addAll(slot.tooltip());
            }
        }
        if (hovered.isEmpty())
            hovered.addAll(ViewerDrawing.tooltipAt(layout, mouseX, mouseY - entry.y()));
    }
}
