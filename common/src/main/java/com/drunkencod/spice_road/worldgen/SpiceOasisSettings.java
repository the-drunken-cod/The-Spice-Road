package com.drunkencod.spice_road.worldgen;

import java.util.List;
import java.util.Optional;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.levelgen.feature.ConfiguredFeature;

/**
 * When and how a {@link SpicePlantFeature} patch carves an oasis: a
 * {@link SpicePondFeature} whose shore is spice-growable, generated before any
 * plant wherever too little of the patch's dry ground could grow a
 * non-aquatic Spice (e.g. deserts, badlands, stony mountains). Which pond is
 * used depends on the biome, so each oasis can bring its own floor, shore and
 * foliage.
 *
 * @param minGrowableGround Share of the patch's dry surface columns, from
 *                          {@code 0} to {@code 1}, that must be spice-growable
 *                          for the patch to generate without an oasis.
 * @param rules             Biome-specific ponds. The first rule whose biome
 *                          tag contains the patch's biome wins.
 * @param fallback          Pond for biomes no rule matches, or none to leave
 *                          such patches without an oasis.
 */
public record SpiceOasisSettings(float minGrowableGround, List<Rule> rules,
        Optional<Holder<ConfiguredFeature<?, ?>>> fallback) {

    /** Codec of these settings' JSON fields. */
    public static final Codec<SpiceOasisSettings> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.floatRange(0.0F, 1.0F).optionalFieldOf("min_growable_ground", 0.5F)
                    .forGetter(SpiceOasisSettings::minGrowableGround),
            Rule.CODEC.listOf().optionalFieldOf("rules", List.of()).forGetter(SpiceOasisSettings::rules),
            ConfiguredFeature.CODEC.optionalFieldOf("default").forGetter(SpiceOasisSettings::fallback))
            .apply(instance, SpiceOasisSettings::new));

    /**
     * @param biome The biome at the patch's origin.
     * @return The pond to carve as an oasis in {@code biome}, if any.
     */
    public Optional<Holder<ConfiguredFeature<?, ?>>> pondFor(Holder<Biome> biome) {
        for (Rule rule : rules) {
            if (biome.is(rule.biomes()))
                return Optional.of(rule.pond());
        }
        return fallback;
    }

    /**
     * A biome-specific oasis pond.
     *
     * @param biomes Biome tag this rule applies to.
     * @param pond   Pond carved in those biomes, typically a
     *               {@link SpicePondFeature}.
     */
    public record Rule(TagKey<Biome> biomes, Holder<ConfiguredFeature<?, ?>> pond) {

        /** Codec of this rule's JSON fields. */
        public static final Codec<Rule> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                TagKey.hashedCodec(Registries.BIOME).fieldOf("biomes").forGetter(Rule::biomes),
                ConfiguredFeature.CODEC.fieldOf("pond").forGetter(Rule::pond))
                .apply(instance, Rule::new));
    }
}
