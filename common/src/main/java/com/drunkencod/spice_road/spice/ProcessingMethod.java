package com.drunkencod.spice_road.spice;

import net.minecraft.network.chat.Component;

/**
 * How a {@link ProcessedSpice} is made from its source {@link Spice}. Only
 * drying exists so far; soaking, smoking, cutting and the like may follow.
 */
public enum ProcessingMethod {

    /** Dried on a Drying Rack. */
    DRYING("drying");

    private final String id;

    ProcessingMethod(String id) {
        this.id = id;
    }

    /** @return This method's ID, e.g. {@code "drying"}. */
    public String getId() {
        return id;
    }

    /** @return The translation key of this method's name, e.g. {@code "Drying"}. */
    public String getTranslationKey() {
        return "processing_method.spice_road." + id;
    }

    /** @return This method's translatable name. See {@link #getTranslationKey()}. */
    public Component getDisplayName() {
        return Component.translatable(getTranslationKey());
    }
}
