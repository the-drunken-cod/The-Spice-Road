package com.drunkencod.spice_road.spice.dimension;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import com.drunkencod.spice_road.spice.Climate;
import com.drunkencod.spice_road.spice.Spice;

/**
 * One datapack file of a Spice Dimension, before the files of all datapacks
 * are {@linkplain SpiceDimension#merge merged}. Everything is optional, so a
 * pack only writes what it changes.
 *
 * @param replace         Whether to drop everything from lower-priority packs
 *                        first.
 * @param buckets         Edits to Climate Buckets. A Climate with an empty edit
 *                        still counts as one the dimension has.
 * @param rules           Fallback rules, checked in this order, and before
 *                        those of lower-priority packs.
 * @param surface         How ground is found.
 * @param saltOffset      Added to the global region salt.
 * @param cellScaleFactor Multiplies the global region cell scale.
 * @param allowPonds      Whether Spice Ponds and Oases may be carved.
 */
public record SpiceDimensionLayer(boolean replace, Map<Climate, BucketEdit> buckets, List<ClimateRule> rules,
        Optional<SurfaceStrategy> surface, Optional<Long> saltOffset, Optional<Double> cellScaleFactor,
        Optional<Boolean> allowPonds) {

    /** Codec of the file's JSON fields. */
    public static final Codec<SpiceDimensionLayer> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.BOOL.optionalFieldOf("replace", false).forGetter(SpiceDimensionLayer::replace),
            Codec.unboundedMap(Climate.CODEC, BucketEdit.CODEC).optionalFieldOf("buckets", Map.of())
                    .forGetter(SpiceDimensionLayer::buckets),
            ClimateRule.CODEC.listOf().optionalFieldOf("rules", List.of()).forGetter(SpiceDimensionLayer::rules),
            SurfaceStrategy.CODEC.optionalFieldOf("surface").forGetter(SpiceDimensionLayer::surface),
            Codec.LONG.optionalFieldOf("salt_offset").forGetter(SpiceDimensionLayer::saltOffset),
            Codec.doubleRange(0.001D, 1000D).optionalFieldOf("cell_scale_multiplier")
                    .forGetter(SpiceDimensionLayer::cellScaleFactor),
            Codec.BOOL.optionalFieldOf("allow_ponds").forGetter(SpiceDimensionLayer::allowPonds))
            .apply(instance, SpiceDimensionLayer::new));

    /**
     * Spices added to and removed from one Climate Bucket. Written either as
     * just a list of Spice IDs (which are added), or as an object with
     * {@code add} and/or {@code remove} lists; within one file, removing wins.
     *
     * @param add    Spices to put in the bucket.
     * @param remove Spices to take out of it.
     */
    public record BucketEdit(List<Spice> add, List<Spice> remove) {

        private static final Codec<BucketEdit> OBJECT_CODEC = RecordCodecBuilder.create(instance -> instance.group(
                Spice.CODEC.listOf().optionalFieldOf("add", List.of()).forGetter(BucketEdit::add),
                Spice.CODEC.listOf().optionalFieldOf("remove", List.of()).forGetter(BucketEdit::remove))
                .apply(instance, BucketEdit::new));

        /** Codec of an edit, accepting the shorthand list. */
        public static final Codec<BucketEdit> CODEC = Codec.either(Spice.CODEC.listOf(), OBJECT_CODEC).xmap(
                either -> either.map(list -> new BucketEdit(list, List.of()), edit -> edit),
                edit -> edit.remove().isEmpty() ? Either.left(edit.add()) : Either.right(edit));
    }
}
