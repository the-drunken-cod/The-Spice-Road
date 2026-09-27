package com.drunkencod.spice_road.worldgen;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.util.ExtraCodecs;
import net.minecraft.world.level.levelgen.feature.configurations.FeatureConfiguration;

/**
 * Configuration of a {@link SpicePlantFeature} patch, read from its
 * {@code configured_feature} JSON.
 *
 * @param tries          Placement attempts, spread around the origin.
 * @param xzSpread       Maximum horizontal offset of an attempt from the
 *                       origin, in blocks.
 * @param maxTrees       Maximum trees placed, if the patch's Spice grows as a
 *                       tree. Every attempt still counts towards
 *                       {@code tries}.
 * @param heartSpiceOnly Whether every plant is the Heart Spice of the Spice
 *                       Region the origin lies in (a Heart Grove), instead of
 *                       resolving the Spice at each attempt's own position.
 */
public record SpicePlantConfiguration(int tries, int xzSpread, int maxTrees, boolean heartSpiceOnly)
        implements FeatureConfiguration {

    /** Codec of this configuration's JSON fields. */
    public static final Codec<SpicePlantConfiguration> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            ExtraCodecs.POSITIVE_INT.fieldOf("tries").forGetter(SpicePlantConfiguration::tries),
            Codec.intRange(0, 16).fieldOf("xz_spread").forGetter(SpicePlantConfiguration::xzSpread),
            Codec.intRange(0, 64).optionalFieldOf("max_trees", 1).forGetter(SpicePlantConfiguration::maxTrees),
            Codec.BOOL.optionalFieldOf("heart_spice_only", false)
                    .forGetter(SpicePlantConfiguration::heartSpiceOnly))
            .apply(instance, SpicePlantConfiguration::new));
}
