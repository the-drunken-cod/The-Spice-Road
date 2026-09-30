package com.drunkencod.spice_road.datagen;

import net.minecraft.resources.ResourceLocation;

import com.drunkencod.spice_road.Constants;
import com.drunkencod.spice_road.spice.Spice;

/**
 * Resource locations of a {@link Spice}'s own textures and block models,
 * which are grouped into one folder per Spice, e.g.
 * {@code spice_road:block/cinnamon/stripped_cinnamon_log}. The same location
 * addresses both a texture ({@code textures/block/...png}) and a block model
 * ({@code models/block/...json}).
 * <p>
 * Item models aren't covered: vanilla looks them up by item ID, so they stay
 * flat at {@code models/item/<id>.json} and only reference these locations.
 */
public final class SpiceAssetPaths {

    private SpiceAssetPaths() {
    }

    /**
     * @param spice The Spice the asset belongs to.
     * @param name  The asset's file name without extension, e.g.
     *              {@code "cinnamon_log_top"}.
     * @return {@code spice_road:block/<spice>/<name>}.
     */
    public static ResourceLocation block(Spice spice, String name) {
        return of("block", spice, name);
    }

    /**
     * @param spice The Spice the asset belongs to.
     * @param name  The asset's file name without extension, e.g.
     *              {@code "cinnamon_sapling"}.
     * @return {@code spice_road:item/<spice>/<name>}.
     */
    public static ResourceLocation item(Spice spice, String name) {
        return of("item", spice, name);
    }

    private static ResourceLocation of(String folder, Spice spice, String name) {
        return ResourceLocation.fromNamespaceAndPath(Constants.MOD_ID, folder + "/" + spice.getId() + "/" + name);
    }
}
