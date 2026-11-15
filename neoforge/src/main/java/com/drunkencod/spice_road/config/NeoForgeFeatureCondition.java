package com.drunkencod.spice_road.config;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.neoforged.neoforge.common.conditions.ICondition;

/**
 * Load condition passing while the common config allows the given
 * {@link ModFeature}, as {@code {"type": "spice_road:feature_enabled", "feature": "<name>"}}.
 * Evaluated whenever datapacks load. Fabric's counterpart is
 * {@code FabricFeatureCondition}.
 *
 * @param feature The feature whose toggle is checked.
 */
public record NeoForgeFeatureCondition(ModFeature feature) implements ICondition {

    /** Codec of this condition. */
    public static final MapCodec<NeoForgeFeatureCondition> CODEC = RecordCodecBuilder.mapCodec(
            builder -> builder
                    .group(ModFeature.CODEC.fieldOf("feature").forGetter(NeoForgeFeatureCondition::feature))
                    .apply(builder, NeoForgeFeatureCondition::new));

    @Override
    public boolean test(IContext context) {
        return feature.isEnabled();
    }

    @Override
    public MapCodec<? extends ICondition> codec() {
        return CODEC;
    }

    @Override
    public String toString() {
        return "feature_enabled(\"" + feature.getSerializedName() + "\")";
    }
}
