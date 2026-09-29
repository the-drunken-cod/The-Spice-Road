package com.drunkencod.spice_road.config;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import com.drunkencod.spice_road.Constants;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

/**
 * Supplies the English prose shown for config options: the comments written
 * into the config files themselves, and the fallback text behind the in-game
 * screens.
 * <p>
 * The strings are parsed straight out of {@code lang/en_us.json} on the
 * classpath rather than looked up through {@code Component.translatable},
 * because config specs are built during mod construction - long before any
 * language file is loaded, and on a dedicated server the language table is
 * never populated at all. Config files are therefore always English; only the
 * in-game screens localize.
 */
public final class ConfigText {

    private static final String LANG_PATH = "/assets/" + Constants.MOD_ID + "/lang/en_us.json";

    private static final Set<String> REPORTED_MISSING = ConcurrentHashMap.newKeySet();

    private static Map<String, String> strings;

    private ConfigText() {
    }

    /**
     * The comment written above an option or section in the config file. Falls
     * back to the in-game tooltip, since the two are usually the same text; a
     * {@code .comment} key exists only for the few entries whose file comment
     * is deliberately different from what players should see in a tooltip.
     *
     * @param translationKey The option's or section's translation key.
     * @return The comment, or an empty string if neither key is present.
     */
    public static String comment(String translationKey) {
        String override = get(translationKey + ".comment");
        if (!override.isEmpty()) {
            return override;
        }
        String tooltip = get(translationKey + ".tooltip");
        if (tooltip.isEmpty() && REPORTED_MISSING.add(translationKey)) {
            // The build checks that every authored key has its counterpart, but it
            // cannot know about a schema option nobody wrote a string for at all.
            Constants.LOG.error("Config option {} has no description in {}", translationKey, LANG_PATH);
        }
        return tooltip;
    }

    /**
     * The extra caution line shown for options whose change affects an existing
     * world. NeoForge's config screen reads the same key on its own.
     *
     * @param translationKey The option's translation key.
     * @return The warning, or an empty string if the option has none.
     */
    public static String warning(String translationKey) {
        return get(translationKey + ".warning");
    }

    /**
     * @param key The full translation key.
     * @return The English string for that key, or an empty string if absent.
     */
    public static String get(String key) {
        return load().getOrDefault(key, "");
    }

    /**
     * @param key The full translation key.
     * @return Whether the language file defines that key.
     */
    public static boolean has(String key) {
        return load().containsKey(key);
    }

    private static synchronized Map<String, String> load() {
        if (strings != null) {
            return strings;
        }
        Map<String, String> parsed = new LinkedHashMap<>();
        try (InputStream in = ConfigText.class.getResourceAsStream(LANG_PATH)) {
            if (in == null) {
                Constants.LOG.error("Config comments unavailable: {} is missing from the classpath", LANG_PATH);
            } else {
                JsonObject json = new Gson().fromJson(new InputStreamReader(in, StandardCharsets.UTF_8),
                        JsonObject.class);
                for (Map.Entry<String, JsonElement> entry : json.entrySet()) {
                    if (entry.getValue().isJsonPrimitive()) {
                        parsed.put(entry.getKey(), entry.getValue().getAsString());
                    }
                }
            }
        } catch (IOException | RuntimeException e) {
            Constants.LOG.error("Config comments unavailable: failed to read {}", LANG_PATH, e);
        }
        strings = Collections.unmodifiableMap(parsed);
        return strings;
    }
}
