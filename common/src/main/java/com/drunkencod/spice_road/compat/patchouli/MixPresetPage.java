package com.drunkencod.spice_road.compat.patchouli;

import java.util.List;

import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;

import com.drunkencod.spice_road.compat.viewer.SpiceProfileEntry;
import com.drunkencod.spice_road.compat.viewer.ViewerLayout;
import com.drunkencod.spice_road.mix.MixPreset;
import com.drunkencod.spice_road.mix.MixPresetRegistry;
import com.drunkencod.spice_road.mix.SpiceMixes;

/**
 * Page type {@code spice_road:mix_preset}: a Mix Preset's spices in their
 * proportions, the jar they make and its Effective Profile, as in the Spice
 * Profile viewer category.
 * <p>
 * JSON: {@code "preset": "<preset id>"}, e.g. {@code "spice_road:pumpkin_spice"}.
 * Datapack-added presets work too.
 */
public class MixPresetPage extends ViewerEntryPage {

    private String preset;

    @Override
    protected List<ViewerLayout> layouts(Level level, int width) {
        ResourceLocation id = preset == null ? null : ResourceLocation.tryParse(preset);
        MixPreset found = id == null ? null : MixPresetRegistry.getAll().get(id);
        if (found == null)
            return List.of();
        return SpiceMixes.ofPreset(id, found)
                .map(jar -> layoutAll(List.of(new SpiceProfileEntry.OfPreset(id, found, jar)), width))
                .orElse(List.of());
    }

    @Override
    protected Component missingText() {
        return Component.translatable("spice_road.guide.unknown_preset", String.valueOf(preset));
    }
}
