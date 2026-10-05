package com.drunkencod.spice_road.compat;

import com.mojang.serialization.Codec;

import net.minecraft.util.StringRepresentable;

import com.drunkencod.spice_road.platform.Services;

/**
 * An optional mod the mod datagens recipes for, each of which can be switched
 * off in the common config. Loaded recipes carry a load condition naming one of
 * these, so toggling takes effect on the next datapack (re)load.
 */
public enum CompatRecipeMod implements StringRepresentable {

    /** Botany Pots crop recipes. */
    BOTANY_POTS("botanypots"),
    /** Immersive Engineering Garden Cloche recipes. NeoForge only. */
    IMMERSIVE_ENGINEERING("immersiveengineering");

    /** Codec of a mod by its mod ID. */
    public static final Codec<CompatRecipeMod> CODEC = StringRepresentable.fromEnum(CompatRecipeMod::values);

    /**
     * Registry path of the load condition, in this mod's namespace, on both
     * loaders.
     */
    public static final String CONDITION_ID = "compat_recipes_enabled";

    private final String modId;

    CompatRecipeMod(String modId) {
        this.modId = modId;
    }

    /** @return The mod's ID, which is also the key of its config option. */
    public String getModId() {
        return modId;
    }

    @Override
    public String getSerializedName() {
        return modId;
    }

    /** @return Whether the config currently allows loading recipes for this mod. */
    public boolean isEnabled() {
        return Services.CONFIG.isCompatRecipeEnabled(this);
    }
}
