package com.drunkencod.spice_road.config;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.ModContainer;
import net.neoforged.neoforge.common.ModConfigSpec;

import com.drunkencod.spice_road.compat.CompatRecipeMod;
import com.drunkencod.spice_road.spice.Tier;

/**
 * NeoForge implementation of {@link IConfigHelper}, building one
 * {@link ModConfigSpec} per {@link ConfigFile} out of {@link ConfigSchema}.
 * <p>
 * Every option's default, range, restart requirement and translation key comes
 * from the schema, and its comment from {@link ConfigText}, so nothing about an
 * option is restated here.
 */
public class NeoForgeConfigHelper implements IConfigHelper {

    private static final Map<ConfigOption<?>, ModConfigSpec.ConfigValue<?>> VALUES = new HashMap<>();
    private static final Map<ConfigFile, ModConfigSpec> SPECS = new HashMap<>();

    static {
        for (ConfigFile file : ConfigFile.values()) {
            SPECS.put(file, buildSpec(file));
        }
    }

    // #region Registration

    /**
     * Registers every config spec. Must be called from the NeoForge mod
     * constructor with the injected {@link ModContainer}, so configs are loaded
     * before the world is.
     *
     * @param modContainer The mod's container.
     */
    public void register(ModContainer modContainer) {
        modContainer.registerConfig(ModConfig.Type.SERVER, SPECS.get(ConfigFile.SERVER));
        modContainer.registerConfig(ModConfig.Type.COMMON, SPECS.get(ConfigFile.COMMON));
        modContainer.registerConfig(ModConfig.Type.CLIENT, SPECS.get(ConfigFile.CLIENT));
    }

    // #region Spec building

    private static ModConfigSpec buildSpec(ConfigFile file) {
        ModConfigSpec.Builder builder = new ModConfigSpec.Builder();
        for (ConfigSection section : rootsOf(file)) {
            appendSection(builder, file, section);
        }
        return builder.build();
    }

    /** Emits one section, its options, and then its subsections, in schema order. */
    private static void appendSection(ModConfigSpec.Builder builder, ConfigFile file, ConfigSection section) {
        builder.comment(ConfigText.comment(section.getTranslationKey()))
                .translation(section.getTranslationKey())
                .push(section.getName());
        for (ConfigOption<?> entry : ConfigSchema.options(file)) {
            if (entry.getSection() == section) {
                VALUES.put(entry, define(builder, entry));
            }
        }
        for (ConfigSection child : childrenOf(file, section)) {
            appendSection(builder, file, child);
        }
        builder.pop();
    }

    private static List<ConfigSection> rootsOf(ConfigFile file) {
        return ConfigSchema.sections(file).stream().filter(section -> section.getParent() == null).toList();
    }

    private static List<ConfigSection> childrenOf(ConfigFile file, ConfigSection parent) {
        List<ConfigSection> children = new ArrayList<>();
        for (ConfigSection section : ConfigSchema.sections(file)) {
            if (section.getParent() == parent) {
                children.add(section);
            }
        }
        return children;
    }

    private static ModConfigSpec.ConfigValue<?> define(ModConfigSpec.Builder builder, ConfigOption<?> entry) {
        applyMetadata(builder, entry);
        if (entry.getType() == Boolean.class) {
            // Unboxed on purpose: define(String, T) would otherwise win overload
            // resolution over define(String, boolean), returning a plain ConfigValue
            // that NeoForge's built-in config screen can't render an input for.
            return builder.define(entry.getKey(), ((Boolean) entry.getDefault()).booleanValue());
        }
        return defineInRange(builder, entry);
    }

    /**
     * Defines a numeric option through the primitive overloads on purpose: the generic
     * {@code defineInRange(..., Class)} overload hands back whatever boxed type the TOML
     * parser produced, so a small {@code long} comes back as an {@code Integer} and
     * fails the cast in {@link #value}.
     */
    private static ModConfigSpec.ConfigValue<?> defineInRange(ModConfigSpec.Builder builder, ConfigOption<?> entry) {
        String key = entry.getKey();
        Class<?> type = entry.getType();
        if (type == Long.class) {
            return builder.defineInRange(key, (Long) entry.getDefault(), (Long) entry.getMin(),
                    (Long) entry.getMax());
        }
        if (type == Integer.class) {
            return builder.defineInRange(key, (Integer) entry.getDefault(), (Integer) entry.getMin(),
                    (Integer) entry.getMax());
        }
        if (type == Double.class) {
            return builder.defineInRange(key, (Double) entry.getDefault(), (Double) entry.getMin(),
                    (Double) entry.getMax());
        }
        throw new IllegalArgumentException("Unsupported config option type " + type.getName() + " for " + key);
    }

