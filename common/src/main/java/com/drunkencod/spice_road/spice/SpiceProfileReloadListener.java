package com.drunkencod.spice_road.spice;

import java.util.ArrayList;
import java.util.List;
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
 * Loads {@code data/<namespace>/spice_profile/*.json} files, each
 * registering a single item as a Spice Road compatible spice with a
 * datapack-defined {@link SpiceProfile}. This is what lets any item,
 * vanilla, modded, or added by this mod, become a spice without a
 * compile-time dependency on the {@link Spice} enum.
 * <p>
 * A malformed entry is logged and skipped rather than failing the whole
 * reload, so one bad datapack file can't break every other spice.
 * <p>
 * Populates {@link SpiceProfileRegistry} on every reload. Registered on both
 * loaders via {@code IRegistryHelper#registerReloadListener}.
 */
public class SpiceProfileReloadListener extends SimpleJsonResourceReloadListener {

    public static final String DIRECTORY = "spice_profile";
    public static final ResourceLocation ID = ResourceLocation.fromNamespaceAndPath(Constants.MOD_ID, DIRECTORY);

    public SpiceProfileReloadListener() {
        super(new Gson(), DIRECTORY);
    }

    @Override
    protected void apply(Map<ResourceLocation, JsonElement> object, ResourceManager resourceManager,
            ProfilerFiller profiler) {

        List<SpiceProfileEntry> entries = new ArrayList<>(object.size());
        object.forEach((id, json) -> SpiceProfileEntry.CODEC.parse(JsonOps.INSTANCE, json)
                .resultOrPartial(error -> Constants.LOG.error("Couldn't load Spice Profile {}: {}", id, error))
                .ifPresent(entries::add));
        SpiceProfileRegistry.setAll(entries);
    }
}
