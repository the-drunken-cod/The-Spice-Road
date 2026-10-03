package com.drunkencod.spice_road.spice.effect;

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
 * Loads {@code data/<namespace>/seasoning_effect/*.json} into the
 * {@link SeasoningEffectRegistry}. A malformed entry is logged and skipped
 * rather than failing the whole reload. Registered on both loaders via
 * {@code IRegistryHelper#registerReloadListener}.
 */
public class SeasoningEffectReloadListener extends SimpleJsonResourceReloadListener {

    /** Datapack directory the catalog files are loaded from. */
    public static final String DIRECTORY = "seasoning_effect";
    /** ID of this reload listener. */
    public static final ResourceLocation ID = ResourceLocation.fromNamespaceAndPath(Constants.MOD_ID, DIRECTORY);

    /** Creates the listener for the {@link #DIRECTORY} directory. */
    public SeasoningEffectReloadListener() {
        super(new Gson(), DIRECTORY);
    }

    @Override
    protected void apply(Map<ResourceLocation, JsonElement> object, ResourceManager resourceManager,
            ProfilerFiller profiler) {
        Map<ResourceLocation, SeasoningEffectDef> entries = new HashMap<>(object.size());
        object.forEach((id, json) -> SeasoningEffectDef.CODEC.parse(JsonOps.INSTANCE, json)
                .resultOrPartial(error -> Constants.LOG.error("Couldn't load Seasoning Effect {}: {}", id, error))
                .ifPresent(entry -> entries.put(id, entry)));
        SeasoningEffectRegistry.set(entries);
        Constants.LOG.info("Loaded {} Seasoning Effects", entries.size());
    }
}
