package com.drunkencod.spice_road.block;

import com.drunkencod.spice_road.spice.Spice;
import com.drunkencod.spice_road.spice.Tier;

import net.minecraft.world.item.BlockItem;
import net.minecraft.world.level.block.Block;

/**
 * The block item of a {@link SpiceSaplingBlock}. Its own class rather than
 * a plain {@link BlockItem} so that {@link #defaultProperties(Spice)} can
 * carry the Spice's {@link Tier} rarity.
 */
public class SpiceSaplingItem extends BlockItem {

    /**
     * @param block      The Spice Sapling block this item plants.
     * @param properties Item properties, typically {@link #defaultProperties(Spice)}.
     */
    public SpiceSaplingItem(Block block, Properties properties) {
        super(block, properties);
    }

    /**
     * @param spice The {@link Spice} this sapling grows.
     * @return Default item properties, rarity-coded by {@code spice}'s
     *         {@link Tier}.
     */
    public static Properties defaultProperties(Spice spice) {
        return new Properties().rarity(spice.getTier().getRarity());
    }
}
