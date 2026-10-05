package com.drunkencod.spice_road.datagen;

import com.drunkencod.spice_road.Constants;
import com.drunkencod.spice_road.config.ConfigSchema;
import com.drunkencod.spice_road.spice.HarvestAction;
import com.drunkencod.spice_road.spice.SourceType;
import com.drunkencod.spice_road.spice.Spice;

/**
 * Which Spices the pot-style compat recipes (Botany Pots, Immersive
 * Engineering's Garden Cloche) cover, and the timing and yield those recipes
 * bake in.
 * <p>
 * Those recipes are static, so they can't read live config nor judge the Spice
 * Region of the position they run at. They therefore only cover hardy Spices,
 * which grow anywhere, judged against the <em>default</em> hardy threshold, and
 * skip hand-pick Spices. Modpacks that change the config can override or
 * extend the recipes with a datapack.
 */
final class PotCompatSpices {

    /**
     * Growth time in ticks of a {@link SourceType#CROP}-like recipe whose
     * tier has a growth speed multiplier of {@code 1.0}.
     */
    private static final double BASE_GROW_TICKS = 4800.0;

    /** Spice Trees take this many times longer to mature than other Spices. */
    private static final double TREE_GROW_TIME_FACTOR = 2.0;

    private PotCompatSpices() {
    }

    /**
     * @param spice The Spice to check.
     * @return Whether pot-style compat recipes should be generated for
     *         {@code spice}: it's hardy at the default threshold, isn't
     *         hand-pick, and is neither a vine nor a tree harvested by
     *         stripping bark, neither of which a pot can reproduce.
     */
    static boolean isCovered(Spice spice) {
        if (spice.requiresHandPick())
            return false;
        if (spice.getHarvestDifficulty() > ConfigSchema.CULTIVATION_HARDY_HARVEST_DIFFICULTY.getDefault())
            return false;
        if (spice.getSourceType() == SourceType.VINE)
            return false;
        return spice.getSourceType() != SourceType.TREE || spice.getHarvestAction() != HarvestAction.STRIP;
    }

    /**
     * @param spice The Spice to time.
     * @return Ticks a pot takes to grow {@code spice}, using the tier's default
     *         growth speed multiplier, so faster-growing tiers also finish
     *         sooner in a pot.
     */
    static int growTicks(Spice spice) {
        double multiplier = ConfigSchema.CULTIVATION_GROWTH_SPEED_MULTIPLIER.get(spice.getTier()).getDefault();
        double factor = spice.getSourceType() == SourceType.TREE ? TREE_GROW_TIME_FACTOR : 1.0;
        return (int) Math.round(BASE_GROW_TICKS * factor / multiplier);
    }

    /**
     * @param spice The Spice to yield.
     * @return The expected Spice count per harvest: the Spice's drop amount
     *         scaled by the default harvest yield multiplier of its source
     *         type, to be split into a whole count and a fractional chance.
     */
    static double yield(Spice spice) {
        double multiplier = spice.getSourceType() == SourceType.TREE
                ? Constants.DEFAULT_SPICE_TREE_HARVEST_YIELD_MULTIPLIER
                : Constants.DEFAULT_SPICE_PLANT_HARVEST_YIELD_MULTIPLIER;
        return spice.getDropAmount() * multiplier;
    }
}
