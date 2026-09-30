package com.drunkencod.spice_road.item;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;

import com.drunkencod.spice_road.Constants;

/**
 * Item tag keys used to identify Spice-related items and tools by
 * data/behaviour rather than by a compile-time reference to the {@code Spice}
 * enum.
 * <p>
 * Tags are the deliberate mechanism here (over an enum-driven lookup) for two
 * reasons: they let datapacks extend/override membership (e.g. adding a
 * modded axe to {@link #CUTTING_TOOLS}), and they keep this class free of a
 * compile-time dependency on the {@code Spice}/{@code SourceType}/
 * {@code HarvestAction} enums, which live elsewhere.
 * <p>
 * Backing tag files live under
 * {@code common/src/main/resources/data/spice_road/tags/item/}.
 */
public class SpiceItemTags {

    /**
     * Every Spice Item, including datapack-registered ones. Mirrors the items
     * with a Default Profile for recipes and filtering - having a Default
     * Profile is what actually makes an item a Spice Item. Includes
     * {@link #RAW_SPICES} and {@link #PROCESSED_SPICES}.
     */
    public static final TagKey<Item> SPICES = create("spices");

    /** All raw (freshly harvested) Spice items. */
    public static final TagKey<Item> RAW_SPICES = create("spices/raw");

    /** All Processed Spice items, e.g. dried ones produced by the Drying Rack. */
    public static final TagKey<Item> PROCESSED_SPICES = create("spices/processed");

    /** The seeds of every Spice that grows as a plant (i.e. not a tree or vine). */
    public static final TagKey<Item> SPICE_PLANT_SEEDS = create("spice_plant_seeds");

    /**
     * Tools that count as a "cutting tool" for harvest-tool-requirement checks
     * (e.g. axe-stripping a tree spice, or a bush spice that requires a tool to
     * harvest without damaging the player).
     */
    public static final TagKey<Item> CUTTING_TOOLS = create("cutting_tools");

    /**
     * Tools that satisfy the Harvest Tool Requirement of a Spice harvested by
     * picking (e.g. plucking vanilla pods or saffron stigmas). Includes
     * {@link #CUTTING_TOOLS} by default.
     */
    public static final TagKey<Item> PICKING_TOOLS = create("picking_tools");

    /**
     * Non-edible items that keep the flavor of their ingredients when crafted
     * or cooked (e.g. storage blocks), making them Flavor Carriers.
     */
    public static final TagKey<Item> RETAINS_FLAVOR = create("retains_flavor");

    /**
     * Items that never hold a Profile Override, even if edible or in
     * {@link #RETAINS_FLAVOR}.
     */
    public static final TagKey<Item> UNSEASONABLE = create("unseasonable");

    /**
     * Items that place as a block without being a {@code BlockItem}
     * themselves, so the generic "is this a {@code BlockItem} whose block
     * isn't a {@code BlockEntity}" check can't catch them - e.g. vanilla
     * {@code minecraft:pumpkin_pie}, which Farmer's Delight makes placeable
     * through a Mixin rather than by changing its item class. Datapacks add
     * to this for other mods' items with the same kind of hack.
     */
    public static final TagKey<Item> VOIDS_FLAVOR_WHEN_PLACED = create("voids_flavor_when_placed");

    private SpiceItemTags() {
    }

    private static TagKey<Item> create(String path) {
        return TagKey.create(Registries.ITEM, ResourceLocation.fromNamespaceAndPath(Constants.MOD_ID, path));
    }
}
