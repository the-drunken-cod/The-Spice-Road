package com.drunkencod.spice_road.compat;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.neoforged.neoforge.common.conditions.ICondition;

/**
 * Load condition passing while the common config allows recipes for the given
 * optional mod, as {@code {"type": "spice_road:compat_recipes_enabled", "mod": "<mod ID>"}}.
 * Evaluated whenever datapacks load. Fabric's counterpart is
 * {@code FabricCompatRecipesCondition}.
 *
 * @param mod The mod whose recipe toggle is checked.
 */
public record NeoForgeCompatRecipesCondition(CompatRecipeMod mod) implements ICondition {

    /** Codec of this condition. */
    public static final MapCodec<NeoForgeCompatRecipesCondition> CODEC = RecordCodecBuilder.mapCodec(
            builder -> builder
                    .group(CompatRecipeMod.CODEC.fieldOf("mod").forGetter(NeoForgeCompatRecipesCondition::mod))
                    .apply(builder, NeoForgeCompatRecipesCondition::new));

    @Override
    public boolean test(IContext context) {
        return mod.isEnabled();
    }

    @Override
    public MapCodec<? extends ICondition> codec() {
        return CODEC;
    }

    @Override
    public String toString() {
        return "compat_recipes_enabled(\"" + mod.getModId() + "\")";
    }
}
