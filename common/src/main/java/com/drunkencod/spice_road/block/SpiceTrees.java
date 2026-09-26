package com.drunkencod.spice_road.block;

import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;

import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.FireBlock;

import com.drunkencod.spice_road.spice.SourceType;
import com.drunkencod.spice_road.spice.Spice;

/**
 * Registers a {@link SpiceTree} for every {@link Spice} enum member with
 * {@link SourceType#TREE}, driven entirely by the member's own fields.
 */
public final class SpiceTrees {

    private static final Map<Spice, SpiceTree> REGISTERED = new EnumMap<>(Spice.class);

    private SpiceTrees() {
    }

    /**
     * Registers every tree Spice's blocks and items. Must be called during mod
     * initialization (see {@code SpiceRoad#init()}).
     */
    public static void bootstrap() {
        for (Spice spice : Spice.values()) {
            if (spice.getSourceType() == SourceType.TREE)
                REGISTERED.put(spice, new SpiceTree(spice));
        }
    }

    /**
     * Makes logs and leaves burn like vanilla ones. Must be called after block
     * registration has completed (see {@code SpiceRoad#commonSetup()}).
     */
    public static void registerFlammability() {
        FireBlock fire = (FireBlock) Blocks.FIRE;
        REGISTERED.values().forEach(tree -> {
            fire.setFlammable(tree.getLog().get(), 5, 5);
            fire.setFlammable(tree.getStrippedLog().get(), 5, 5);
            fire.setFlammable(tree.getLeaves().get(), 30, 60);
        });
    }

    /**
     * @return All Spice Trees registered by {@link #bootstrap()}, keyed by their
     *         {@link Spice}.
     */
    public static Map<Spice, SpiceTree> getRegistered() {
        return Collections.unmodifiableMap(REGISTERED);
    }
}
