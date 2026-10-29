package com.drunkencod.spice_road.worldgen.village;

import java.util.List;

import net.minecraft.resources.ResourceLocation;

import com.drunkencod.spice_road.Constants;
import com.drunkencod.spice_road.spice.Climate;

/**
 * The vanilla village styles that get a Spice Trader house. Zombie villages
 * deliberately have none.
 */
public enum VillageBiome {
    PLAINS("plains", "minecraft:mossify_10_percent", Climate.TEMPERATE),
    DESERT("desert", "minecraft:empty", Climate.ARID),
    SAVANNA("savanna", "minecraft:empty", Climate.ARID, Climate.TROPICAL),
    SNOWY("snowy", "minecraft:empty", Climate.COLD),
    TAIGA("taiga", "minecraft:mossify_10_percent", Climate.COLD, Climate.TEMPERATE);

    private final String id;
    private final String processors;
    private final List<Climate> climates;

    VillageBiome(String id, String processors, Climate... climates) {
        this.id = id;
        this.processors = processors;
        this.climates = List.of(climates);
    }

    /** @return The style's name, as used in vanilla's village paths. */
    public String getId() {
        return id;
    }

    /** @return The vanilla template pool this style's houses are drawn from. */
    public ResourceLocation getPool() {
        return ResourceLocation.withDefaultNamespace("village/" + id + "/houses");
    }

    /**
     * @return The ID of the structure template of this style's Spice Trader
     *         house, which a resource pack author provides.
     */
    public ResourceLocation getHouseStructure() {
        return ResourceLocation.fromNamespaceAndPath(Constants.MOD_ID, "village/houses/" + id + "_spice_trader");
    }

    /**
     * @return The ID of the loot table that stocks this style's house's Spice Rack.
     */
    public ResourceLocation getRackLootTable() {
        return ResourceLocation.fromNamespaceAndPath(Constants.MOD_ID, "racks/village/" + id);
    }

    /**
     * @return The ID of the processor list vanilla applies to its own houses of
     *         this style, so ours weather the same way.
     */
    public String getProcessors() {
        return processors;
    }

    /** @return The Climates whose Spices stock this style's racks. */
    public List<Climate> getClimates() {
        return climates;
    }
}
