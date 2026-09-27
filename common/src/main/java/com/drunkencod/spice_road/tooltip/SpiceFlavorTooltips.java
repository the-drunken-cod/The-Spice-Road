package com.drunkencod.spice_road.tooltip;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.network.chat.MutableComponent;

import java.util.ArrayList;
import java.util.List;
import java.util.function.IntPredicate;
import java.util.function.ToIntFunction;

import com.drunkencod.spice_road.platform.Services;
import com.drunkencod.spice_road.spice.FlavorAxis;
import com.drunkencod.spice_road.spice.SpiceProfile;

/**
 * Formats a Spice's Flavor Axis scores as tooltip lines, one per axis, with a
 * bracketed label in the middle and a progress bar extending left (negative)
 * or right (positive) of it:
 *
 * <pre>
 *      [ Spicy ]+++++
 *      [Sweet  ]++-
 *  -+++[Mellow ]
 * </pre>
 *
 * The outermost bar character is a {@link #HALF_BAR_CHAR} for odd multiples of
 * half a {@link #BAR_CHAR}, doubling the bar's resolution.
 *
 * Padding is measured in pixels (see {@link #setTextWidthMeasurer}), so the
 * bars line up despite the proportional font and adapt to the current locale.
 */
public class SpiceFlavorTooltips {

    /** Number of bar characters on either side of the label at a score of ±1. */
    public static final int BAR_LENGTH = 5;
    /** Character progress bars are drawn with. */
    public static final String BAR_CHAR = "+";
    /**
     * Outermost bar character, when the score ends halfway through a
     * {@link #BAR_CHAR}.
     */
    public static final String HALF_BAR_CHAR = "-";
    /**
     * Factor scores are multiplied by when shown as a number inside the brackets.
     */
    public static final int VALUE_SCALE = 10;
    /** Color ({@code 0xRRGGBB}) of the label brackets and label separator. */
    public static final int BRACKET_COLOR = 0x777777;

    /**
     * Measures the rendered width of a text. Defaults to its character count,
     * which falls back to plain character padding.
     */
    private static ToIntFunction<FormattedText> textWidth = text -> text.getString().length();

    private SpiceFlavorTooltips() {
    }

    /**
     * Sets the function used to measure rendered text widths, typically the
     * client font's {@code width} method. Must be called from client init.
     *
     * @param measurer Returns the rendered width of a text, in pixels
     */
    public static void setTextWidthMeasurer(ToIntFunction<FormattedText> measurer) {
        textWidth = measurer;
    }

    /**
     * Formats every {@link FlavorAxis} of a {@link SpiceProfile} as a
     * tooltip line, in {@link FlavorAxis} enum order.
     *
     * @param profile The Spice Profile to format
     * @return One tooltip line per Flavor Axis
     */
    public static List<Component> formatFlavorAxes(SpiceProfile profile) {
        boolean bothLabels = Services.CONFIG.isTooltipBothAxisLabelsShown();
        boolean showValues = Services.CONFIG.isTooltipAxisValueShown();
        Padder padder = Padder.measure();

        int labelWidth;
        if (showValues) {
            // Sized over the shown rows only, since sizing for every possible value would
            // leave most rows with a wide gap. All padding goes between label and number,
            // at least about a space wide
            List<Integer> rowWidths = new ArrayList<>();
            for (FlavorAxis axis : FlavorAxis.values())
                rowWidths.add(valueRowWidth(axis, profile.get(axis), bothLabels));
            int minGap = Math.max(1, padder.space() - 1);
            labelWidth = padder.alignedWidth(rowWidths, px -> px >= minGap && padder.isPaddable(px));
        } else {
            // Sized over every label any axis could show, so the layout stays the same
            // across Spices
            List<Integer> labelWidths = new ArrayList<>();
            for (FlavorAxis axis : FlavorAxis.values()) {
                labelWidths.add(width(axisLabel(axis, true, bothLabels)));
                labelWidths.add(width(axisLabel(axis, false, bothLabels)));
            }
            labelWidth = padder.alignedWidth(labelWidths, padder::isSplittable);
        }

        List<Integer> barWidths = new ArrayList<>();
        for (int halfSteps = 0; halfSteps <= BAR_LENGTH * 2; halfSteps++)
            barWidths.add(width(Component.literal(barText(halfSteps, false))));
        int barWidth = padder.alignedWidth(barWidths, padder::isPaddable);

        List<Component> lines = new ArrayList<>(FlavorAxis.values().length);
        for (FlavorAxis axis : FlavorAxis.values())
            lines.add(formatLine(axis, profile.get(axis), bothLabels, showValues, padder, labelWidth, barWidth));
        return lines;
    }

