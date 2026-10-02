package com.drunkencod.spice_road.worldgen;

import java.util.List;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.tags.TagKey;
import net.minecraft.util.RandomSource;
import net.minecraft.util.valueproviders.ConstantInt;
import net.minecraft.util.valueproviders.IntProvider;
import net.minecraft.world.level.biome.Biome;

/**
 * How many trees a {@link SpicePlantFeature} patch places if its Spice grows
 * as a tree, and how far apart they stand. The count depends on the biome, so
 * groves in sparse biomes like plains or savannas don't turn into small
 * forests.
 *
 * @param fallback     Tree count for biomes no rule matches.
 * @param rules        Biome-specific tree counts. The first rule whose biome
 *                     tag
 *                     contains the patch's biome wins.
 * @param spacing      Minimum horizontal distance between two trunks, in
 *                     blocks.
 * @param groundRadius Radius, in blocks, of the square of dry columns that has
 *                     to surround a trunk, so trees don't grow on lone blocks
 *                     sticking out of water. {@code 0} only checks the trunk's
 *                     own column.
 */
public record SpicePlantTreeSettings(IntProvider fallback, List<Rule> rules, int spacing, int groundRadius) {

    /** Default for {@link #groundRadius()}. */
    public static final int DEFAULT_GROUND_RADIUS = 2;

    /** A single tree, no spacing - the default for scattered patches. */
    public static final SpicePlantTreeSettings SINGLE = new SpicePlantTreeSettings(ConstantInt.of(1), List.of(), 0,
            DEFAULT_GROUND_RADIUS);

    /** Codec of these settings' JSON fields. */
    public static final Codec<SpicePlantTreeSettings> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            IntProvider.codec(0, 64).fieldOf("default").forGetter(SpicePlantTreeSettings::fallback),
            Rule.CODEC.listOf().optionalFieldOf("rules", List.of()).forGetter(SpicePlantTreeSettings::rules),
            Codec.intRange(0, 16).optionalFieldOf("spacing", 0).forGetter(SpicePlantTreeSettings::spacing),
            Codec.intRange(0, 8).optionalFieldOf("ground_radius", DEFAULT_GROUND_RADIUS)
                    .forGetter(SpicePlantTreeSettings::groundRadius))
            .apply(instance, SpicePlantTreeSettings::new));

    /**
     * Rolls the tree count for a patch in {@code biome}.
     *
     * @param biome  The biome at the patch's origin.
     * @param random Rolls the count.
     * @return The number of trees to place at most.
     */
    public int sampleCount(Holder<Biome> biome, RandomSource random) {
        for (Rule rule : rules) {
            if (biome.is(rule.biomes()))
                return rule.count().sample(random);
        }
        return fallback.sample(random);
    }

    /**
     * A biome-specific tree count.
     *
     * @param biomes Biome tag this rule applies to.
     * @param count  Tree count for those biomes.
     */
    public record Rule(TagKey<Biome> biomes, IntProvider count) {

        /** Codec of this rule's JSON fields. */
        public static final Codec<Rule> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                TagKey.hashedCodec(Registries.BIOME).fieldOf("biomes").forGetter(Rule::biomes),
                IntProvider.codec(0, 64).fieldOf("count").forGetter(Rule::count))
                .apply(instance, Rule::new));
    }
}
