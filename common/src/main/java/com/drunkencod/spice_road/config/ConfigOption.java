package com.drunkencod.spice_road.config;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

/**
 * A single config option, declared once and consumed by both loaders: NeoForge
 * builds a {@code ModConfigSpec} entry from it, Fabric initializes and clamps
 * its backing field against it. Keeping defaults and ranges here is what stops
 * the two loaders from drifting apart.
 *
 * @param <T> The option's value type.
 */
public final class ConfigOption<T extends Comparable<T>> {

    /** Whether changing an option takes effect live, or needs a restart. */
    public enum Restart {
        /** Read live; no restart needed. */
        NONE,
        /** Affects an already generated world; needs the world to be reloaded. */
        WORLD,
        /** Read once during startup; needs the game to be restarted. */
        GAME
    }

    private final ConfigSection section;
    private final String key;
    private final Class<T> type;
    private final Supplier<T> defaultSupplier;
    private final T min;
    private final T max;
    private final Restart restart;

    private T memoizedDefault;

    private ConfigOption(ConfigSection section, String key, Class<T> type, Supplier<T> defaultSupplier, T min, T max,
            Restart restart) {
        this.section = section;
        this.key = key;
        this.type = type;
        this.defaultSupplier = defaultSupplier;
        this.min = min;
        this.max = max;
        this.restart = restart;
    }

    // #region Factories

    /**
     * Declares a boolean option.
     *
     * @param section      The section the option lives in.
     * @param key          The option's key within that section.
     * @param defaultValue The value a freshly written config gets.
     * @return The declared entry.
     */
    public static ConfigOption<Boolean> ofBoolean(ConfigSection section, String key, boolean defaultValue) {
        return new ConfigOption<>(section, key, Boolean.class, () -> defaultValue, null, null, Restart.NONE);
    }

    /**
     * Declares an integer option.
     *
     * @param section      The section the option lives in.
     * @param key          The option's key within that section.
     * @param defaultValue The value a freshly written config gets.
     * @param min          The lowest accepted value, inclusive.
     * @param max          The highest accepted value, inclusive.
     * @return The declared entry.
     */
    public static ConfigOption<Integer> ofInt(ConfigSection section, String key, int defaultValue, int min, int max) {
        return new ConfigOption<>(section, key, Integer.class, () -> defaultValue, min, max, Restart.NONE);
    }

    /**
     * Declares a double option.
     *
     * @param section      The section the option lives in.
     * @param key          The option's key within that section.
     * @param defaultValue The value a freshly written config gets.
     * @param min          The lowest accepted value, inclusive.
     * @param max          The highest accepted value, inclusive.
     * @return The declared entry.
     */
    public static ConfigOption<Double> ofDouble(ConfigSection section, String key, double defaultValue, double min,
            double max) {
        return new ConfigOption<>(section, key, Double.class, () -> defaultValue, min, max, Restart.NONE);
    }

    /**
     * Declares a double option whose default is computed rather than fixed. The
     * supplier is invoked at most once, and only once the option is first read,
     * so it may safely consult services that are still initializing.
     *
     * @param section         The section the option lives in.
     * @param key             The option's key within that section.
     * @param defaultSupplier Supplies the value a freshly written config gets.
     * @param min             The lowest accepted value, inclusive.
     * @param max             The highest accepted value, inclusive.
     * @return The declared entry.
     */
    public static ConfigOption<Double> ofDouble(ConfigSection section, String key, Supplier<Double> defaultSupplier,
            double min, double max) {
        return new ConfigOption<>(section, key, Double.class, defaultSupplier, min, max, Restart.NONE);
    }

    /**
     * Declares a long option whose default is computed rather than fixed. The
     * supplier is invoked at most once, so the default stays stable for the
     * lifetime of the game - see {@code ConfigSchema#REGION_SALT}.
     *
     * @param section         The section the option lives in.
     * @param key             The option's key within that section.
     * @param defaultSupplier Supplies the value a freshly written config gets.
     * @param min             The lowest accepted value, inclusive.
     * @param max             The highest accepted value, inclusive.
     * @return The declared entry.
     */
    public static ConfigOption<Long> ofLong(ConfigSection section, String key, Supplier<Long> defaultSupplier, long min,
            long max) {
        return new ConfigOption<>(section, key, Long.class, defaultSupplier, min, max, Restart.NONE);
    }

    /**
     * Returns a copy of this entry that needs the given kind of restart to take
     * effect.
     *
     * @param value The restart requirement to apply.
     * @return The adjusted entry.
     */
    public ConfigOption<T> restart(Restart value) {
        return new ConfigOption<>(section, key, type, defaultSupplier, min, max, value);
    }

    // #region Accessors

    /** @return The section this option lives in. */
    public ConfigSection getSection() {
        return section;
    }

    /** @return The option's key within its section, e.g. {@code "cellScale"}. */
    public String getKey() {
        return key;
    }

    /** @return The option's value type. */
    public Class<T> getType() {
        return type;
    }

    /** @return The value a freshly written config gets, computed at most once. */
    public synchronized T getDefault() {
        if (memoizedDefault == null) {
            memoizedDefault = defaultSupplier.get();
        }
        return memoizedDefault;
    }

    /** @return The lowest accepted value, or {@code null} if unbounded. */
    public T getMin() {
        return min;
    }

    /** @return The highest accepted value, or {@code null} if unbounded. */
    public T getMax() {
        return max;
    }

    /** @return What kind of restart a change to this option needs. */
    public Restart getRestart() {
        return restart;
    }

    /**
     * @return The section path plus this option's key, e.g.
     *         {@code ["cultivation", "growthSpeed", "common"]}.
     */
    public List<String> getPath() {
        List<String> path = new ArrayList<>(section.getPath());
        path.add(key);
        return path;
    }

    /**
     * @return The dotted path of this option, e.g.
     *         {@code "cultivation.growthSpeed.common"}.
     */
    public String getDottedPath() {
        return String.join(".", getPath());
    }

    /**
     * The translation key of this option's label. Its tooltip, config-file
     * comment and world-restart warning are derived from it - see
     * {@link ConfigText}.
     *
     * @return The option's translation key, e.g.
     *         {@code "spice_road.configuration.cultivation.growthSpeed.common"}.
     */
    public String getTranslationKey() {
        return section.getTranslationKey() + "." + key;
    }

    /**
     * Constrains a value to this option's range. Needed on Fabric, where
     * AutoConfig enforces no bounds of its own; NeoForge rejects out-of-range
     * values through the spec instead.
     *
     * @param value The value read from the config file.
     * @return The value, moved inside the range if it fell outside it.
     */
    public T clamp(T value) {
        if (value == null) {
            return getDefault();
        }
        if (min != null && value.compareTo(min) < 0) {
            return min;
        }
        if (max != null && value.compareTo(max) > 0) {
            return max;
        }
        return value;
    }

    @Override
    public String toString() {
        return getDottedPath();
    }
}
