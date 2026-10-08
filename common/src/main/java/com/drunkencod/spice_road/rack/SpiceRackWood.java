package com.drunkencod.spice_road.rack;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.material.MapColor;

/**
 * Every vanilla wood a Spice Rack comes in: what its planks and slab are, for
 * the recipe and the texture, and how its block looks and sounds.
 */
public enum SpiceRackWood {
    OAK("oak", Items.OAK_SLAB, MapColor.WOOD, SoundType.WOOD),
    SPRUCE("spruce", Items.SPRUCE_SLAB, MapColor.PODZOL, SoundType.WOOD),
    BIRCH("birch", Items.BIRCH_SLAB, MapColor.SAND, SoundType.WOOD),
    JUNGLE("jungle", Items.JUNGLE_SLAB, MapColor.DIRT, SoundType.WOOD),
    ACACIA("acacia", Items.ACACIA_SLAB, MapColor.COLOR_ORANGE, SoundType.WOOD),
    DARK_OAK("dark_oak", Items.DARK_OAK_SLAB, MapColor.COLOR_BROWN, SoundType.WOOD),
    MANGROVE("mangrove", Items.MANGROVE_SLAB, MapColor.COLOR_RED, SoundType.WOOD),
    CHERRY("cherry", Items.CHERRY_SLAB, MapColor.TERRACOTTA_WHITE, SoundType.CHERRY_WOOD),
    BAMBOO("bamboo", Items.BAMBOO_SLAB, MapColor.COLOR_YELLOW, SoundType.BAMBOO_WOOD),
    CRIMSON("crimson", Items.CRIMSON_SLAB, MapColor.CRIMSON_STEM, SoundType.NETHER_WOOD),
    WARPED("warped", Items.WARPED_SLAB, MapColor.WARPED_STEM, SoundType.NETHER_WOOD);

    private final String id;
    private final Item slab;
    private final MapColor mapColor;
    private final SoundType soundType;

    SpiceRackWood(String id, Item slab, MapColor mapColor, SoundType soundType) {
        this.id = id;
        this.slab = slab;
        this.mapColor = mapColor;
        this.soundType = soundType;
    }

    /** @return The wood's name as used in vanilla IDs, e.g. {@code "dark_oak"}. */
    public String getId() {
        return id;
    }

    /** @return The registry path of this wood's rack, e.g. {@code "dark_oak_spice_rack"}. */
    public String getRackId() {
        return id + "_spice_rack";
    }

    /** @return The wood's planks block texture, e.g. {@code "minecraft:block/dark_oak_planks"}. */
    public String getPlanksTexture() {
        return "minecraft:block/" + id + "_planks";
    }

    /** @return This wood's vanilla slab item, which the rack is crafted from. */
    public Item getSlab() {
        return slab;
    }

    /** @return The map color of this wood's rack. */
    public MapColor getMapColor() {
        return mapColor;
    }

    /** @return The sounds of this wood's rack. */
    public SoundType getSoundType() {
        return soundType;
    }
}
