package com.drunkencod.spice_road.config;

import com.mojang.serialization.Codec;

import net.minecraft.util.StringRepresentable;

import com.drunkencod.spice_road.platform.Services;

/**
 * A part of the mod that can be switched off in the common config. Its
 * datapack content carries a load condition naming one of these, so toggling
 * takes effect on the next datapack (re)load.
 */
public enum ModFeature implements StringRepresentable {

    /** The Soil Analyzer item and its recipe. */
    SOIL_ANALYZER("soil_analyzer");

    /** Codec of a feature by its serialized name. */
    public static final Codec<ModFeature> CODEC = StringRepresentable.fromEnum(ModFeature::values);

    /**
     * Registry path of the load condition, in this mod's namespace, on both
     * loaders.
     */
    public static final String CONDITION_ID = "feature_enabled";

    private final String name;

    ModFeature(String name) {
        this.name = name;
    }

    @Override
    public String getSerializedName() {
        return name;
    }

    /** @return Whether the config currently allows this feature. */
    public boolean isEnabled() {
        return switch (this) {
            case SOIL_ANALYZER -> Services.CONFIG.isSoilAnalyzerEnabled();
        };
    }
}
