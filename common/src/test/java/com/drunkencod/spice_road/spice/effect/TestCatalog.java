package com.drunkencod.spice_road.spice.effect;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import net.minecraft.resources.ResourceLocation;

import com.drunkencod.spice_road.spice.FlavorAxis;
import com.drunkencod.spice_road.spice.effect.SeasoningEffectDef.Pole;

/** A full test catalog: one boon and one bane per axis pole, and a random pool of two. */
public final class TestCatalog {

    public static final int MAX_LEVEL = 3;

    private TestCatalog() {
    }

    public static ResourceLocation id(FlavorAxis axis, boolean positive, EffectKind kind) {
        return ResourceLocation.fromNamespaceAndPath("test",
                axis.getId() + (positive ? "_pos_" : "_neg_") + kind.getSerializedName());
    }

    public static EffectCatalog full() {
        Map<ResourceLocation, EffectCatalog.Entry> entries = new HashMap<>();
        for (FlavorAxis axis : FlavorAxis.values()) {
            for (boolean positive : new boolean[] { true, false }) {
                for (EffectKind kind : EffectKind.values()) {
                    entries.put(id(axis, positive, kind),
                            new EffectCatalog.Entry(kind, Optional.of(new Pole(axis, positive)), 0, MAX_LEVEL));
                }
            }
        }
        entries.put(ResourceLocation.fromNamespaceAndPath("test", "random_boon"),
                new EffectCatalog.Entry(EffectKind.BOON, Optional.empty(), 1, MAX_LEVEL));
        entries.put(ResourceLocation.fromNamespaceAndPath("test", "random_bane"),
                new EffectCatalog.Entry(EffectKind.BANE, Optional.empty(), 1, MAX_LEVEL));
        return new EffectCatalog(entries);
    }
}
