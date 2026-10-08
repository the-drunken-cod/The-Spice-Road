package com.drunkencod.spice_road.compat.viewer;

import java.util.ArrayList;
import java.util.List;
import java.util.function.ToIntFunction;

import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

/**
 * The positioned content of one Recipe Viewer entry, built fresh whenever a
 * viewer lays the entry out so it always reflects the current config. Limited
 * on purpose to what every supported viewer draws alike: item slots, plus
 * decorations (panels, sprites, text, tooltip areas) that {@link ViewerDrawing}
 * renders the same way for all of them.
 * <p>
 * Coordinates are relative to the entry's top-left corner, in pixels.
 *
 * @param slots    The item slots.
 * @param panels   Dark, tooltip-styled backgrounds, drawn first.
 * @param sprites  GUI sprites, drawn over the panels.
 * @param texts    Text, drawn last.
 * @param tooltips Areas that show a tooltip while hovered.
 */
public record ViewerLayout(List<Slot> slots, List<Panel> panels, List<Sprite> sprites, List<Text> texts,
        List<Tooltip> tooltips) {

    /** Edge length of an item slot, including its border. */
    public static final int SLOT_SIZE = 18;

    /** How an entry's slot relates to it, which decides how lookups find it. */
    public enum Role {
        /** Shown when looking up the uses of its stacks. */
        INPUT,
        /** Shown when looking up how to obtain its stacks. */
        OUTPUT
    }

    /**
     * An item slot.
     *
     * @param role    How the slot relates to the entry.
     * @param x       Left edge of the slot's border.
     * @param y       Top edge of the slot's border.
     * @param stacks  The stacks it cycles through; never empty.
     * @param tooltip Extra lines appended to a hovered stack's tooltip.
     */
    public record Slot(Role role, int x, int y, List<ItemStack> stacks, List<Component> tooltip) {
    }

    /**
     * A dark background in the style of an item tooltip, so text colored for
     * tooltips stays readable on a viewer's light background.
     *
     * @param x      Left edge.
     * @param y      Top edge.
     * @param width  Width.
     * @param height Height.
     */
    public record Panel(int x, int y, int width, int height) {
    }

    /**
     * A sprite of the GUI atlas.
     *
     * @param id     The sprite's ID, e.g. {@code minecraft:container/furnace/lit_progress}.
     * @param x      Left edge.
     * @param y      Top edge.
     * @param width  Width.
     * @param height Height.
     */
    public record Sprite(ResourceLocation id, int x, int y, int width, int height) {
    }

    /**
     * One line of text, shortened with an ellipsis where it would grow past
     * {@code maxWidth}. A shortened line shows its full text when hovered.
     *
     * @param text     The text.
     * @param x        Left edge.
     * @param y        Top edge.
     * @param color    Base color as {@code 0xRRGGBB}; styled parts keep their own.
     * @param shadow   Whether to draw a drop shadow.
     * @param maxWidth Most pixels the drawn line may span; {@code 0} for no limit.
     * @param scale    Factor the line is drawn at, e.g. to fit a block of
     *                 aligned lines into a panel as a whole.
     */
    public record Text(Component text, int x, int y, int color, boolean shadow, int maxWidth, float scale) {

        /**
         * @param width Measures a text's unscaled width, in pixels.
         * @return Whether the line is too wide for {@link #maxWidth} and gets
         *         shortened.
         */
        public boolean overflows(ToIntFunction<FormattedText> width) {
            return maxWidth > 0 && width.applyAsInt(text) * scale > maxWidth;
        }

        /** @return The height the line spans, in pixels. */
        public int height() {
            return (int) Math.ceil((ViewerText.LINE_HEIGHT - 1) * scale);
        }
    }

    /**
     * An area showing a tooltip while hovered.
     *
     * @param x      Left edge.
     * @param y      Top edge.
     * @param width  Width.
     * @param height Height.
     * @param lines  The tooltip's lines.
     */
    public record Tooltip(int x, int y, int width, int height, List<Component> lines) {

        /**
         * @param mouseX Mouse X, relative to the entry.
         * @param mouseY Mouse Y, relative to the entry.
         * @return Whether the mouse is over this area.
         */
        public boolean contains(double mouseX, double mouseY) {
            return mouseX >= x && mouseX < x + width && mouseY >= y && mouseY < y + height;
        }
    }

    /** @return A new, empty builder. */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * @return The height the content takes up, from the entry's top edge to
     *         the lowest bottom edge of any part, counting panel borders.
     */
    public int height() {
        int height = 0;
        for (Slot slot : slots)
            height = Math.max(height, slot.y() + SLOT_SIZE);
        for (Panel panel : panels)
            height = Math.max(height, panel.y() + panel.height() + ViewerDrawing.PANEL_BORDER);
        for (Sprite sprite : sprites)
            height = Math.max(height, sprite.y() + sprite.height());
        for (Text text : texts)
            height = Math.max(height, text.y() + text.height());
        for (Tooltip tooltip : tooltips)
            height = Math.max(height, tooltip.y() + tooltip.height());
        return height;
    }

    /**
     * @param role The role to filter by.
     * @return The stacks of every slot with {@code role}, one list per slot.
     */
    public List<List<ItemStack>> stacksOf(Role role) {
        return slots.stream().filter(slot -> slot.role() == role).map(Slot::stacks).toList();
    }

    /** Collects the parts of a {@link ViewerLayout}. */
    public static final class Builder {

        private final List<Slot> slots = new ArrayList<>();
        private final List<Panel> panels = new ArrayList<>();
        private final List<Sprite> sprites = new ArrayList<>();
        private final List<Text> texts = new ArrayList<>();
        private final List<Tooltip> tooltips = new ArrayList<>();

        private Builder() {
        }

        /**
         * Adds a slot, unless {@code stacks} holds nothing but empty stacks.
         *
         * @param role    How the slot relates to the entry.
         * @param x       Left edge of the slot's border.
         * @param y       Top edge of the slot's border.
         * @param stacks  The stacks it cycles through.
         * @param tooltip Extra lines appended to a hovered stack's tooltip.
         * @return This builder.
         */
        public Builder slot(Role role, int x, int y, List<ItemStack> stacks, List<Component> tooltip) {
            List<ItemStack> present = stacks.stream().filter(stack -> !stack.isEmpty()).toList();
            if (!present.isEmpty())
                slots.add(new Slot(role, x, y, present, List.copyOf(tooltip)));
            return this;
        }

        /**
         * @param x      Left edge.
         * @param y      Top edge.
         * @param width  Width.
         * @param height Height.
         * @return This builder.
         */
        public Builder panel(int x, int y, int width, int height) {
            panels.add(new Panel(x, y, width, height));
            return this;
        }

        /**
         * @param id     The GUI sprite's ID.
         * @param x      Left edge.
         * @param y      Top edge.
         * @param width  Width.
         * @param height Height.
         * @return This builder.
         */
        public Builder sprite(ResourceLocation id, int x, int y, int width, int height) {
            sprites.add(new Sprite(id, x, y, width, height));
            return this;
        }

        /**
         * Adds a line of text with no width limit.
         *
         * @param text   The text.
         * @param x      Left edge.
         * @param y      Top edge.
         * @param color  Base color as {@code 0xRRGGBB}.
         * @param shadow Whether to draw a drop shadow.
         * @return This builder.
         */
        public Builder text(Component text, int x, int y, int color, boolean shadow) {
            return text(text, x, y, color, shadow, 0, 1F);
        }

        /**
         * @param text     The text.
         * @param x        Left edge.
         * @param y        Top edge.
         * @param color    Base color as {@code 0xRRGGBB}.
         * @param shadow   Whether to draw a drop shadow.
         * @param maxWidth Most pixels the drawn line may span; {@code 0} for no limit.
         * @param scale    Factor the line is drawn at.
         * @return This builder.
         */
        public Builder text(Component text, int x, int y, int color, boolean shadow, int maxWidth, float scale) {
            texts.add(new Text(text, x, y, color, shadow, maxWidth, scale));
            return this;
        }

        /**
         * Adds a tooltip area, unless {@code lines} is empty.
         *
         * @param x      Left edge.
         * @param y      Top edge.
         * @param width  Width.
         * @param height Height.
         * @param lines  The tooltip's lines.
         * @return This builder.
         */
        public Builder tooltip(int x, int y, int width, int height, List<Component> lines) {
            if (!lines.isEmpty())
                tooltips.add(new Tooltip(x, y, width, height, List.copyOf(lines)));
            return this;
        }

        /** @return The finished layout. */
        public ViewerLayout build() {
            return new ViewerLayout(List.copyOf(slots), List.copyOf(panels), List.copyOf(sprites),
                    List.copyOf(texts), List.copyOf(tooltips));
        }
    }
}
