package com.drunkencod.spice_road.item;

import com.drunkencod.spice_road.spice.ProcessedSpice;
import com.drunkencod.spice_road.spice.Tier;

import net.minecraft.world.item.Item;

/**
 * The item of a {@link ProcessedSpice}, e.g. dried nutmeg (see
 * {@link SpiceItemTags#PROCESSED_SPICES}). Its own class rather than a plain
 * {@link Item} so that {@link #defaultProperties(ProcessedSpice)} can carry
 * the source Spice's {@link Tier} rarity, like {@link SpiceItem}.
 */
public class ProcessedSpiceItem extends Item {

    /** @param properties Item properties, typically {@link #defaultProperties(ProcessedSpice)}. */
    public ProcessedSpiceItem(Properties properties) {
        super(properties);
    }

    /**
     * @param processed The Processed Spice this is the item of.
     * @return Default item properties, rarity-coded by {@code processed}'s
     *         {@link Tier}.
     */
    public static Properties defaultProperties(ProcessedSpice processed) {
        return new Properties().rarity(processed.getTier().getRarity());
    }
}
