package com.drunkencod.spice_road.item;

import java.util.LinkedHashMap;
import java.util.Map;

import net.minecraft.world.item.Item;

import com.drunkencod.spice_road.block.SpicePlants;
import com.drunkencod.spice_road.block.SpiceTree;
import com.drunkencod.spice_road.block.SpiceTrees;

/**
 * The compost chances of the Spice Road's plant items: planting items (seeds,
 * cuttings), saplings and leaves. NeoForge datagens them into the
 * {@code neoforge:compostables} data map; Fabric registers them in code.
 */
public final class SpiceCompostables {

    /** The chance for a layer, equal to vanilla seeds, saplings and leaves. */
    public static final float CHANCE = 0.3F;

    private SpiceCompostables() {
    }

    /**
     * @return Every compostable Spice Road item mapped to its chance of raising
     *         the composter's level by one, in registration order.
     */
    public static Map<Item, Float> getAll() {
        Map<Item, Float> compostables = new LinkedHashMap<>();
        SpicePlants.getRegistered().values()
                .forEach(plant -> compostables.put(plant.seedItem().get(), CHANCE));
        for (SpiceTree tree : SpiceTrees.getRegistered().values()) {
            compostables.put(tree.getSaplingItem().get(), CHANCE);
            compostables.put(tree.getLeavesItem().get(), CHANCE);
        }
        return compostables;
    }
}
