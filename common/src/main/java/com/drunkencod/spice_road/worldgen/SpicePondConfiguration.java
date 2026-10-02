package com.drunkencod.spice_road.worldgen;

import java.util.List;
import java.util.Optional;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.util.valueproviders.IntProvider;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.levelgen.feature.configurations.FeatureConfiguration;
import net.minecraft.world.level.levelgen.feature.stateproviders.BlockStateProvider;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;

import com.drunkencod.spice_road.Constants;

/**
 * Configuration of a {@link SpicePondFeature} Spice Pond, read from its
 * {@code configured_feature} JSON. Splotchy floors and shores come from
 * noise-based block state providers (e.g.
 * {@code minecraft:noise_threshold_provider}).
 *
 * @param radius      Radius of the water surface, in blocks, before its edge
 *                    is roughened.
 * @param floor       Block laid under the water, and wherever the pond needs
 *                    a bank to hold its water in.
 * @param shore       Block the ground around the pond is replaced with, if
 *                    any.
 * @param shoreWidth  Width of the {@code shore} ring, in blocks.
 * @param maxSlope    Largest height difference, in blocks, between the pond's
 *                    water level and any ground it covers. Steeper spots
 *                    don't get a pond at all.
 * @param replaceable Ground the {@code shore} may replace.
 * @param decorations Features generated around the pond, e.g. trees, single
 *                    Spice Plants or small Spice patches.
 * @param waterDecorations Features generated on the pond's water surface,
 *                    e.g. lily pads. Placed one block above the water.
 * @param noWaterDecorationBiomes Biomes the pond's origin must not be in for
 *                    its {@code waterDecorations} to generate.
 */
public record SpicePondConfiguration(IntProvider radius, BlockStateProvider floor, Optional<BlockStateProvider> shore,
        int shoreWidth, int maxSlope, TagKey<Block> replaceable, List<Decoration> decorations,
        List<Decoration> waterDecorations, TagKey<Biome> noWaterDecorationBiomes)
        implements FeatureConfiguration {

    /** Default {@link #maxSlope()}, in blocks. */
    public static final int DEFAULT_MAX_SLOPE = 4;

    /** Largest accepted {@link #maxSlope()}. */
    public static final int MAX_SLOPE = 6;

    /**
     * Largest accepted {@link #radius()}. A pond is shrunk further if needed
     * to fit the area worldgen may write to around its origin.
     */
    public static final int MAX_RADIUS = 8;

    /**
     * Default {@link #replaceable()} ground: natural surface blocks a pond's shore
     * may paint over.
     */
    public static final TagKey<Block> SHORE_REPLACEABLE = TagKey.create(Registries.BLOCK,
            ResourceLocation.fromNamespaceAndPath(Constants.MOD_ID, "pond_shore_replaceable"));

    /**
     * Default {@link #noWaterDecorationBiomes()}: arid and cold biomes, where
     * water plants don't belong.
     */
    public static final TagKey<Biome> NO_WATER_DECORATIONS = TagKey.create(Registries.BIOME,
            ResourceLocation.fromNamespaceAndPath(Constants.MOD_ID, "no_pond_water_vegetation"));

    /** Largest accepted {@link #shoreWidth()}. */
    public static final int MAX_SHORE_WIDTH = 3;

    /** Codec of this configuration's JSON fields. */
    public static final Codec<SpicePondConfiguration> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            IntProvider.codec(1, MAX_RADIUS).fieldOf("radius").forGetter(SpicePondConfiguration::radius),
            BlockStateProvider.CODEC.fieldOf("floor").forGetter(SpicePondConfiguration::floor),
            BlockStateProvider.CODEC.optionalFieldOf("shore").forGetter(SpicePondConfiguration::shore),
            Codec.intRange(0, MAX_SHORE_WIDTH).optionalFieldOf("shore_width", 1)
                    .forGetter(SpicePondConfiguration::shoreWidth),
            Codec.intRange(0, MAX_SLOPE).optionalFieldOf("max_slope", DEFAULT_MAX_SLOPE)
                    .forGetter(SpicePondConfiguration::maxSlope),
            TagKey.hashedCodec(Registries.BLOCK).optionalFieldOf("replaceable", SHORE_REPLACEABLE)
                    .forGetter(SpicePondConfiguration::replaceable),
            Decoration.CODEC.listOf().optionalFieldOf("decorations", List.of())
                    .forGetter(SpicePondConfiguration::decorations),
            Decoration.CODEC.listOf().optionalFieldOf("water_decorations", List.of())
                    .forGetter(SpicePondConfiguration::waterDecorations),
            TagKey.hashedCodec(Registries.BIOME).optionalFieldOf("no_water_decoration_biomes", NO_WATER_DECORATIONS)
                    .forGetter(SpicePondConfiguration::noWaterDecorationBiomes))
            .apply(instance, SpicePondConfiguration::new));

    /**
     * A feature generated around the pond, placed at random surface positions
     * just outside its water, then run through its own placement modifiers.
     *
     * @param feature The placed feature to generate.
     * @param count   How many times to generate it.
     */
    public record Decoration(Holder<PlacedFeature> feature, IntProvider count) {

        /** Codec of this decoration's JSON fields. */
        public static final Codec<Decoration> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                PlacedFeature.CODEC.fieldOf("feature").forGetter(Decoration::feature),
                IntProvider.codec(0, 16).fieldOf("count").forGetter(Decoration::count))
                .apply(instance, Decoration::new));
    }
}
