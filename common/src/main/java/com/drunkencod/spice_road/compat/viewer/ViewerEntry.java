package com.drunkencod.spice_road.compat.viewer;

import net.minecraft.core.HolderLookup;
import net.minecraft.resources.ResourceLocation;

/**
 * One entry of a {@link ViewerCategory}, as every Recipe Viewer adapter shows
 * it. Holds only what identifies the entry; its content is laid out on demand,
 * so values that depend on the config are never stale.
 */
public interface ViewerEntry {

    /** @return The category this entry belongs to. */
    ViewerCategory category();

    /**
     * @return An ID unique among all entries. Entries that aren't backed by a
     *         recipe use a path starting with {@code /}, as EMI expects of
     *         synthetic IDs.
     */
    ResourceLocation id();

    /**
     * @param registries The client's registries, for data-driven content such
     *                   as biome tags.
     * @return The entry's content as of now. Its slots' stacks must not depend
     *         on the config, since viewers index them only once.
     */
    ViewerLayout layout(HolderLookup.Provider registries);
}
