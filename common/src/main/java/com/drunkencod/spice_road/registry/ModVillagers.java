package com.drunkencod.spice_road.registry;

import java.util.function.Supplier;

import net.minecraft.resources.ResourceKey;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.ai.village.poi.PoiType;
import net.minecraft.world.entity.npc.VillagerProfession;

import com.drunkencod.spice_road.platform.Services;

/**
 * The Spice Trader profession and its workstation, via
 * {@link Services#REGISTRY}. Register the blocks first.
 */
public final class ModVillagers {

    /** Registry path of the Spice Trader and of its workstation's point of interest. */
    public static final String SPICE_TRADER_ID = "spice_trader";

    /**
     * The Spice Trader's workstation: every state of every Spice Rack, whatever
     * its wood and however it is placed.
     */
    public static final ResourceKey<PoiType> SPICE_TRADER_POI = Services.REGISTRY.registerPoiType(SPICE_TRADER_ID,
            ModBlocks.SPICE_RACKS.values(), 1, 1);

    /** The villager profession that trades spices. */
    public static final Supplier<VillagerProfession> SPICE_TRADER = Services.REGISTRY
            .registerVillagerProfession(SPICE_TRADER_ID, SPICE_TRADER_POI, SoundEvents.VILLAGER_WORK_MASON);

    private ModVillagers() {
    }

    /**
     * No-op other than forcing this class (and therefore its static
     * initializers) to load.
     */
    public static void register() {
    }
}
