package com.drunkencod.spice_road.block;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.block.Block;

import com.drunkencod.spice_road.Constants;

/**
 * Block tags of the mod. Those listing its own Spice blocks are generated from
 * the registered Spice Plants, Trees, Vines and Racks, so they can't fall out of sync
 * with registration.
 */
public final class SpiceBlockTags {

    /** Every Spice Plant block, both the planted ones and their worldgen-only wild counterparts. */
    public static final TagKey<Block> SPICE_CROPS = create("spice_crops");

    /** Every Spice Tree log and stripped log. */
    public static final TagKey<Block> SPICE_TREE_LOGS = create("spice_tree_logs");

    /** Every Spice Tree's leaves. */
    public static final TagKey<Block> SPICE_TREE_LEAVES = create("spice_tree_leaves");

    /** Every Spice Tree sapling. */
    public static final TagKey<Block> SPICE_TREE_SAPLINGS = create("spice_tree_saplings");

    /** Every Spice Rack block, of every wood. */
    public static final TagKey<Block> SPICE_RACKS = create("spice_racks");

    /**
     * Blocks whose block entity stores items that the Spice Grinder may take
     * spices from, within a configurable radius of the player. Hand-written
     * (not generated), so datapacks can add any block that saves its items in
     * a list, e.g. another mod's chests. Includes {@link #SPICE_RACKS}.
     */
    public static final TagKey<Block> SPICE_STORAGE = create("spice_storage");

    /**
     * Blocks that heat a Drying Rack standing next to them, so it dries faster.
     * Hand-written, so datapacks can add any hot block; a block with a
     * {@code lit} property only heats while it is lit.
     */
    public static final TagKey<Block> HEAT_SOURCES = create("heat_sources");

    /** Every Spice Vine block. */
    public static final TagKey<Block> SPICE_VINES = create("spice_vines");

    private SpiceBlockTags() {
    }

    private static TagKey<Block> create(String path) {
        return TagKey.create(Registries.BLOCK, ResourceLocation.fromNamespaceAndPath(Constants.MOD_ID, path));
    }
}
