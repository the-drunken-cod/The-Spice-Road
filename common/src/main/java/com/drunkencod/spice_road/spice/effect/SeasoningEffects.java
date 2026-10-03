package com.drunkencod.spice_road.spice.effect;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;

import com.drunkencod.spice_road.platform.Services;
import com.drunkencod.spice_road.spice.Seasoning;

/**
 * Rules for combining Seasoning Effects and for what eating them does.
 */
public final class SeasoningEffects {

    private SeasoningEffects() {
    }

    /**
     * @param def        The catalog entry.
     * @param level      The stacked level.
     * @param multiplier The configured duration multiplier.
     * @return The duration in ticks, at least {@code 1}.
     */
    public static int durationTicks(SeasoningEffectDef def, int level, double multiplier) {
        long levels = def.scaling() == LevelScaling.DURATION ? level : 1;
        return (int) Math.max(1, Math.min(Integer.MAX_VALUE, Math.round(def.baseDuration() * levels * multiplier)));
    }

    /**
     * @param def   The catalog entry.
     * @param level The stacked level.
     * @return The mob effect amplifier.
     */
    public static int amplifier(SeasoningEffectDef def, int level) {
        return def.scaling() == LevelScaling.AMPLIFIER ? level - 1 : 0;
    }

    /**
     * @param def        The catalog entry.
     * @param level      The stacked level.
     * @param multiplier The configured duration multiplier.
     * @return The mob effect instance eating the effect applies.
     */
    public static MobEffectInstance toInstance(SeasoningEffectDef def, int level, double multiplier) {
        return new MobEffectInstance(def.effect(), durationTicks(def, level, multiplier), amplifier(def, level));
    }

    /**
     * Adds a gained effect to the ones a food already has. The same catalog
     * entry merges into one effect with the levels added, up to the entry's own
     * maximum. A new entry is refused once the food holds the maximum number of
     * distinct effects, unless it is forced (a landmine's bane can't be
     * refused), in which case it replaces the weakest boon, or the weakest
     * effect if there is no boon.
     *
     * @param current    The food's effects.
     * @param gained     The effect gained.
     * @param forced     Whether the effect can't be refused.
     * @param maxEffects The maximum number of distinct effects on one food.
     * @param catalog    Looks up catalog entries; effects without one count as
     *                   unlimited in level and as boons.
     * @return The food's effects after gaining {@code gained}.
     */
    public static List<SeasoningEffect> gain(List<SeasoningEffect> current, SeasoningEffect gained, boolean forced,
            int maxEffects, Function<ResourceLocation, Optional<SeasoningEffectDef>> catalog) {
        List<SeasoningEffect> result = new ArrayList<>(current);
        int maxLevel = catalog.apply(gained.id()).map(SeasoningEffectDef::maxLevel).orElse(Integer.MAX_VALUE);
        for (int i = 0; i < result.size(); i++) {
            if (result.get(i).id().equals(gained.id())) {
                int level = (int) Math.min(maxLevel, (long) result.get(i).level() + gained.level());
                result.set(i, new SeasoningEffect(gained.id(), level));
                return result;
            }
        }
        SeasoningEffect added = new SeasoningEffect(gained.id(), Math.min(maxLevel, gained.level()));
        if (result.size() < maxEffects) {
            result.add(added);
            return result;
        }
        if (!forced)
            return result;
        int weakest = weakestIndex(result, true, catalog);
        if (weakest < 0)
            weakest = weakestIndex(result, false, catalog);
        if (weakest < 0)
            return result;
        result.set(weakest, added);
        return result;
    }

    /**
     * Applies a seasoned food's effects to whoever ate it. Entries missing from
     * the catalog (e.g. removed by a datapack) are skipped.
     *
     * @param entity    The entity that ate the food.
     * @param seasoning The food's Seasoning.
     */
    public static void apply(LivingEntity entity, Seasoning seasoning) {
        double multiplier = Services.CONFIG.getSeasoningEffectDurationMultiplier();
        for (SeasoningEffect effect : seasoning.effects()) {
            SeasoningEffectRegistry.get(effect.id())
                    .ifPresent(def -> entity.addEffect(toInstance(def, effect.level(), multiplier)));
        }
    }

    /**
     * @return The index of the lowest-level effect, the last one among equals,
     *         counting only boons if {@code boonsOnly}; {@code -1} if none
     *         qualifies.
     */
    private static int weakestIndex(List<SeasoningEffect> effects, boolean boonsOnly,
            Function<ResourceLocation, Optional<SeasoningEffectDef>> catalog) {
        int weakest = -1;
        for (int i = 0; i < effects.size(); i++) {
            boolean isBoon = catalog.apply(effects.get(i).id()).map(def -> def.kind() == EffectKind.BOON).orElse(true);
            if (boonsOnly && !isBoon)
                continue;
            if (weakest < 0 || effects.get(i).level() <= effects.get(weakest).level())
                weakest = i;
        }
        return weakest;
    }
}
