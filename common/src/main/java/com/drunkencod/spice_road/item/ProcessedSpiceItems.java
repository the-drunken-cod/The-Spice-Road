package com.drunkencod.spice_road.item;

import com.drunkencod.spice_road.datagen.ItemModelHelper;
import com.drunkencod.spice_road.platform.Services;
import com.drunkencod.spice_road.spice.ProcessedSpice;

/**
 * Registers the item of every {@link ProcessedSpice}, with a flat item model
 * whose texture lives in its source Spice's folder.
 */
public final class ProcessedSpiceItems {

    private ProcessedSpiceItems() {
    }

    /**
     * Registers every Processed Spice's item. Must be called during mod
     * initialization (see {@code SpiceRoad#init()}).
     */
    public static void bootstrap() {
        for (ProcessedSpice processed : ProcessedSpice.values()) {
            Services.REGISTRY.registerItem(processed.getId(),
                    () -> new ProcessedSpiceItem(ProcessedSpiceItem.defaultProperties(processed)));
            ItemModelHelper.addFlatItem(processed.getId(), processed.getSource());
        }
    }
}
