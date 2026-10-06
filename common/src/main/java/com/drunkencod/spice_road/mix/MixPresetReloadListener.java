package com.drunkencod.spice_road.mix;

import java.util.HashMap;
import java.util.Map;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.mojang.serialization.JsonOps;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;

import com.drunkencod.spice_road.Constants;

/**
 * Loads {@code data/<namespace>/mix_preset/*.json} into the
 * {@link MixPresetRegistry}. A malformed preset (or one naming an item that
 * doesn't exist, e.g. from a mod that isn't installed) is logged and skipped
 * rather than failing the whole reload.
 */
public class MixPresetReloadListener extends SimpleJsonResourceReloadListener {

    /** Datapack directory the preset files are loaded from. */
    public static final String DIRECTORY = "mix_preset";
    /** ID of this reload listener. */
    public static final ResourceLocation ID = ResourceLocation.fromNamespaceAndPath(Constants.MOD_ID, DIRECTORY);

    /** Creates the listener for the {@link #DIRECTORY} directory. */
    public MixPresetReloadListener() {
        super(new Gson(), DIRECTORY);
    }

    @Override
    protected void apply(Map<ResourceLocation, JsonElement> object, ResourceManager resourceManager,
            ProfilerFiller profiler) {
        Map<ResourceLocation, MixPreset> presets = new HashMap<>(object.size());
        object.forEach((id, json) -> MixPreset.CODEC.parse(JsonOps.INSTANCE, json)
                .resultOrPartial(error -> Constants.LOG.error("Couldn't load Mix Preset {}: {}", id, error))
                .ifPresent(preset -> presets.put(id, preset)));
        MixPresetRegistry.set(presets);
        Constants.LOG.info("Loaded {} Mix Presets", presets.size());
    }
}
