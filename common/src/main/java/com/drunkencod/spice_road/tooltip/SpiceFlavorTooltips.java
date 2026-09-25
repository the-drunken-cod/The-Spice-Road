package com.drunkencod.spice_road.tooltip;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Formats a Spice's Flavor Axis scores as tooltip lines, shown while Shift is
 * held.
 * <p>
 * TODO: once {@code com.drunkencod.spice_road.spice.FlavorAxis} and
 * {@code com.drunkencod.spice_road.spice.SpiceProfile} land, add an overload
 * of {@link #formatFlavorAxes} that takes a {@code SpiceProfile} directly
 * (iterating its 8 {@code FlavorAxis} entries) instead of a
 * {@code List<FlavorValue>}, and have raw/dried Spice items call
 * {@link TooltipUtil#register} with that overload as their content provider.
 * No change to {@link TooltipUtil} itself is needed for that follow-up.
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
}
