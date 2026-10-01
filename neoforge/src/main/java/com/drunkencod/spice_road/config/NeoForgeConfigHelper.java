package com.drunkencod.spice_road.config;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.ModContainer;
import net.neoforged.neoforge.common.ModConfigSpec;

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

    private static <T extends Comparable<T>> ModConfigSpec.ConfigValue<T> defineInRange(ModConfigSpec.Builder builder,
            ConfigOption<T> entry) {
        return builder.defineInRange(entry.getKey(), entry.getDefault(), entry.getMin(), entry.getMax(),
                entry.getType());
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
    public double getCookingVarianceMin() {
        return value(ConfigSchema.COOKING_VARIANCE_MIN);
    }

    @Override
    public double getCookingVarianceMax() {
        return value(ConfigSchema.COOKING_VARIANCE_MAX);
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
    public double getSufficientlySeasoned() {
        return value(ConfigSchema.FLAVOR_SUFFICIENTLY_SEASONED);
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
    public boolean isSneakRequiredToPlaceFlavoredFood() {
        return value(ConfigSchema.COMPAT_SNEAK_TO_PLACE_FLAVORED_FOOD);
    }
}