    /**
     * Formats a single Flavor Axis score as a tooltip line.
     *
     * @param axis       The Flavor Axis
     * @param value      Score in {@code [-1, 1]}
     * @param bothLabels Whether to show both pole labels
     * @param showValues Whether to show the scaled value after the label
     * @param padder     Builds the padding
     * @param labelWidth Common width of the space between the brackets
     * @param barWidth   Common width of the space left of the opening bracket
     * @return The formatted tooltip line
     */
    private static Component formatLine(FlavorAxis axis, double value, boolean bothLabels, boolean showValues,
            Padder padder, int labelWidth, int barWidth) {
        // Exactly 0 is labeled as positive with an empty bar, any other score shows at
        // least a half step
        boolean positive = value >= 0D;
        int halfSteps = value == 0D ? 0
                : Math.max(1, (int) Math.round(Math.min(Math.abs(value), 1D) * BAR_LENGTH * 2));
        Component bar = Component.literal(barText(halfSteps, positive)).withColor(axis.getColor(positive));
        Component leftBar = positive ? Component.empty() : bar;

        MutableComponent label = axisLabel(axis, positive, bothLabels);
        MutableComponent line = Component.empty()
                .append(padder.build(barWidth - width(leftBar)))
                .append(leftBar)
                .append(Component.literal("[").withColor(BRACKET_COLOR));
        if (showValues) {
            line.append(label)
                    .append(valueSeparator())
                    .append(padder.build(labelWidth - valueRowWidth(axis, value, bothLabels)))
                    .append(valueNumber(axis, value));
        } else {
            int labelPadding = labelWidth - width(label);
            int labelPaddingLeft = padder.split(labelPadding);
            line.append(padder.build(labelPaddingLeft))
                    .append(label)
                    .append(padder.build(labelPadding - labelPaddingLeft));
        }
        line.append(Component.literal("]").withColor(BRACKET_COLOR));
        if (positive)
            line.append(bar);
        return line;
    }

    /**
     * @param axis       The Flavor Axis
     * @param value      Score in {@code [-1, 1]}
     * @param bothLabels Whether to show both pole labels
     * @return Width of a value row's label, separator and number, without
     *         the padding between them
     */
    private static int valueRowWidth(FlavorAxis axis, double value, boolean bothLabels) {
        return width(axisLabel(axis, value >= 0D, bothLabels)) + width(valueSeparator())
                + width(valueNumber(axis, value));
    }

    /** @return The separator between a label and its value, {@code ":"} */
    private static Component valueSeparator() {
        return Component.literal(":").withColor(BRACKET_COLOR);
    }

    /**
     * @param axis  The Flavor Axis, whose pole color is used
     * @param value Score in {@code [-1, 1]}
     * @return The score multiplied by {@link #VALUE_SCALE}, e.g. {@code "5"}
     */
    private static Component valueNumber(FlavorAxis axis, double value) {
        return Component.literal(String.valueOf(Math.round(value * VALUE_SCALE)))
                .withColor(axis.getColor(value >= 0D));
    }

    /**
     * @param halfSteps Bar length, in halves of a {@link #BAR_CHAR}
     * @param positive  Whether the bar extends to the right, putting its
     *                  outermost character last instead of first
     * @return The bar's text, ending in a {@link #HALF_BAR_CHAR} on odd
     *         {@code halfSteps}
     */
    private static String barText(int halfSteps, boolean positive) {
        String full = BAR_CHAR.repeat(halfSteps / 2);
        if (halfSteps % 2 == 0)
            return full;
        return positive ? full + HALF_BAR_CHAR : HALF_BAR_CHAR + full;
    }

