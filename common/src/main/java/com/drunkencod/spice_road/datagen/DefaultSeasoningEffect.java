package com.drunkencod.spice_road.datagen;

import static com.drunkencod.spice_road.spice.FlavorAxis.*;
import static com.drunkencod.spice_road.spice.effect.EffectKind.BANE;
import static com.drunkencod.spice_road.spice.effect.EffectKind.BOON;
import static com.drunkencod.spice_road.spice.effect.LevelScaling.AMPLIFIER;
import static com.drunkencod.spice_road.spice.effect.LevelScaling.DURATION;

import com.drunkencod.spice_road.Constants;
import com.drunkencod.spice_road.spice.FlavorAxis;
import com.drunkencod.spice_road.spice.effect.EffectKind;
import com.drunkencod.spice_road.spice.effect.LevelScaling;

/**
 * The Seasoning Effect catalog the mod ships: one boon and one bane per
 * {@link FlavorAxis} pole (16 + 16, mostly vanilla stand-ins that can be
 * replaced by overriding the datapack entry) plus the pool of vanilla effects a
 * random effect cell draws from. {@link SeasoningEffectProvider} writes them out
 * as {@code data/spice_road/seasoning_effect/*.json}.
 */
public enum DefaultSeasoningEffect {

    // #region Boons
    HEAT_POSITIVE_BOON("minecraft:strength", BOON, HEAT_COOLING, true, 2400, 3, AMPLIFIER),
    HEAT_NEGATIVE_BOON("minecraft:fire_resistance", BOON, HEAT_COOLING, false, 2400, 3, DURATION),
    SWEET_BITTER_POSITIVE_BOON("minecraft:speed", BOON, SWEET_BITTER, true, 2400, 3, AMPLIFIER),
    SWEET_BITTER_NEGATIVE_BOON("minecraft:regeneration", BOON, SWEET_BITTER, false, 600, 3, AMPLIFIER),
    SOUR_MELLOW_POSITIVE_BOON("minecraft:haste", BOON, SOUR_MELLOW, true, 2400, 3, AMPLIFIER),
    SOUR_MELLOW_NEGATIVE_BOON("minecraft:absorption", BOON, SOUR_MELLOW, false, 1200, 3, AMPLIFIER),
    EARTHY_FLORAL_POSITIVE_BOON("minecraft:resistance", BOON, EARTHY_FLORAL, true, 1200, 2, AMPLIFIER),
    EARTHY_FLORAL_NEGATIVE_BOON("minecraft:luck", BOON, EARTHY_FLORAL, false, 3600, 3, AMPLIFIER),
    WOODY_GREEN_POSITIVE_BOON("minecraft:health_boost", BOON, WOODY_GREEN, true, 2400, 3, AMPLIFIER),
    WOODY_GREEN_NEGATIVE_BOON("minecraft:jump_boost", BOON, WOODY_GREEN, false, 2400, 3, AMPLIFIER),
    PUNGENT_SOFT_POSITIVE_BOON("minecraft:night_vision", BOON, PUNGENT_SOFT, true, 3600, 3, DURATION),
    PUNGENT_SOFT_NEGATIVE_BOON("minecraft:dolphins_grace", BOON, PUNGENT_SOFT, false, 2400, 2, DURATION),
    RESINOUS_CLEAN_POSITIVE_BOON("minecraft:water_breathing", BOON, RESINOUS_CLEAN, true, 3600, 3, DURATION),
    RESINOUS_CLEAN_NEGATIVE_BOON("minecraft:conduit_power", BOON, RESINOUS_CLEAN, false, 2400, 2, DURATION),
    SAVORY_DELICATE_POSITIVE_BOON("minecraft:saturation", BOON, SAVORY_DELICATE, true, 3, 3, DURATION),
    SAVORY_DELICATE_NEGATIVE_BOON("minecraft:slow_falling", BOON, SAVORY_DELICATE, false, 1800, 3, DURATION),

