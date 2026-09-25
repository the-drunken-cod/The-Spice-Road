package com.drunkencod.spice_road.tooltip;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

import com.drunkencod.spice_road.spice.FlavorAxis;
import com.drunkencod.spice_road.spice.SpiceProfile;

/**
 * Formats a Spice's Flavor Axis scores as tooltip lines, shown while Shift is
 * held.
 */
public class SpiceFlavorTooltips {

    /**
     * One Flavor Axis score of a Spice Profile.
     *
     * @param axisName Display name of the axis (e.g. {@code "Heat"})
     * @param value    Score in {@code [-1, 1]}, matching the eventual
     *                 {@code FlavorAxis}/{@code SpiceProfile} representation
     */
    public record FlavorValue(String axisName, double value) {
    }

    private SpiceFlavorTooltips() {
    }

    /**
     * Formats each {@link FlavorValue} as a tooltip line on the -100..100
     * integer scale, one axis per line.
     *
     * @param values The Flavor Axis scores for a Spice Profile (8 in the full
     *               enum, but this makes no assumption about the count)
     * @return One tooltip line per axis, in the order given
     */
    public static List<Component> formatFlavorAxes(List<FlavorValue> values) {
        return values.stream()
                .map(v -> Component.literal(v.axisName() + ": " + Math.round(v.value() * 100))
                        .withStyle(ChatFormatting.GRAY))
                .collect(Collectors.toList());
    }

    /**
     * Formats every {@link FlavorAxis} of a {@link SpiceProfile} as a
     * tooltip line, in {@link FlavorAxis} enum order.
     *
     * @param profile The Spice Profile to format
     * @return One tooltip line per Flavor Axis
     */
    public static List<Component> formatFlavorAxes(SpiceProfile profile) {
        List<FlavorValue> values = new ArrayList<>(FlavorAxis.values().length);
        for (FlavorAxis axis : FlavorAxis.values()) {
            values.add(new FlavorValue(displayName(axis), profile.get(axis)));
        }
        return formatFlavorAxes(values);
    }

    /**
     * Prettifies a {@link FlavorAxis} constant name for display, e.g.
     * {@code SWEET_BITTER} -> {@code "Sweet Bitter"}.
     */
    private static String displayName(FlavorAxis axis) {
        String[] words = axis.name().split("_");
        StringBuilder result = new StringBuilder();
        for (String word : words) {
            if (!result.isEmpty()) {
                result.append(' ');
            }
            result.append(word.substring(0, 1).toUpperCase(Locale.ROOT));
            result.append(word.substring(1).toLowerCase(Locale.ROOT));
        }
        return result.toString();
    }
}
