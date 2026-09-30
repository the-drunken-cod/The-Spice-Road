package com.drunkencod.spice_road.worldgen;

import java.util.Optional;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.core.Holder;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.world.level.levelgen.feature.ConfiguredFeature;
import net.minecraft.world.level.levelgen.feature.configurations.FeatureConfiguration;

/**
 * Configuration of a {@link SpicePlantFeature} patch, read from its
 * {@code configured_feature} JSON.
 *
 * @param tries          Placement attempts, spread around the origin.
 * @param xzSpread       Maximum horizontal offset of an attempt from the
 *                       origin, in blocks.
 * @param trees          How many trees to place, and how far apart, if the
 *                       patch's Spice grows as a tree. Every attempt still
 *                       counts towards {@code tries}.
 * @param heartSpiceOnly Whether every plant is the Heart Spice of the Spice
 *                       Region the origin lies in (a Heart Grove), instead of
 *                       resolving the Spice at each attempt's own position.
 * @param aquaticPond    Feature generated at the origin before any plant if
 *                       the origin's Spice is an Aquatic Spice - typically a
 *                       {@link SpicePondFeature}, giving its plants water to
 *                       grow in.
 * @param oasis          When and which oasis to carve at the origin before
 *                       any plant if the origin's Spice isn't an Aquatic
 *                       Spice, giving its plants ground to grow on. None for
 *                       patches that should simply skip unsuitable ground.
 */
public record SpicePlantConfiguration(int tries, int xzSpread, SpicePlantTreeSettings trees, boolean heartSpiceOnly,
        Optional<Holder<ConfiguredFeature<?, ?>>> aquaticPond, Optional<SpiceOasisSettings> oasis)
        implements FeatureConfiguration {

    /** Codec of this configuration's JSON fields. */
    public static final Codec<SpicePlantConfiguration> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            ExtraCodecs.POSITIVE_INT.fieldOf("tries").forGetter(SpicePlantConfiguration::tries),
            Codec.intRange(0, 16).fieldOf("xz_spread").forGetter(SpicePlantConfiguration::xzSpread),
            SpicePlantTreeSettings.CODEC.optionalFieldOf("trees", SpicePlantTreeSettings.SINGLE)
                    .forGetter(SpicePlantConfiguration::trees),
            Codec.BOOL.optionalFieldOf("heart_spice_only", false)
                    .forGetter(SpicePlantConfiguration::heartSpiceOnly),
            ConfiguredFeature.CODEC.optionalFieldOf("aquatic_pond")
                    .forGetter(SpicePlantConfiguration::aquaticPond),
            SpiceOasisSettings.CODEC.optionalFieldOf("oasis")
                    .forGetter(SpicePlantConfiguration::oasis))
            .apply(instance, SpicePlantConfiguration::new));
}
