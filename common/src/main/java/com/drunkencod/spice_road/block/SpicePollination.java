package com.drunkencod.spice_road.block;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

import com.drunkencod.spice_road.platform.Services;

/**
 * Decides which Spice plants bees may pollinate. Which blocks count as flowers
 * at all is left to the {@code #minecraft:flowers} block tag; this only narrows
 * that down to ripe Spice plants, which a block tag can't express since it
 * ignores blockstates.
 */
public final class SpicePollination {

    private SpicePollination() {
    }

    /**
     * @param state A block state bees already consider a flower.
     * @return {@code false} if {@code state} is a Spice plant, vine or
     *         fruiting leaves that isn't ripe yet and unripe pollination is
     *         disabled (see
     *         {@code IConfigHelper#isUnripeSpicePollinationAllowed()}),
     *         otherwise {@code true}.
     */
    public static boolean mayPollinate(BlockState state) {
        Block block = state.getBlock();
        boolean ripe;
        if (block instanceof SpicePlantBlock plant)
            ripe = plant.isMaxAge(state);
        else if (block instanceof SpiceVineBlock)
            ripe = SpiceVineBlock.isRipe(state);
        else if (block instanceof FruitingSpiceLeavesBlock)
            ripe = FruitingSpiceLeavesBlock.isRipe(state);
        else
            return true;

        return ripe || Services.CONFIG.isUnripeSpicePollinationAllowed();
    }
}