    /**
     * Applies the comment, translation key and restart requirement that the
     * next {@code define} call picks up. The comment carries no range or
     * default, since the TOML writer appends those itself.
     */
    private static void applyMetadata(ModConfigSpec.Builder builder, ConfigOption<?> entry) {
        String warning = ConfigText.warning(entry.getTranslationKey());
        String comment = ConfigText.comment(entry.getTranslationKey());
        if (warning.isEmpty()) {
            builder.comment(comment);
        } else {
            builder.comment(comment, warning);
        }
        builder.translation(entry.getTranslationKey());
        switch (entry.getRestart()) {
            case WORLD -> builder.worldRestart();
            case GAME -> builder.gameRestart();
            case NONE -> {
            }
        }
    }

    private static <T extends Comparable<T>> T value(ConfigOption<T> entry) {
        return entry.getType().cast(VALUES.get(entry).get());
    }

    // #region IConfigHelper implementation

    @Override
    public double getSpiceRegionCellScale() {
        return value(ConfigSchema.REGION_CELL_SCALE);
    }

    @Override
    public long getSpiceRegionSalt() {
        return value(ConfigSchema.REGION_SALT);
    }

    @Override
    public double getSpiceRegionClusteringStrength() {
        return value(ConfigSchema.REGION_CLUSTERING_STRENGTH);
    }

    @Override
    public double getSpiceRegionSpicelessChance() {
        return value(ConfigSchema.REGION_SPICELESS_CHANCE);
    }

    @Override
    public double getSpiceRegionTierWeight(Tier tier) {
        return value(ConfigSchema.REGION_TIER_WEIGHT.get(tier));
    }

    @Override
    public int getSpiceMapSearchRadiusCells() {
        return value(ConfigSchema.SPICE_MAP_SEARCH_RADIUS_CELLS);
    }

    @Override
    public int getSpiceMapVillagerSearchRadiusCells() {
        return value(ConfigSchema.SPICE_MAP_VILLAGER_SEARCH_RADIUS_CELLS);
    }

    @Override
    public boolean isSpiceMapTradesEnabled() {
        return value(ConfigSchema.SPICE_MAP_TRADES_ENABLED);
    }

    @Override
    public int getSpiceMapBasePrice() {
        return value(ConfigSchema.SPICE_MAP_BASE_PRICE);
    }

    @Override
    public double getSpiceMapPriceMultiplier(Tier tier) {
        return value(ConfigSchema.SPICE_MAP_PRICE_MULTIPLIER.get(tier));
    }

    @Override
    public boolean isSpiceMapLootEnabled() {
        return value(ConfigSchema.SPICE_MAP_LOOT_ENABLED);
    }

    @Override
    public boolean isSpiceRegionPlantingRestricted() {
        return value(ConfigSchema.CULTIVATION_REGION_PLANTING_RESTRICTED);
    }

    @Override
    public int getSpiceHardyHarvestDifficulty() {
        return value(ConfigSchema.CULTIVATION_HARDY_HARVEST_DIFFICULTY);
    }

    @Override
    public boolean isAquaticSpiceWaterRequired() {
        return value(ConfigSchema.CULTIVATION_AQUATIC_REQUIRES_WATER);
    }

    @Override
    public boolean isAquaticSpiceFlowThroughEnabled() {
        return value(ConfigSchema.CULTIVATION_AQUATIC_FLOW_THROUGH);
    }

    @Override
    public boolean isUnripeSpicePollinationAllowed() {
        return value(ConfigSchema.CULTIVATION_BEES_POLLINATE_UNRIPE);
    }

    @Override
    public double getSpicePlantHarvestYieldMultiplier() {
        return value(ConfigSchema.PLANT_HARVEST_YIELD_MULTIPLIER);
    }

    @Override
    public double getSpicePlantGrowthSpeedMultiplier(Tier tier) {
        return value(ConfigSchema.CULTIVATION_GROWTH_SPEED_MULTIPLIER.get(tier));
    }

    @Override
    public double getSpiceTreeHarvestYieldMultiplier() {
        return value(ConfigSchema.TREE_HARVEST_YIELD_MULTIPLIER);
    }

    @Override
    public double getSpiceTreeFruitingLeavesChance(Tier tier) {
        return value(ConfigSchema.TREE_FRUITING_LEAVES_CHANCE.get(tier));
    }

    @Override
    public double getSpiceVineRipeningSegmentsChance(Tier tier) {
        return value(ConfigSchema.VINE_RIPENING_SEGMENTS_CHANCE.get(tier));
    }

    @Override
    public int getHarvestLuckMaxLevel() {
        return value(ConfigSchema.HARVEST_LUCK_MAX_LEVEL);
    }

    @Override
    public double getHarvestLuckYieldBonus() {
        return value(ConfigSchema.HARVEST_LUCK_YIELD_BONUS);
    }

    @Override
    public double getHarvestLuckSeedChanceBonus() {
        return value(ConfigSchema.HARVEST_LUCK_SEED_CHANCE_BONUS);
    }