    /**
     * Builds the colored display label of a {@link FlavorAxis}, e.g.
     * {@code SWEET_BITTER} -> {@code "Sweet"}, or {@code "Sweet / Bitter"}
     * with the pole matching {@code positive} in bold and underlined.
     *
     * @param axis       The Flavor Axis
     * @param positive   Which pole the value is on
     * @param bothLabels Whether to show both pole labels
     * @return The display label
     */
    public static MutableComponent axisLabel(FlavorAxis axis, boolean positive, boolean bothLabels) {
        if (!bothLabels)
            return poleLabel(axis, positive);
        MutableComponent positiveLabel = poleLabel(axis, true);
        MutableComponent negativeLabel = poleLabel(axis, false);
        (positive ? positiveLabel : negativeLabel).withStyle(ChatFormatting.BOLD, ChatFormatting.UNDERLINE);
        return Component.empty()
                .append(positiveLabel)
                .append(Component.literal(" / ").withColor(BRACKET_COLOR))
                .append(negativeLabel);
    }

    /**
     * @param axis     The Flavor Axis
     * @param positive Which pole to label
     * @return The pole's translated name, in the pole's color
     */
    private static MutableComponent poleLabel(FlavorAxis axis, boolean positive) {
        return Component.translatable(positive ? axis.positiveTranslationKey() : axis.negativeTranslationKey())
                .withColor(axis.getColor(positive));
    }

    private static int width(FormattedText text) {
        return textWidth.applyAsInt(text);
    }

    /**
     * Builds invisible padding of exact pixel widths from regular and bold
     * spaces, since bold glyphs are wider and give a second step size.
     *
     * @param space     Width of a regular space
     * @param boldSpace Width of a bold space
     */
    private record Padder(int space, int boldSpace) {

        /** @return A padder using the current text width measurer. */
        static Padder measure() {
            return new Padder(width(Component.literal(" ")),
                    width(Component.literal(" ").withStyle(ChatFormatting.BOLD)));
        }

        /**
         * @return The smallest width of at least the largest of {@code widths},
         *         for which {@code fits} accepts the padding each of them needs,
         *         or just the largest width if none is found
         */
        int alignedWidth(List<Integer> widths, IntPredicate fits) {
            int max = widths.stream().mapToInt(Integer::intValue).max().orElse(0);
            // Any padding beyond this is always expressible as a sum of both step sizes
            int limit = max + this.space * this.boldSpace;
            for (int candidate = max; candidate <= limit; candidate++) {
                int target = candidate;
                if (widths.stream().allMatch(w -> fits.test(target - w)))
                    return candidate;
            }
            return max;
        }

        /** @return Whether exactly {@code px} pixels of padding can be built. */
        boolean isPaddable(int px) {
            return this.boldSpaceCount(px) >= 0;
        }

        /**
         * @return Whether {@code px} pixels of padding can be split into two paddable
         *         shares.
         */
        boolean isSplittable(int px) {
            int left = this.split(px);
            return this.isPaddable(left) && this.isPaddable(px - left);
        }

        /**
         * @return The left share of {@code px} pixels of padding, as close to
         *         half as possible while leaving both shares paddable
         */
        int split(int px) {
            int half = px / 2;
            for (int offset = 0; offset <= half; offset++) {
                if (this.isPaddable(half - offset) && this.isPaddable(px - half + offset))
                    return half - offset;
                if (this.isPaddable(half + offset) && this.isPaddable(px - half - offset))
                    return half + offset;
            }
            return half;
        }

        /**
         * @return How many bold spaces make up exactly {@code px} pixels
         *         together with regular spaces, or {@code -1} if impossible
         */
        int boldSpaceCount(int px) {
            if (px < 0 || this.space <= 0 || this.boldSpace <= 0)
                return -1;
            for (int bold = 0; bold * this.boldSpace <= px; bold++) {
                if ((px - bold * this.boldSpace) % this.space == 0)
                    return bold;
            }
            return -1;
        }

        /**
         * @return Invisible padding of {@code px} pixels, or as close as
         *         possible below that if it can't be built exactly
         */
        Component build(int px) {
            if (px <= 0 || this.space <= 0)
                return Component.empty();
            int bold = Math.max(this.boldSpaceCount(px), 0);
            int regular = (px - bold * this.boldSpace) / this.space;
            return Component.empty()
                    .append(" ".repeat(regular))
                    .append(Component.literal(" ".repeat(bold)).withStyle(ChatFormatting.BOLD));
        }
    }
}
