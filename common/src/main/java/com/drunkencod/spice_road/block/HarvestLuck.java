package com.drunkencod.spice_road.block;

import org.jetbrains.annotations.Nullable;

import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;

import com.drunkencod.spice_road.platform.Services;

/**
 * Harvest Luck: how a harvesting player's Luck changes the Spice yield and
 * the seed drop chance of a harvest. Shared by the Java harvest paths and the
 * custom loot function/condition of the break loot tables, so both read the
 * same live config.
 */
public final class HarvestLuck {

    private HarvestLuck() {
    }

    /**
     * @param harvester The entity that harvested, if any.
     * @return The harvester's Luck, clamped to the configured cap in both
     *         directions; {@code 0} if it isn't a player.
     */
    public static float of(@Nullable Entity harvester) {
        if (!(harvester instanceof Player player))
            return 0.0F;

        float cap = Services.CONFIG.getHarvestLuckMaxLevel();
        return Mth.clamp(player.getLuck(), -cap, cap);
    }

    /**
     * Rolls the number of items a harvest yields: one, unless the harvester's
     * Luck interferes. With a chance of {@code yieldBonus * |luck|}, Luck
     * yields one extra item per whole level, while Bad Luck yields nothing.
     * The yield therefore never exceeds the configured Luck cap plus one.
     *
     * @param random    The random source to roll with.
     * @param harvester The entity that harvested, if any.
     * @return The number of items to drop, never negative.
     */
    public static int rollYield(RandomSource random, @Nullable Entity harvester) {
        float luck = of(harvester);
        if (luck == 0.0F || random.nextDouble() >= Services.CONFIG.getHarvestLuckYieldBonus() * Math.abs(luck))
            return 1;

        return luck > 0.0F ? 1 + (int) luck : 0;
    }

    /**
     * @param baseChance The seed drop chance before Luck, from {@code 0.0} to {@code 1.0}.
     * @param harvester  The entity that harvested, if any.
     * @return {@code baseChance} scaled by the harvester's Luck, capped to
     *         {@code 0.0}-{@code 1.0}.
     */
    public static float seedChance(float baseChance, @Nullable Entity harvester) {
        return Mth.clamp((float) (baseChance * scale(Services.CONFIG.getHarvestLuckSeedChanceBonus(), of(harvester))),
                0.0F, 1.0F);
    }

    /**
     * Scales a value by Luck.
     *
     * @param bonusPerLevel The change per level of Luck, as a fraction.
     * @param luck          The clamped Luck.
     * @return {@code 1 + bonusPerLevel * luck}, never negative.
     */
    private static double scale(double bonusPerLevel, float luck) {
        return Math.max(0.0D, 1.0D + bonusPerLevel * luck);
    }
}
