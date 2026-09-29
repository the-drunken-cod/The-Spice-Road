package com.drunkencod.spice_road.config;

import java.util.ArrayList;
import java.util.List;

import com.drunkencod.spice_road.Constants;

/**
 * A section of a config file - a TOML {@code [table]} on NeoForge, a nested
 * JSON object on Fabric. Sections may nest; every config entry lives in one,
 * so no key is ever defined at file level.
 */
public final class ConfigSection {

    private final ConfigFile file;
    private final ConfigSection parent;
    private final String name;

    private ConfigSection(ConfigFile file, ConfigSection parent, String name) {
        this.file = file;
        this.parent = parent;
        this.name = name;
    }

    /**
     * Creates a top-level section of the given file.
     *
     * @param file The config file this section lives in.
     * @param name The section's key, e.g. {@code "region"}.
     * @return The new section.
     */
    public static ConfigSection root(ConfigFile file, String name) {
        return new ConfigSection(file, null, name);
    }

    /**
     * Creates a section nested inside this one.
     *
     * @param name The subsection's key, e.g. {@code "growthSpeed"}.
     * @return The new subsection.
     */
    public ConfigSection child(String name) {
        return new ConfigSection(file, this, name);
    }

    /** @return The config file this section belongs to. */
    public ConfigFile getFile() {
        return file;
    }

    /** @return The enclosing section, or {@code null} for a top-level one. */
    public ConfigSection getParent() {
        return parent;
    }

    /** @return This section's own key, without any enclosing sections. */
    public String getName() {
        return name;
    }

    /**
     * @return The section keys from the file root down to and including this
     *         one, e.g. {@code ["spiceMap", "trades", "priceMultiplier"]}.
     */
    public List<String> getPath() {
        List<String> path = parent == null ? new ArrayList<>() : new ArrayList<>(parent.getPath());
        path.add(name);
        return path;
    }

    /**
     * @return The dotted path of this section, e.g.
     *         {@code "spiceMap.trades.priceMultiplier"}.
     */
    public String getDottedPath() {
        return String.join(".", getPath());
    }

    /**
     * The translation key of this section's label. Its tooltip and the comment
     * written above the section in the config file are derived from it - see
     * {@link ConfigText}.
     *
     * @return The section's translation key, e.g.
     *         {@code "spice_road.configuration.spiceMap.trades"}.
     */
    public String getTranslationKey() {
        return Constants.MOD_ID + ".configuration." + getDottedPath();
    }

    @Override
    public String toString() {
        return getDottedPath();
    }
}
