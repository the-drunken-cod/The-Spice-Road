package com.drunkencod.spice_road.spice.dimension;

import java.util.List;
import java.util.Optional;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.level.biome.Biome;

import com.drunkencod.spice_road.spice.Climate;

/**
 * A fallback rule giving biomes a {@link Climate} when no Climate tag claims
 * them. A rule matches a biome if every condition it has holds; one without
 * conditions matches everything, so it works as a catch-all at the end of a
 * dimension's rules.
 *
 * @param climate       The Climate a matching biome gets.
 * @param biomes        Biomes or biome tags, any of which has to match. Empty
 *                      means no restriction.
 * @param temperature   Range the biome's base temperature has to lie in.
 * @param precipitation Whether the biome has to have precipitation at the
 *                      queried position or not.
 */
public record ClimateRule(Climate climate, List<BiomeMatcher> biomes, TemperatureRange temperature,
        Optional<Precipitation> precipitation) {

    /**
     * Matcher list as either a single string or a list, so a rule can name one
     * biome or tag without brackets.
     */
    private static final Codec<List<BiomeMatcher>> BIOMES_CODEC = Codec.withAlternative(
            BiomeMatcher.CODEC.listOf(), BiomeMatcher.CODEC, List::of);

    /** Codec of the rule's JSON fields. */
    public static final Codec<ClimateRule> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Climate.CODEC.fieldOf("climate").forGetter(ClimateRule::climate),
            BIOMES_CODEC.optionalFieldOf("biomes", List.of()).forGetter(ClimateRule::biomes),
            TemperatureRange.CODEC.optionalFieldOf("temperature", TemperatureRange.ANY)
                    .forGetter(ClimateRule::temperature),
            Precipitation.CODEC.optionalFieldOf("precipitation").forGetter(ClimateRule::precipitation))
            .apply(instance, ClimateRule::new));

    /** Whether a biome has precipitation where it is queried. */
    public enum Precipitation implements StringRepresentable {
        /** No rain or snow, like deserts. */
        NONE("none"),
        /** Rain or snow. */
        SOME("some");

        /** Codec of the condition's name. */
        public static final Codec<Precipitation> CODEC = StringRepresentable.fromEnum(Precipitation::values);

        private final String id;

        Precipitation(String id) {
            this.id = id;
        }

        @Override
        public String getSerializedName() {
            return id;
        }
    }

    /**
     * A range of base temperatures. Every bound is optional; {@code min} and
     * {@code max} are inclusive, {@code above} and {@code below} exclusive.
     *
     * @param min   The lowest temperature allowed.
     * @param max   The highest temperature allowed.
     * @param above Temperatures have to be greater than this.
     * @param below Temperatures have to be less than this.
     */
    public record TemperatureRange(Optional<Float> min, Optional<Float> max, Optional<Float> above,
            Optional<Float> below) {

        /** The range without bounds. */
        public static final TemperatureRange ANY = new TemperatureRange(Optional.empty(), Optional.empty(),
                Optional.empty(), Optional.empty());

        /** Codec of the range's JSON fields. */
        public static final Codec<TemperatureRange> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                Codec.FLOAT.optionalFieldOf("min").forGetter(TemperatureRange::min),
                Codec.FLOAT.optionalFieldOf("max").forGetter(TemperatureRange::max),
                Codec.FLOAT.optionalFieldOf("above").forGetter(TemperatureRange::above),
                Codec.FLOAT.optionalFieldOf("below").forGetter(TemperatureRange::below))
                .apply(instance, TemperatureRange::new));

        /**
         * @param temperature A base temperature.
         * @return Whether it lies in this range.
         */
        public boolean contains(float temperature) {
            return min.map(bound -> temperature >= bound).orElse(true)
                    && max.map(bound -> temperature <= bound).orElse(true)
                    && above.map(bound -> temperature > bound).orElse(true)
                    && below.map(bound -> temperature < bound).orElse(true);
        }
    }

    /**
     * @param biome The biome to test.
     * @param pos   The position within it, as precipitation can vary with
     *              height (snow at altitude).
     * @return Whether this rule applies to {@code biome}.
     */
    public boolean matches(Holder<Biome> biome, BlockPos pos) {
        if (!biomes.isEmpty() && biomes.stream().noneMatch(matcher -> matcher.matches(biome)))
            return false;
        if (!temperature.contains(biome.value().getBaseTemperature()))
            return false;
        return precipitation.map(wanted -> {
            boolean none = biome.value().getPrecipitationAt(pos) == Biome.Precipitation.NONE;
            return wanted == Precipitation.NONE ? none : !none;
        }).orElse(true);
    }
}
