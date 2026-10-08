package com.drunkencod.spice_road.compat.viewer;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;

import com.drunkencod.spice_road.Constants;

/**
 * Translation keys, colors and text helpers shared by the Recipe Viewer
 * entries.
 */
public final class ViewerText {

    /** Prefix of every translation key the Recipe Viewer entries use. */
    public static final String KEY = Constants.MOD_ID + ".viewer.";

    /** Color of plain text drawn straight onto a viewer's light background. */
    public static final int DARK_TEXT_COLOR = 0x404040;

    /** Color of text drawn onto a {@link ViewerLayout.Panel}. */
    public static final int PANEL_TEXT_COLOR = 0xFFFFFF;

    /** Height of one line of text on a panel, including spacing. */
    public static final int LINE_HEIGHT = 10;

    /** Most names a tooltip lists before summarizing the rest. */
    public static final int MAX_LISTED = 8;

    private ViewerText() {
    }

    /**
     * @param key  The key, relative to {@link #KEY}.
     * @param args The arguments.
     * @return The translatable text.
     */
    public static MutableComponent translatable(String key, Object... args) {
        return Component.translatable(KEY + key, args);
    }

    /**
     * @param labelKey The key of the label, relative to {@link #KEY}.
     * @param value    The value.
     * @return A {@code "Label: value"} line, with the label in gray.
     */
    public static Component labeled(String labelKey, Component value) {
        return translatable("label", translatable(labelKey).withStyle(ChatFormatting.GRAY), value);
    }

    /**
     * One unit of a {@link #duration}, e.g. the {@code 15s} of {@code 1m 15s}.
     *
     * @param unit   The unit's key, relative to {@code KEY + "time."}: {@code h},
     *               {@code m} or {@code s}.
     * @param amount The amount of the unit, as shown.
     */
    public record DurationPart(String unit, String amount) {
    }

    /**
     * @param seconds A duration, in seconds.
     * @return The duration split into hours, minutes and seconds, leaving out
     *         units of zero, e.g. {@code 2m} or {@code 1m 15s}. Durations below
     *         ten seconds keep one decimal (e.g. {@code 7.5s}), longer ones are
     *         rounded to the second.
     */
    public static List<DurationPart> durationParts(double seconds) {
        if (seconds < 10D) {
            double tenths = Math.round(seconds * 10D) / 10D;
            return List.of(new DurationPart("s", amount(tenths)));
        }
        long total = Math.round(seconds);
        List<DurationPart> parts = new ArrayList<>();
        if (total >= 3600)
            parts.add(new DurationPart("h", String.valueOf(total / 3600)));
        if (total % 3600 >= 60)
            parts.add(new DurationPart("m", String.valueOf(total % 3600 / 60)));
        if (total % 60 > 0)
            parts.add(new DurationPart("s", String.valueOf(total % 60)));
        return parts;
    }

    /**
     * @param seconds A duration, in seconds.
     * @return The duration like {@code "2m"}, {@code "1m 15s"} or {@code "45s"};
     *         see {@link #durationParts}.
     */
    public static Component duration(double seconds) {
        MutableComponent text = Component.empty();
        for (DurationPart part : durationParts(seconds)) {
            if (!text.getSiblings().isEmpty())
                text.append(" ");
            text.append(translatable("time." + part.unit(), part.amount()));
        }
        return text;
    }

    /**
     * @param chance A chance from 0 to 1.
     * @return The chance like {@code "50%"}, with one decimal if it isn't whole.
     */
    public static String percent(double chance) {
        double percent = chance * 100D;
        return Math.abs(percent - Math.rint(percent)) < 1e-6 ? Math.round(percent) + "%"
                : String.format(Locale.ROOT, "%.1f%%", percent);
    }

    /**
     * @param amount An amount that may be fractional.
     * @return The amount without a fraction if it's whole, otherwise with up
     *         to two decimals.
     */
    public static String amount(double amount) {
        if (Math.abs(amount - Math.rint(amount)) < 1e-6)
            return String.valueOf(Math.round(amount));
        return String.format(Locale.ROOT, "%.2f", amount).replaceAll("0+$", "");
    }

    /**
     * @param names Names to list, in order.
     * @return One line per name, indented and in dark gray, up to
     *         {@link #MAX_LISTED}, followed by an "and N more" line for the
     *         rest.
     */
    public static List<Component> listed(List<Component> names) {
        List<Component> lines = new ArrayList<>();
        for (int i = 0; i < Math.min(names.size(), MAX_LISTED); i++)
            lines.add(Component.literal(" ").append(names.get(i)).withStyle(ChatFormatting.DARK_GRAY));
        if (names.size() > MAX_LISTED)
            lines.add(translatable("and_more", names.size() - MAX_LISTED).withStyle(ChatFormatting.DARK_GRAY));
        return lines;
    }
}
