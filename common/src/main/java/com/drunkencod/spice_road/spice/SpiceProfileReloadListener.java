package com.drunkencod.spice_road.spice;

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
 * Loads {@code data/<namespace>/spice_profile/*.json} files, each giving
 * one or more items (by ID or tag) a Default Profile and thereby making them
 * Spice Items. This is what lets any item, vanilla, modded, or added by this
 * mod, become a spice without a compile-time dependency on the {@link Spice}
 * enum.
 * <p>
 * A malformed entry is logged and skipped rather than failing the whole
 * reload, so one bad datapack file can't break every other spice.
 * <p>
 * Hands the entries to {@link SpiceProfileRegistry#setPending} on every
 * reload; they're resolved once item tags are bound. Registered on both
 * loaders via {@code IRegistryHelper#registerReloadListener}.
 */
public class SpiceProfileReloadListener extends SimpleJsonResourceReloadListener {

    /** Datapack directory the Spice Profile files are loaded from. */
    public static final String DIRECTORY = "spice_profile";
    /** ID of this reload listener. */
    public static final ResourceLocation ID = ResourceLocation.fromNamespaceAndPath(Constants.MOD_ID, DIRECTORY);

    /** Axis magnitude a single Spice Item's Default Profile conventionally stays within. */
    private static final double CONVENTIONAL_AXIS_RANGE = 1D;

    /** Creates the listener for the {@link #DIRECTORY} directory. */
    public SpiceProfileReloadListener() {
        super(new Gson(), DIRECTORY);
    }

    @Override
    protected void apply(Map<ResourceLocation, JsonElement> object, ResourceManager resourceManager,
            ProfilerFiller profiler) {
        Map<ResourceLocation, SpiceProfileEntry> entries = new HashMap<>(object.size());
        object.forEach((id, json) -> SpiceProfileEntry.CODEC.parse(JsonOps.INSTANCE, json)
                .resultOrPartial(error -> Constants.LOG.error("Couldn't load Spice Profile {}: {}", id, error))
                .ifPresent(entry -> {
                    if (entry.profile().maxMagnitude() > CONVENTIONAL_AXIS_RANGE)
                        Constants.LOG.warn("Spice Profile {} has flavor axis values beyond +-{}, which is "
                                + "stronger than a single spice is meant to be", id, CONVENTIONAL_AXIS_RANGE);
                    entries.put(id, entry);
                }));
        SpiceProfileRegistry.setPending(entries);
    }
}
