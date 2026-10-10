package com.drunkencod.spice_road.compat.patchouli;

import java.util.List;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;

import com.drunkencod.spice_road.compat.viewer.SpiceProfileEntry;
import com.drunkencod.spice_road.compat.viewer.ViewerLayout;
import com.drunkencod.spice_road.spice.SpiceProfile;
import com.drunkencod.spice_road.spice.SpiceProfileRegistry;

/**
 * Page type {@code spice_road:spice_profile}: a Spice Item's Effective
 * Profile as bars per Flavor Axis, as on its tooltip and in the Spice Profile
 * viewer category.
 * <p>
 * JSON: {@code "item": "<item id>"}, e.g. {@code "spice_road:cinnamon"}. Any
 * item with a Default Profile works, including datapack-added ones.
 */
public class SpiceProfilePage extends ViewerEntryPage {

    private String item;

    @Override
    protected List<ViewerLayout> layouts(Level level, int width) {
        ResourceLocation id = item == null ? null : ResourceLocation.tryParse(item);
        if (id == null)
            return List.of();
        Item resolved = BuiltInRegistries.ITEM.get(id);
        SpiceProfile profile = SpiceProfileRegistry.getAll().get(resolved);
        return profile == null ? List.of()
                : layoutAll(List.of(new SpiceProfileEntry.OfItem(resolved, profile)), width);
    }

    @Override
    protected Component missingText() {
        return Component.translatable("spice_road.guide.unknown_profile", String.valueOf(item));
    }
}
