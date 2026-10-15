package com.drunkencod.spice_road.compat.patchouli;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemLore;
import net.minecraft.world.level.Level;

import vazkii.patchouli.client.book.BookContentsBuilder;
import vazkii.patchouli.client.book.BookEntry;
import vazkii.patchouli.client.book.gui.GuiBook;
import vazkii.patchouli.client.book.gui.GuiBookEntry;
import vazkii.patchouli.client.book.page.abstr.PageWithText;

import com.drunkencod.spice_road.Constants;
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

    private String title;

    private transient Component titleText = Component.empty();
    private transient List<Placed> placed = List.of();
    private transient List<Component> footer = List.of();
    private transient Component missing = Component.empty();
    private transient ItemStack hoveredStack = ItemStack.EMPTY;
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
     * Items this page is the one to read about: holding the lookup key over
     * them anywhere in the game, or Shift-clicking them in another page of the
     * book, leads here. Patchouli keeps one page per item, so a page should
     * only name items that no other page of the book covers.
     *
     * @param level The client's level.
     * @return The items, empty for none.
     */
    protected List<ItemStack> relevantStacks(Level level) {
        return List.of();
    }

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
    public void build(Level level, BookEntry entry, BookContentsBuilder builder, int pageNum) {
        super.build(level, entry, builder, pageNum);
        try {
            for (ItemStack stack : relevantStacks(level))
                entry.addRelevantStack(builder, stack, pageNum);
        } catch (RuntimeException e) {
            // A broken lookup link must never take the whole book down with it
            Constants.LOG.error("Could not register the lookup items of a {} page", getClass().getSimpleName(), e);
        }
    }

    @Override
    public void onDisplayed(GuiBookEntry parent, int left, int top) {
        Level level = parent.getMinecraft().level;
        List<ViewerLayout> layouts = level == null ? List.of() : layouts(level, GuiBook.PAGE_WIDTH);
        titleText = title != null && !title.isEmpty() ? i18nText(title) : defaultTitle();
        titleBottom = titleText.getString().isEmpty() ? 0 : FlavorFolioLayout.TITLE_HEIGHT;

        int y = titleBottom;
        List<Placed> laidOut = new ArrayList<>();
        for (ViewerLayout layout : layouts) {
            laidOut.add(new Placed(layout, y));
            y += layout.height() + FlavorFolioLayout.GAP;
        }
        placed = laidOut;
        missing = laidOut.isEmpty() ? missingText() : Component.empty();
        footer = laidOut.isEmpty() ? List.of() : footer();
        footerY = y - FlavorFolioLayout.GAP + FlavorFolioLayout.FOOTER_GAP;
        contentBottom = laidOut.isEmpty() ? y + ViewerText.LINE_HEIGHT
                : footerY + footer.size() * ViewerText.LINE_HEIGHT + FlavorFolioLayout.GAP;
        super.onDisplayed(parent, left, top);
    }

    @Override
    public int getTextHeight() {
        return contentBottom;
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float pticks) {
        if (titleBottom > 0) {
            parent.drawCenteredStringNoShadow(graphics, titleText.getVisualOrderText(), GuiBook.PAGE_WIDTH / 2,
                    FlavorFolioLayout.TITLE_Y, book.headerColor);
            GuiBook.drawSeparator(graphics, book, 0, FlavorFolioLayout.TITLE_SEPARATOR_Y);
        }
        if (!missing.getString().isEmpty())
            graphics.drawString(fontRenderer, missing, 0, titleBottom, FlavorFolioLayout.MISSING_COLOR, false);

        List<Component> hovered = new ArrayList<>();
        hoveredStack = ItemStack.EMPTY;
        for (Placed entry : placed)
            drawEntry(graphics, entry, mouseX, mouseY, hovered);
        for (int i = 0; i < footer.size(); i++)
            graphics.drawString(fontRenderer, footer.get(i), 0, footerY + i * ViewerText.LINE_HEIGHT,
                    FlavorFolioLayout.FOOTER_COLOR, false);

        if (!hoveredStack.isEmpty())
            parent.setTooltipStack(hoveredStack);
        else if (!hovered.isEmpty())
            parent.setTooltip(hovered);
        super.render(graphics, mouseX, mouseY, pticks);
    }

    /**
     * Draws one layout and collects the tooltip of whatever is under the mouse.
     * A hovered item slot is handed to Patchouli as an item tooltip rather than
     * as plain lines, so it gets the book's own lookup hint, Shift-click to the
     * item's page and the recipe viewer keybinds.
     *
     * @param graphics The page's graphics.
     * @param entry    The layout and its position.
     * @param mouseX   Mouse X, relative to the page.
     * @param mouseY   Mouse Y, relative to the page.
     * @param hovered  Receives the lines of the hovered non-slot part's tooltip, if any.
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
                    FlavorFolioLayout.SLOT_FILL_COLOR);
            graphics.renderOutline(slot.x(), slotY, ViewerLayout.SLOT_SIZE, ViewerLayout.SLOT_SIZE,
                    FlavorFolioLayout.SLOT_FRAME_COLOR);
            ItemStack stack = slot.stacks().get((parent.ticksInBook / 20) % slot.stacks().size());
            int itemX = slot.x() + ViewerLayout.SLOT_ITEM_INSET;
            int itemY = slotY + ViewerLayout.SLOT_ITEM_INSET;
            graphics.renderItem(stack, itemX, itemY);
            graphics.renderItemDecorations(fontRenderer, stack, itemX, itemY);
            if (parent.isMouseInRelativeRange(mouseX, mouseY, itemX, itemY, 16, 16))
                hoveredStack = withNotes(stack, slot.tooltip());
        }
        if (hoveredStack.isEmpty() && hovered.isEmpty())
            hovered.addAll(ViewerDrawing.tooltipAt(layout, mouseX, mouseY - entry.y()));
    }

    /**
     * Patchouli's item tooltips can't take extra lines, so they ride along as
     * the stack's lore. Its lookup only compares the item, so they don't get
     * in its way.
     *
     * @param stack The hovered stack.
     * @param notes The slot's extra tooltip lines.
     * @return {@code stack}, or a copy of it with {@code notes} added to its lore.
     */
    private static ItemStack withNotes(ItemStack stack, List<Component> notes) {
        if (notes.isEmpty())
            return stack;
        ItemStack copy = stack.copy();
        ItemLore lore = copy.getOrDefault(DataComponents.LORE, ItemLore.EMPTY);
        for (Component note : notes)
            lore = lore.withLineAdded(note.copy().withStyle(style -> style.withItalic(false)));
        copy.set(DataComponents.LORE, lore);
        return copy;
    }
}
