package com.drunkencod.spice_road.config;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.drunkencod.spice_road.Constants;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;

import me.shedaniel.autoconfig.ConfigData;
import me.shedaniel.autoconfig.annotation.Config;
import me.shedaniel.autoconfig.serializer.ConfigSerializer;
import me.shedaniel.autoconfig.util.Utils;
import net.fabricmc.loader.api.FabricLoader;

/**
 * Writes a Fabric config file as JSON with a {@code "<key>._comment_"} line
 * before every option and section, so the file documents itself the way
 * NeoForge's TOML does. JSON has no comment syntax, and TOML cannot express
 * these as data at all - a key cannot be both a value and a table - which is
 * why Fabric stays on JSON.
 * <p>
 * Comments are not fields on the config class: they are generated from
 * {@link ConfigText} on every write and stripped again on every read, so they
 * always reflect the current wording rather than whatever was on disk.
 *
 * @param <T> The config class being serialized.
 */
public class CommentedJsonConfigSerializer<T extends ConfigData> implements ConfigSerializer<T> {

    /** Key suffix marking a generated comment line. */
    private static final String COMMENT_SUFFIX = "._comment_";

    private final Config definition;
    private final Class<T> configClass;
    private final ConfigFile file;
    private final Gson gson = new GsonBuilder().setPrettyPrinting().serializeNulls().create();

    /**
     * @param definition  The config class' {@link Config} annotation.
     * @param configClass The config class being serialized.
     * @param file        The schema file whose options back that class.
     */
    public CommentedJsonConfigSerializer(Config definition, Class<T> configClass, ConfigFile file) {
        this.definition = definition;
        this.configClass = configClass;
        this.file = file;
    }

    @Override
    public void serialize(T config) throws SerializationException {
        Path path = getConfigPath();
        try {
            Files.createDirectories(path.getParent());
            JsonObject plain = gson.toJsonTree(config).getAsJsonObject();
            Files.writeString(path, gson.toJson(withComments(plain, "")), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new SerializationException(e);
        }
    }

    @Override
    public T deserialize() throws SerializationException {
        Path path = getConfigPath();
        if (!Files.exists(path)) {
            return createDefault();
        }
        try {
            JsonObject stored = JsonParser.parseString(Files.readString(path, StandardCharsets.UTF_8))
                    .getAsJsonObject();
            return gson.fromJson(stripComments(stored), configClass);
        } catch (IOException | JsonParseException | IllegalStateException e) {
            Constants.LOG.error("Could not read {}, falling back to defaults", path, e);
            return createDefault();
        }
    }

    @Override
    public T createDefault() {
        return Utils.constructUnsafely(configClass);
    }

    private Path getConfigPath() {
        return FabricLoader.getInstance().getConfigDir().resolve(definition.name() + ".json");
    }

    // #region Comment handling

    /**
     * Copies a config tree, inserting a generated comment line before every key
     * that the schema knows about.
     *
     * @param source The plain serialized config tree.
     * @param prefix The dotted path of {@code source} within the config file.
     * @return A new tree with comment lines interleaved.
     */
    private JsonObject withComments(JsonObject source, String prefix) {
        JsonObject commented = new JsonObject();
        for (Map.Entry<String, JsonElement> member : source.entrySet()) {
            String key = member.getKey();
            String path = prefix.isEmpty() ? key : prefix + "." + key;
            String comment = commentFor(path);
            if (!comment.isEmpty()) {
                commented.addProperty(key + COMMENT_SUFFIX, comment);
            }
            if (member.getValue().isJsonObject()) {
                commented.add(key, withComments(member.getValue().getAsJsonObject(), path));
            } else {
                commented.add(key, member.getValue());
            }
        }
        return commented;
    }

    /**
     * Builds the comment for one path. Options get their default and range
     * appended, matching the {@code # Default:}/{@code # Range:} lines NeoForge
     * writes into its own config files.
     *
     * @param path The dotted path of an option or section.
     * @return The comment, or an empty string if the schema has no such path.
     */
    private String commentFor(String path) {
        ConfigSection section = ConfigSchema.section(file, path);
        if (section != null) {
            return ConfigText.comment(section.getTranslationKey());
        }
        ConfigOption<?> entry = ConfigSchema.option(file, path);
        if (entry == null) {
            return "";
        }
        List<String> parts = new ArrayList<>();
        String prose = ConfigText.comment(entry.getTranslationKey());
        if (!prose.isEmpty()) {
            parts.add(prose);
        }
        String warning = ConfigText.warning(entry.getTranslationKey());
        if (!warning.isEmpty()) {
            parts.add(warning);
        }
        parts.add(metadataOf(entry));
        return String.join(" ", parts);
    }

    private static String metadataOf(ConfigOption<?> entry) {
        String defaults = "Default: " + entry.getDefault();
        if (entry.getMin() == null || entry.getMax() == null) {
            return "(" + defaults + ")";
        }
        return "(" + defaults + ", Range: " + entry.getMin() + " ~ " + entry.getMax() + ")";
    }

    /**
     * Removes every generated comment line, so they are never bound to a field
     * and can never persist stale wording.
     *
     * @param source The config tree as read from disk.
     * @return A new tree holding only real config keys.
     */
    private static JsonObject stripComments(JsonObject source) {
        JsonObject stripped = new JsonObject();
        for (Map.Entry<String, JsonElement> member : source.entrySet()) {
            if (member.getKey().endsWith(COMMENT_SUFFIX)) {
                continue;
            }
            if (member.getValue().isJsonObject()) {
                stripped.add(member.getKey(), stripComments(member.getValue().getAsJsonObject()));
            } else {
                stripped.add(member.getKey(), member.getValue());
            }
        }
        return stripped;
    }
}
