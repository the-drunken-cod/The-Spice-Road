package com.drunkencod.spice_road.spice.dimension;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.Reader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.JsonOps;

import net.minecraft.resources.FileToIdConverter;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimplePreparableReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;

import com.drunkencod.spice_road.Constants;
import com.drunkencod.spice_road.spice.region.RegionHeartSearch;

/**
 * Loads {@code data/<namespace>/spice_dimension/<dimension_path>.json} into
 * {@link SpiceDimensions}. Unlike most listeners, it reads every datapack's
 * copy of a file, not only the top one, so packs can add to a dimension
 * instead of replacing it (see {@link SpiceDimension#merge}). A malformed file
 * is logged and skipped, leaving the others of that dimension in place.
 */
public class SpiceDimensionReloadListener extends SimplePreparableReloadListener<Map<ResourceLocation, List<SpiceDimensionLayer>>> {

    /** Datapack directory the files are loaded from. */
    public static final String DIRECTORY = "spice_dimension";
    /** ID of this reload listener. */
    public static final ResourceLocation ID = ResourceLocation.fromNamespaceAndPath(Constants.MOD_ID, DIRECTORY);

    @Override
    protected Map<ResourceLocation, List<SpiceDimensionLayer>> prepare(ResourceManager resourceManager,
            ProfilerFiller profiler) {
        FileToIdConverter converter = FileToIdConverter.json(DIRECTORY);
        Map<ResourceLocation, List<SpiceDimensionLayer>> layers = new HashMap<>();
        // Each file's copies come lowest-priority first, like tags
        converter.listMatchingResourceStacks(resourceManager).forEach((file, resources) -> {
            ResourceLocation id = converter.fileToId(file);
            List<SpiceDimensionLayer> stack = new ArrayList<>(resources.size());
            for (Resource resource : resources)
                readLayer(id, resource).ifPresent(stack::add);
            if (!stack.isEmpty())
                layers.put(id, stack);
        });
        return layers;
    }

    @Override
    protected void apply(Map<ResourceLocation, List<SpiceDimensionLayer>> layers, ResourceManager resourceManager,
            ProfilerFiller profiler) {
        Map<ResourceLocation, SpiceDimension> dimensions = new HashMap<>(layers.size());
        layers.forEach((id, stack) -> dimensions.put(id, SpiceDimension.merge(id, stack)));
        SpiceDimensions.set(dimensions);
        RegionHeartSearch.clearCaches();
        Constants.LOG.info("Loaded {} Spice Dimensions", dimensions.size());
    }

    private static Optional<SpiceDimensionLayer> readLayer(ResourceLocation id, Resource resource) {
        try (BufferedReader reader = resource.openAsReader()) {
            return parse(id, resource.sourcePackId(), reader);
        } catch (IOException | RuntimeException e) {
            Constants.LOG.error("Couldn't read Spice Dimension {} from data pack {}", id, resource.sourcePackId(), e);
            return Optional.empty();
        }
    }

    private static Optional<SpiceDimensionLayer> parse(ResourceLocation id, String pack, Reader reader) {
        JsonElement json = JsonParser.parseReader(reader);
        DataResult<SpiceDimensionLayer> result = SpiceDimensionLayer.CODEC.parse(JsonOps.INSTANCE, json);
        result.ifError(error -> Constants.LOG.error("Couldn't load Spice Dimension {} from data pack {}: {}", id, pack,
                error.message()));
        return result.result();
    }
}