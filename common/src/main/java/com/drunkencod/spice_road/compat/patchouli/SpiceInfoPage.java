package com.drunkencod.spice_road.compat.patchouli;

import java.util.List;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;

import com.drunkencod.spice_road.compat.viewer.SpiceOriginEntry;
import com.drunkencod.spice_road.compat.viewer.ViewerLayout;
import com.drunkencod.spice_road.spice.Spice;
import com.drunkencod.spice_road.stats.ClientSpiceFindings;

/**
 * Page type {@code spice_road:spice_info}: a Spice's planting item growing
 * into its raw item, with its tier, climate, yield, plant and harvest, as the
 * Spice Origin viewer category shows it, plus whether the reader has found it.
 * <p>
 * JSON: {@code "spice": "<spice id>"}, e.g. {@code "cinnamon"}.
 */
public class SpiceInfoPage extends ViewerEntryPage {

    private String spice;

    private transient Spice resolved;

    @Override
    protected List<ViewerLayout> layouts(Level level, int width) {
        resolved = spice == null ? null : Spice.byId(spice);
        return resolved == null ? List.of() : layoutAll(List.of(new SpiceOriginEntry(resolved)), width);
    }

    @Override
    protected Component missingText() {
        return Component.translatable("spice_road.guide.unknown_spice", String.valueOf(spice));
    }

    @Override
    protected Component defaultTitle() {
        Item raw = resolved == null ? null : Spice.getRawById(resolved.getId());
        return raw == null ? Component.empty() : raw.getDefaultInstance().getHoverName();
    }

    @Override
    protected List<Component> footer() {
        boolean found = resolved != null && ClientSpiceFindings.isFound(resolved);
        return List.of(found
                ? Component.translatable("spice_road.guide.found").withStyle(ChatFormatting.DARK_GREEN)
                : Component.translatable("spice_road.guide.not_found").withStyle(ChatFormatting.DARK_GRAY));
    }
}
