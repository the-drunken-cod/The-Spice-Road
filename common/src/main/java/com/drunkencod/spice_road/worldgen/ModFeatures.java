package com.drunkencod.spice_road.worldgen;

import java.util.function.Supplier;

import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;

import com.drunkencod.spice_road.platform.Services;

/**
 * Registers the {@link Feature} <i>types</i> used by Spice Plant worldgen,
 * via {@link Services#REGISTRY} - plain vanilla {@code Registry.register}
 * doesn't work here on NeoForge, since {@code BuiltInRegistries.FEATURE}
 * freezes during vanilla bootstrap, before mods even construct (see
 * {@code IRegistryHelper#registerFeature}'s javadoc).
 * <p>
 * The actual placement configuration ({@code configured_feature}/
 * {@code placed_feature}) lives under
 * {@code common/src/main/resources/data/spice_road/worldgen/}. Biome
 * injection is per-loader: a NeoForge {@code biome_modifier} JSON
 * (dynamic registry, NeoForge-only data), and a
 * {@code BiomeModifications} call in Fabric's
 * {@code SpiceRoadMod#onInitialize()}.
 */
public final class ModFeatures {

    /** {@code spice_road:spice_plant} - see {@link SpicePlantFeature}. */
    public static final Supplier<Feature<NoneFeatureConfiguration>> SPICE_PLANT = Services.REGISTRY
            .registerFeature("spice_plant", () -> new SpicePlantFeature(NoneFeatureConfiguration.CODEC));

    private ModFeatures() {
    }

    /**
     * No-op other than forcing this class (and therefore {@link #SPICE_PLANT}'s
     * static initializer) to load.
     */
    public static void register() {
    }
}
