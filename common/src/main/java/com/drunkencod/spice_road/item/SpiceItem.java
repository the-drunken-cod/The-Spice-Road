package com.drunkencod.spice_road.item;

import java.util.List;

import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

import com.drunkencod.spice_road.spice.Spice;
import com.drunkencod.spice_road.spice.SpiceProfile;
import com.drunkencod.spice_road.tooltip.SpiceFlavorTooltips;
import com.drunkencod.spice_road.tooltip.TooltipUtil;

/**
 * Base item for a {@link Spice}'s raw/dried forms. Registers a shift-only
 * tooltip (via {@link TooltipUtil}) showing the Spice's 8 Flavor Axis
 * values on the -100..100 scale (see {@link SpiceFlavorTooltips}).
 * <p>
 * Referencing {@code net.minecraft.client.gui.screens.Screen} here (a
 * client-only class) from a common-module item is deliberate and safe:
 * {@link #appendHoverText} is only ever invoked by client-side tooltip
 * rendering, never on a dedicated server, so {@code Screen} is never
 * resolved/loaded there - this is the exact usage {@link TooltipUtil}'s own
 * javadoc documents.
 */
public abstract class SpiceItem extends Item {

    private final Spice spice;

    protected SpiceItem(Properties properties, Spice spice) {
        super(properties);
        this.spice = spice;
        TooltipUtil.register(this, TooltipUtil.Visibility.SHIFT_ONLY,
                stack -> SpiceFlavorTooltips.formatFlavorAxes(getProfile()));
    }

    public Spice getSpice() {
        return spice;
    }

    /**
     * @return The {@link SpiceProfile} this item's form (raw or dried)
     *         should show in its tooltip.
     */
    protected abstract SpiceProfile getProfile();

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltipComponents,
            TooltipFlag tooltipFlag) {
        super.appendHoverText(stack, context, tooltipComponents, tooltipFlag);
        TooltipUtil.append(stack, tooltipComponents, Screen.hasShiftDown());
    }
}
