package com.drunkencod.spice_road.compat.viewer.layout;

import net.minecraft.resources.ResourceLocation;

import com.drunkencod.spice_road.Constants;

/**
 * The GUI sprites of the Recipe Viewer categories and the Flavor Folio's
 * viewer pages. Every sprite has its own constant, even where two default to
 * the same vanilla sprite, so each can be replaced on its own.
 * <p>
 * To use a custom sprite, point the constant at {@link #own(String)}, e.g.
 * {@code own("viewer/drying_arrow")}, and add the PNG as
 * {@code assets/spice_road/textures/gui/sprites/viewer/drying_arrow.png}.
 * Sprites are stretched to the size given by the matching layout class, so
 * draw them at exactly that size.
 */
public final class ViewerSprites {

    // #region Drying

    /** Arrow between the drying input and its results. */
    public static final ResourceLocation DRYING_ARROW = vanilla("container/furnace/burn_progress");
    /** Marks the drying time with a heat source. */
    public static final ResourceLocation DRYING_FLAME = vanilla("container/furnace/lit_progress");

    // #region Spice Origin

    /** Arrow between a Spice's planting item and its raw item. */
    public static final ResourceLocation ORIGIN_ARROW = vanilla("container/furnace/burn_progress");

    private ViewerSprites() {
        // noop
    }

    /**
     * @param path The sprite's path, relative to {@code textures/gui/sprites/}
     *             and without extension.
     * @return The ID of a sprite of this mod.
     */
    public static ResourceLocation own(String path) {
        return ResourceLocation.fromNamespaceAndPath(Constants.MOD_ID, path);
    }

    /**
     * @param path The sprite's path within the vanilla GUI atlas.
     * @return The ID of a vanilla sprite.
     */
    private static ResourceLocation vanilla(String path) {
        return ResourceLocation.withDefaultNamespace(path);
    }
}