    @Override
    public double getFlavorSoftCap() {
        return value(ConfigSchema.FLAVOR_SOFT_CAP);
    }

    @Override
    public double getFlavorMinimumAxisValue() {
        return value(ConfigSchema.FLAVOR_MINIMUM_AXIS_VALUE);
    }

    @Override
    public int getSeasoningMaxEffects() {
        return value(ConfigSchema.SEASONING_MAX_EFFECTS);
    }

    @Override
    public double getSeasoningEffectDurationMultiplier() {
        return value(ConfigSchema.SEASONING_EFFECT_DURATION_MULTIPLIER);
    }

    @Override
    public long getSeasoningBoardSalt() {
        return value(ConfigSchema.SEASONING_BOARD_SALT);
    }

    @Override
    public double getSeasoningStepCost() {
        return value(ConfigSchema.SEASONING_STEP_COST);
    }

    @Override
    public double getSeasoningLockInCost(int ring) {
        return switch (ring) {
            case 2 -> value(ConfigSchema.SEASONING_LOCK_IN_COST_RING_2);
            case 3 -> value(ConfigSchema.SEASONING_LOCK_IN_COST_RING_3);
            default -> value(ConfigSchema.SEASONING_LOCK_IN_COST_RING_4);
        };
    }

    @Override
    public int getSeasoningMaxSpices() {
        return value(ConfigSchema.SEASONING_MAX_SPICES);
    }

    @Override
    public int getSeasoningMaxSpicesPerKind() {
        return value(ConfigSchema.SEASONING_MAX_SPICES_PER_KIND);
    }

    @Override
    public int getSeasoningStorageRadius() {
        return value(ConfigSchema.SEASONING_STORAGE_RADIUS);
    }

    @Override
    public int getSeasoningDiscoveryLimit() {
        return value(ConfigSchema.SEASONING_DISCOVERY_LIMIT);
    }

    @Override
    public boolean getSeasoningBypassSaturationCap() {
        return value(ConfigSchema.SEASONING_BYPASS_SATURATION_CAP);
    }

    @Override
    public double getSeasoningSaturationOvercap() {
        return value(ConfigSchema.SEASONING_SATURATION_OVERCAP);
    }

    @Override
    public double getSeasoningParticipationMinSaturation() {
        return value(ConfigSchema.SEASONING_PARTICIPATION_MIN_SATURATION);
    }

    @Override
    public double getSeasoningParticipationMaxSaturation() {
        return value(ConfigSchema.SEASONING_PARTICIPATION_MAX_SATURATION);
    }

    @Override
    public double getSeasoningParticipationFullDose() {
        return value(ConfigSchema.SEASONING_PARTICIPATION_FULL_DOSE);
    }

    @Override
    public double getSeasoningDiversityMinSaturation() {
        return value(ConfigSchema.SEASONING_DIVERSITY_MIN_SATURATION);
    }

    @Override
    public double getSeasoningDiversityMaxSaturation() {
        return value(ConfigSchema.SEASONING_DIVERSITY_MAX_SATURATION);
    }

    @Override
    public int getSeasoningDiversityFullDiversity() {
        return value(ConfigSchema.SEASONING_DIVERSITY_FULL_DIVERSITY);
    }

    @Override
    public int getGrinderInputBufferMs() {
        return value(ConfigSchema.GRINDER_INPUT_BUFFER_MS);
    }

    @Override
    public boolean isTooltipBothAxisLabelsShown() {
        return value(ConfigSchema.TOOLTIP_SHOW_BOTH_AXIS_LABELS);
    }

    @Override
    public boolean isTooltipAxisValueShown() {
        return value(ConfigSchema.TOOLTIP_SHOW_AXIS_VALUES);
    }

    @Override
    public boolean isTooltipShiftBypassed() {
        return value(ConfigSchema.TOOLTIP_ALWAYS_SHOW_SHIFT_CONTENT);
    }

    @Override
    public boolean isTooltipAnimated() {
        return value(ConfigSchema.TOOLTIP_ANIMATE_AXES);
    }

    @Override
    public int getTooltipAnimationDurationMs() {
        return value(ConfigSchema.TOOLTIP_ANIMATION_DURATION_MS);
    }

    @Override
    public boolean isSneakRequiredToPlaceFlavoredFood() {
        return value(ConfigSchema.COMPAT_SNEAK_TO_PLACE_FLAVORED_FOOD);
    }

    @Override
    public boolean isCompatRecipeEnabled(CompatRecipeMod mod) {
        return value(ConfigSchema.COMPAT_RECIPES_ENABLED.get(mod));
    }

    @Override
    public int getStatsFoundScanIntervalTicks() {
        return value(ConfigSchema.STATS_FOUND_SCAN_INTERVAL_TICKS);
    }
}
