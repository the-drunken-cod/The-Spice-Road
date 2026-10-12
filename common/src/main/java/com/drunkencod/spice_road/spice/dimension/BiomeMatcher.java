package com.drunkencod.spice_road.spice.dimension;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;

import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.biome.Biome;

/**
 * Matches a biome by ID or, with a leading {@code #}, by biome tag. Kept as
 * plain IDs and only looked up when matching, since the datapack listeners
 * that read these have no registry access, and the tag or biome may belong to
 * a mod that isn't installed.
 *
 * @param id    The biome's or tag's ID.
 * @param isTag Whether {@code id} is a biome tag.
 */
public record BiomeMatcher(ResourceLocation id, boolean isTag) {

    /** Codec of a matcher as a string, e.g. {@code "minecraft:plains"} or {@code "#minecraft:is_nether"}. */
    public static final Codec<BiomeMatcher> CODEC = Codec.STRING.comapFlatMap(BiomeMatcher::parse,
            BiomeMatcher::toString);

    private static DataResult<BiomeMatcher> parse(String text) {
        boolean tag = text.startsWith("#");
        ResourceLocation id = ResourceLocation.tryParse(tag ? text.substring(1) : text);
        return id == null ? DataResult.error(() -> "Not a biome or biome tag ID: " + text)
                : DataResult.success(new BiomeMatcher(id, tag));
    }

    /**
     * @param biome The biome to test.
     * @return Whether {@code biome} is the matched biome, or in the matched tag.
     */
    public boolean matches(Holder<Biome> biome) {
        return isTag ? biome.is(TagKey.create(Registries.BIOME, id)) : biome.is(id);
    }

    @Override
    public String toString() {
        return (isTag ? "#" : "") + id;
    }
}
