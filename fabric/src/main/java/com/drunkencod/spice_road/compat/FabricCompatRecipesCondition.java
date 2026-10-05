package com.drunkencod.spice_road.compat;

import org.jetbrains.annotations.Nullable;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.fabricmc.fabric.api.resource.conditions.v1.ResourceCondition;
import net.fabricmc.fabric.api.resource.conditions.v1.ResourceConditionType;
import net.minecraft.core.HolderLookup;
import net.minecraft.resources.ResourceLocation;

import com.drunkencod.spice_road.Constants;

/**
 * Load condition passing while the common config allows recipes for the given
 * optional mod, as {@code {"condition": "spice_road:compat_recipes_enabled", "mod": "<mod ID>"}}.
 * Evaluated whenever datapacks load. NeoForge's counterpart is
 * {@code NeoForgeCompatRecipesCondition}.
 *
 * @param mod The mod whose recipe toggle is checked.
 */
public record FabricCompatRecipesCondition(CompatRecipeMod mod) implements ResourceCondition {

    /** Codec of this condition. */
    public static final MapCodec<FabricCompatRecipesCondition> CODEC = RecordCodecBuilder.mapCodec(
            builder -> builder
                    .group(CompatRecipeMod.CODEC.fieldOf("mod").forGetter(FabricCompatRecipesCondition::mod))
                    .apply(builder, FabricCompatRecipesCondition::new));

    /** The condition's type, to be registered with {@code ResourceConditions}. */
    public static final ResourceConditionType<FabricCompatRecipesCondition> TYPE = ResourceConditionType.create(
            ResourceLocation.fromNamespaceAndPath(Constants.MOD_ID, CompatRecipeMod.CONDITION_ID), CODEC);

    @Override
    public ResourceConditionType<?> getType() {
        return TYPE;
    }

    @Override
    public boolean test(@Nullable HolderLookup.Provider registryLookup) {
        return mod.isEnabled();
    }
}
