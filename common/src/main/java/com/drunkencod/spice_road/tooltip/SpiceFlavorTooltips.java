package com.drunkencod.spice_road.tooltip;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;

import java.util.ArrayList;
import java.util.List;
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
     * @param axisLabel Display label of the axis (e.g. {@code "+Spicy / -Cooling"})
     * @param value     Score in {@code [-1, 1]}, matching the eventual
     *                  {@code FlavorAxis}/{@code SpiceProfile} representation
     */
    public record FlavorValue(Component axisLabel, double value) {
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
                .map(v -> Component.empty()
                        .append(v.axisLabel())
                        .append(": " + Math.round(v.value() * 10))
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
        for (FlavorAxis axis : FlavorAxis.values())
            values.add(new FlavorValue(axisLabelShort(axis, profile), profile.get(axis)));
        return formatFlavorAxes(values);
    }

    /**
     * Builds the full bipolar display label for a {@link FlavorAxis} from its
     * translation keys, e.g. {@code SWEET_BITTER} -> {@code "+Sweet / -Bitter"}.
     */
    public static MutableComponent axisLabelFull(FlavorAxis axis) {
        return Component.literal("+")
                .append(Component.translatable(axis.positiveTranslationKey()))
                .append(" / -")
                .append(Component.translatable(axis.negativeTranslationKey()));
    }

    /**
     * Returns the display label of a {@link FlavorAxis} matching the sign of the
     * given spice profile's value on that axis, e.g. {@code SWEET_BITTER} ->
     * {@code "Sweet"}.
     */
    public static Component axisLabelShort(FlavorAxis axis, SpiceProfile profile) {
        return axisLabelShort(axis, profile.get(axis) >= 0D);
    }

    /**
     * Returns the display label of a {@link FlavorAxis} matching the sign given via
     * {@link isPositive}, e.g. {@code SWEET_BITTER} -> {@code "Sweet"}.
     */
    public static Component axisLabelShort(FlavorAxis axis, boolean isPositive) {
        return Component.translatable(isPositive
                ? axis.positiveTranslationKey()
                : axis.negativeTranslationKey());
    }
}
