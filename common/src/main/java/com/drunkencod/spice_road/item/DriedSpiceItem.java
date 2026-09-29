package com.drunkencod.spice_road.item;

import com.drunkencod.spice_road.spice.Spice;
import com.drunkencod.spice_road.spice.Tier;

import net.minecraft.world.item.Item;

/**
 * The dried product item of a {@link Spice}, produced by drying its raw
 * {@link SpiceItem} (see {@link SpiceItemTags#DRIED_SPICES}). Not yet
 * registered for any Spice - drying isn't implemented - but kept alongside
 * {@link SpiceItem} so registration can slot it in later without another
 * refactor pass.
 */
public class DriedSpiceItem extends Item {

    /** @param properties Item properties, typically {@link #defaultProperties(Spice)}. */
    public DriedSpiceItem(Properties properties) {
        super(properties);
    }

    /**
     * @param spice The {@link Spice} this item is the dried product of.
     * @return Default item properties, rarity-coded by {@code spice}'s
     *         {@link Tier}.
     */
    public static Properties defaultProperties(Spice spice) {
        return new Properties().rarity(spice.getTier().getRarity());
    }
}
