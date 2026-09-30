package com.drunkencod.spice_road.spice;

import java.util.Arrays;
import java.util.List;

import org.jetbrains.annotations.Nullable;

import net.minecraft.world.item.Item;

import com.drunkencod.spice_road.registry.ModItems;

/**
 * One member per Processed Spice: an item made from a {@link Spice} by a
 * {@link ProcessingMethod}, e.g. dried nutmeg. It's its own item with its own
 * ID and Spice Profile, not a variant of the raw item - a Spice may have
 * several Processed Spices or none, and their names don't have to follow the
 * raw item's.
 * <p>
 * Each member gets an item registered under {@link #getId()}; its profile
 * comes from a {@code spice_profile} JSON like any other Spice Item's.
 */
public enum ProcessedSpice {
    ;

    private final String id;
    private final Spice source;
    private final ProcessingMethod method;

    ProcessedSpice(String id, Spice source, ProcessingMethod method) {
        this.id = id;
        this.source = source;
        this.method = method;
    }

    /** @return The registry path of this Processed Spice's item, e.g. {@code "dried_nutmeg"}. */
    public String getId() {
        return id;
    }

    /** @return The Spice this is made from. */
    public Spice getSource() {
        return source;
    }

    /** @return How this is made from {@link #getSource()}. */
    public ProcessingMethod getMethod() {
        return method;
    }

    /** @return The {@link Tier} of {@link #getSource()}. */
    public Tier getTier() {
        return source.getTier();
    }

    /** @return This Processed Spice's registered item, or {@code null} before registration. */
    public @Nullable Item getItem() {
        return ModItems.byPath(id);
    }

    /**
     * @param source A Spice.
     * @return Every Processed Spice made from {@code source}, in enum order.
     */
    public static List<ProcessedSpice> bySource(Spice source) {
        return Arrays.stream(values()).filter(processed -> processed.source == source).toList();
    }
}
