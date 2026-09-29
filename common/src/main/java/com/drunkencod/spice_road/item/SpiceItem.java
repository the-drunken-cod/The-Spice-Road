package com.drunkencod.spice_road.item;

import com.drunkencod.spice_road.spice.Spice;
import com.drunkencod.spice_road.spice.Tier;

import net.minecraft.world.item.Item;

/**
 * The raw product item of a {@link Spice}, e.g. raw cinnamon bark or a
 * saffron thread. Its own class rather than a plain {@link Item} so that
 * {@link #defaultProperties(Spice)} can carry the Spice's {@link Tier}
 * rarity, and so future item-level behaviour has somewhere to live.
 */
public class SpiceItem extends Item {

    /** @param properties Item properties, typically {@link #defaultProperties(Spice)}. */
    public SpiceItem(Properties properties) {
        super(properties);
    }

    /**
     * @param spice The {@link Spice} this item is the raw product of.
     * @return Default item properties, rarity-coded by {@code spice}'s
     *         {@link Tier}.
     */
    public static Properties defaultProperties(Spice spice) {
        return new Properties().rarity(spice.getTier().getRarity());
    }
}