    // #region Banes
    HEAT_POSITIVE_BANE("spice_road:hot", BANE, HEAT_COOLING, true, 600, 3, AMPLIFIER),
    HEAT_NEGATIVE_BANE("spice_road:chilled", BANE, HEAT_COOLING, false, 600, 3, AMPLIFIER),
    SWEET_BITTER_POSITIVE_BANE("minecraft:slowness", BANE, SWEET_BITTER, true, 600, 3, AMPLIFIER),
    SWEET_BITTER_NEGATIVE_BANE("minecraft:nausea", BANE, SWEET_BITTER, false, 300, 3, DURATION),
    SOUR_MELLOW_POSITIVE_BANE("minecraft:weakness", BANE, SOUR_MELLOW, true, 600, 3, AMPLIFIER),
    SOUR_MELLOW_NEGATIVE_BANE("minecraft:mining_fatigue", BANE, SOUR_MELLOW, false, 600, 3, AMPLIFIER),
    EARTHY_FLORAL_POSITIVE_BANE("minecraft:weaving", BANE, EARTHY_FLORAL, true, 1200, 2, DURATION),
    EARTHY_FLORAL_NEGATIVE_BANE("minecraft:oozing", BANE, EARTHY_FLORAL, false, 1200, 2, DURATION),
    WOODY_GREEN_POSITIVE_BANE("minecraft:infested", BANE, WOODY_GREEN, true, 1200, 2, DURATION),
    WOODY_GREEN_NEGATIVE_BANE("minecraft:wind_charged", BANE, WOODY_GREEN, false, 1200, 2, DURATION),
    PUNGENT_SOFT_POSITIVE_BANE("minecraft:blindness", BANE, PUNGENT_SOFT, true, 200, 3, DURATION),
    PUNGENT_SOFT_NEGATIVE_BANE("minecraft:unluck", BANE, PUNGENT_SOFT, false, 3600, 3, AMPLIFIER),
    RESINOUS_CLEAN_POSITIVE_BANE("minecraft:darkness", BANE, RESINOUS_CLEAN, true, 300, 3, DURATION),
    RESINOUS_CLEAN_NEGATIVE_BANE("minecraft:glowing", BANE, RESINOUS_CLEAN, false, 1200, 3, DURATION),
    SAVORY_DELICATE_POSITIVE_BANE("minecraft:hunger", BANE, SAVORY_DELICATE, true, 600, 3, AMPLIFIER),
    SAVORY_DELICATE_NEGATIVE_BANE("minecraft:levitation", BANE, SAVORY_DELICATE, false, 60, 2, DURATION),

    // #region Random pool
    RANDOM_SPEED("minecraft:speed", BOON, 1200, 2, AMPLIFIER),
    RANDOM_HASTE("minecraft:haste", BOON, 1200, 2, AMPLIFIER),
    RANDOM_JUMP_BOOST("minecraft:jump_boost", BOON, 1200, 2, AMPLIFIER),
    RANDOM_NIGHT_VISION("minecraft:night_vision", BOON, 3600, 2, DURATION),
    RANDOM_FIRE_RESISTANCE("minecraft:fire_resistance", BOON, 2400, 2, DURATION),
    RANDOM_REGENERATION("minecraft:regeneration", BOON, 200, 2, AMPLIFIER),
    RANDOM_SLOWNESS("minecraft:slowness", BANE, 400, 2, AMPLIFIER),
    RANDOM_MINING_FATIGUE("minecraft:mining_fatigue", BANE, 400, 2, AMPLIFIER),
    RANDOM_HUNGER("minecraft:hunger", BANE, 400, 2, AMPLIFIER),
    RANDOM_NAUSEA("minecraft:nausea", BANE, 200, 2, DURATION),
    RANDOM_BLINDNESS("minecraft:blindness", BANE, 100, 2, DURATION),
    RANDOM_WEAKNESS("minecraft:weakness", BANE, 400, 2, AMPLIFIER);

    private final String effect;
    private final EffectKind kind;
    private final FlavorAxis axis;
    private final boolean positive;
    private final int randomWeight;
    private final int baseDuration;
    private final int maxLevel;
    private final LevelScaling scaling;

    /** An entry that is the boon or bane of one axis pole. */
    DefaultSeasoningEffect(String effect, EffectKind kind, FlavorAxis axis, boolean positive, int baseDuration,
            int maxLevel, LevelScaling scaling) {
        this.effect = effect;
        this.kind = kind;
        this.axis = axis;
        this.positive = positive;
        this.randomWeight = 0;
        this.baseDuration = baseDuration;
        this.maxLevel = maxLevel;
        this.scaling = scaling;
    }

    /** An entry of the vanilla-random pool, with weight 1. */
    DefaultSeasoningEffect(String effect, EffectKind kind, int baseDuration, int maxLevel, LevelScaling scaling) {
        this.effect = effect;
        this.kind = kind;
        this.axis = null;
        this.positive = false;
        this.randomWeight = 1;
        this.baseDuration = baseDuration;
        this.maxLevel = maxLevel;
        this.scaling = scaling;
    }

    /** @return The entry's path in the {@value Constants#MOD_ID} namespace, also its file name. */
    public String getId() {
        return name().toLowerCase(java.util.Locale.ROOT);
    }

    /** @return The ID of the mob effect the entry applies. */
    public String getEffect() {
        return effect;
    }

    /** @return Whether the entry helps or hurts the eater. */
    public EffectKind getKind() {
        return kind;
    }

    /** @return The axis the entry is the effect of a pole of, or {@code null} for a random pool entry. */
    public FlavorAxis getAxis() {
        return axis;
    }

    /** @return Whether the entry belongs to the positive pole of {@link #getAxis()}. */
    public boolean isPositive() {
        return positive;
    }

    /** @return The weight in the vanilla-random pool, {@code 0} for a pole entry. */
    public int getRandomWeight() {
        return randomWeight;
    }

    /** @return The duration in ticks at level 1. */
    public int getBaseDuration() {
        return baseDuration;
    }

    /** @return The highest level the entry stacks to. */
    public int getMaxLevel() {
        return maxLevel;
    }

    /** @return What a higher level does to the mob effect. */
    public LevelScaling getScaling() {
        return scaling;
    }
}
