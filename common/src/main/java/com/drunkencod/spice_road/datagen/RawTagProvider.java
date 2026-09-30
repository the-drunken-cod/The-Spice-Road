package com.drunkencod.spice_road.datagen;

import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Stream;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import net.minecraft.core.Registry;
import net.minecraft.data.CachedOutput;
import net.minecraft.data.DataProvider;
import net.minecraft.data.PackOutput;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;

/**
 * Base of the tag providers that write raw tag JSON, so they run unchanged on
 * both loaders instead of going through each loader's own tag provider API.
 *
 * @param <T> The tagged registry's element type, e.g. {@code Item}.
 */
public abstract class RawTagProvider<T> implements DataProvider {

    private final PackOutput.PathProvider tagPathProvider;
    private final Registry<T> registry;

    /**
     * @param output    The pack output to write into.
     * @param tagFolder The tag folder within the data pack, e.g.
     *                  {@code "tags/item"}.
     * @param registry  The registry the tagged elements come from.
     */
    protected RawTagProvider(PackOutput output, String tagFolder, Registry<T> registry) {
        this.tagPathProvider = output.createPathProvider(PackOutput.Target.DATA_PACK, tagFolder);
        this.registry = registry;
    }

    /** One {@code values} entry of a tag: a required reference, or an {@link #optional} one that may not exist. */
    public record TagValue(String id, boolean required) {

        /** @param id A required element ID or {@code #}-prefixed tag reference. */
        public static TagValue of(String id) {
            return new TagValue(id, true);
        }

        /** @param id A reference tolerated as missing, e.g. another mod's convention tag that may not be loaded. */
        public static TagValue optional(String id) {
            return new TagValue(id, false);
        }

        /** @param tag A tag to reference, required. */
        public static TagValue tag(TagKey<?> tag) {
            return of("#" + tag.location());
        }
    }

    /**
     * @param elements The elements to list, with {@code null}s skipped.
     * @return A required entry per element, in order.
     */
    protected List<TagValue> entries(Stream<? extends T> elements) {
        return elements.filter(Objects::nonNull)
                .map(element -> TagValue.of(Objects.requireNonNull(registry.getKey(element)).toString()))
                .toList();
    }

    /**
     * Writes one tag file, never replacing what other packs add to it.
     *
     * @param tag    The tag's ID.
     * @param values The tag's entries, in order.
     * @return The pending write.
     */
    protected CompletableFuture<?> save(CachedOutput cachedOutput, ResourceLocation tag, List<TagValue> values) {
        JsonObject json = new JsonObject();
        json.addProperty("replace", false);
        JsonArray valuesJson = new JsonArray();
        for (TagValue value : values) {
            if (value.required()) {
                valuesJson.add(value.id());
                continue;
            }
            JsonObject entry = new JsonObject();
            entry.addProperty("id", value.id());
            entry.addProperty("required", false);
            valuesJson.add(entry);
        }
        json.add("values", valuesJson);
        return DataProvider.saveStable(cachedOutput, json, tagPathProvider.json(tag));
    }

    /** {@link #save(CachedOutput, ResourceLocation, List)} for a {@link TagKey}. */
    protected CompletableFuture<?> save(CachedOutput cachedOutput, TagKey<T> tag, List<TagValue> values) {
        return save(cachedOutput, tag.location(), values);
    }
}
