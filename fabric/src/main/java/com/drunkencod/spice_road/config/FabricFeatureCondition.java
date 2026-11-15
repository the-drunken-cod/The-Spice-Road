package com.drunkencod.spice_road.config;

import org.jetbrains.annotations.Nullable;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.fabricmc.fabric.api.resource.conditions.v1.ResourceCondition;
import net.fabricmc.fabric.api.resource.conditions.v1.ResourceConditionType;
import net.minecraft.core.HolderLookup;
import net.minecraft.resources.ResourceLocation;

import com.drunkencod.spice_road.Constants;

/**
 * Load condition passing while the common config allows the given
 * {@link ModFeature}, as {@code {"condition": "spice_road:feature_enabled", "feature": "<name>"}}.
 * Evaluated whenever datapacks load. NeoForge's counterpart is
 * {@code NeoForgeFeatureCondition}.
 *
 * @param feature The feature whose toggle is checked.
 */
public record FabricFeatureCondition(ModFeature feature) implements ResourceCondition {

    /** Codec of this condition. */
    public static final MapCodec<FabricFeatureCondition> CODEC = RecordCodecBuilder.mapCodec(
            builder -> builder
                    .group(ModFeature.CODEC.fieldOf("feature").forGetter(FabricFeatureCondition::feature))
                    .apply(builder, FabricFeatureCondition::new));

    /** The condition's type, to be registered with {@code ResourceConditions}. */
    public static final ResourceConditionType<FabricFeatureCondition> TYPE = ResourceConditionType.create(
            ResourceLocation.fromNamespaceAndPath(Constants.MOD_ID, ModFeature.CONDITION_ID), CODEC);

    @Override
    public ResourceConditionType<?> getType() {
        return TYPE;
    }

    @Override
    public boolean test(@Nullable HolderLookup.Provider registryLookup) {
        return feature.isEnabled();
    }
}
