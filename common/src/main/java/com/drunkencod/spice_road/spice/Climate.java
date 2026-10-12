package com.drunkencod.spice_road.spice;

import java.util.List;

import org.jetbrains.annotations.Nullable;

import com.mojang.serialization.Codec;

import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.biome.Biome;

import com.drunkencod.spice_road.Constants;
import com.drunkencod.spice_road.spice.region.SeededHash;

/**
 * A label a biome can have, deciding which Climate Bucket its Spices come
 * from. Open and datapack-extensible: a Climate is just an ID, and the Climate
 * Buckets themselves (which Spices a Climate holds) are per dimension, see
 * {@link com.drunkencod.spice_road.spice.dimension.SpiceDimension}.
 * <p>
 * A Climate {@code mypack:foo} is forced onto biomes through the biome tag
 * {@code mypack:climate/foo} ({@link #biomeTag()}).
 *
 * @param id The Climate's ID, e.g. {@code spice_road:arid}.
 */
public record Climate(ResourceLocation id) {

    /** Tag path prefix of a Climate's {@link #biomeTag() biome tag}. */
    private static final String TAG_PREFIX = "climate/";

    /** Codec of a Climate's ID. */
    public static final Codec<Climate> CODEC = ResourceLocation.CODEC.xmap(Climate::new, Climate::id);

    public static final Climate TROPICAL = builtIn("tropical");
    public static final Climate TEMPERATE = builtIn("temperate");
    public static final Climate ARID = builtIn("arid");
    public static final Climate COLD = builtIn("cold");
    /** The Nether's only built-in Climate. */
    public static final Climate NETHER = builtIn("nether");
    /** The End's only built-in Climate. */
    public static final Climate END = builtIn("end");

    /** The Climates this mod ships, in a fixed order. */
    public static final List<Climate> BUILT_IN = List.of(TROPICAL, TEMPERATE, ARID, COLD, NETHER, END);

    private static Climate builtIn(String path) {
        return new Climate(ResourceLocation.fromNamespaceAndPath(Constants.MOD_ID, path));
    }

    /**
     * @return The translation key of this Climate's plain name, e.g.
     *         {@code "climate.spice_road.tropical"}. A path's slashes become
     *         dots.
     */
    public String getTranslationKey() {
        return "climate." + id.getNamespace() + "." + id.getPath().replace('/', '.');
    }

    /**
     * @return This Climate's translatable plain name. See
     *         {@link #getTranslationKey()}.
     */
    public Component getDisplayName() {
        return Component.translatable(getTranslationKey());
    }

    /**
     * @return The biome tag that forces biomes into this Climate: this
     *         Climate's ID with {@code climate/} in front of its path.
     */
    public TagKey<Biome> biomeTag() {
        return TagKey.create(Registries.BIOME, id.withPath(TAG_PREFIX + id.getPath()));
    }

    /**
     * @return A stable 64-bit key derived from this Climate's ID, used to seed
     *         its Spice picks. Doesn't depend on registration or load order.
     */
    public long seedKey() {
        return SeededHash.hashString(id.toString());
    }

    /**
     * @return This Climate as typed in a command: just the path for this mod's
     *         own Climates, otherwise the full ID in quotes, since a colon
     *         can't be typed in a plain word.
     */
    public String getCommandName() {
        return id.getNamespace().equals(Constants.MOD_ID) ? id.getPath() : "\"" + id + "\"";
    }

    /**
     * Looks up a Climate by what a command holds.
     *
     * @param name A path of this mod's namespace (e.g. {@code "tropical"}), or
     *             a full ID (e.g. {@code "mypack:infernal"}).
     * @return The Climate, or {@code null} if {@code name} isn't a valid ID.
     *         Whether any dimension knows it is not checked.
     */
    public static @Nullable Climate byCommandName(String name) {
        ResourceLocation id = name.indexOf(':') >= 0 ? ResourceLocation.tryParse(name)
                : ResourceLocation.tryBuild(Constants.MOD_ID, name);
        return id == null ? null : new Climate(id);
    }
}
