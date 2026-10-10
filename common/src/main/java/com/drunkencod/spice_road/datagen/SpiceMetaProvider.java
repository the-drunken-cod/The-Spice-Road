package com.drunkencod.spice_road.datagen;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;
import java.util.concurrent.CompletableFuture;

import org.jetbrains.annotations.Nullable;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import net.minecraft.data.CachedOutput;
import net.minecraft.data.DataProvider;
import net.minecraft.data.PackOutput;

import com.drunkencod.spice_road.Constants;
import com.drunkencod.spice_road.spice.FlavorAxis;
import com.drunkencod.spice_road.spice.ProcessedSpice;
import com.drunkencod.spice_road.spice.Season;
import com.drunkencod.spice_road.spice.Spice;
import com.drunkencod.spice_road.spice.Tier;

/**
 * Writes {@code dev/spice_meta.json} at the repository root: the Flavor Axes,
 * Tiers and every Spice's and Processed Spice's growth metadata, for the
 * spice profile balancing tool. It's development data, so it's written outside
 * the pack output and never ends up in the mod jar.
 */
public class SpiceMetaProvider implements DataProvider {

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();

    private final Path outputFolder;

    /** @param output The pack output, used to locate the repository root. */
    public SpiceMetaProvider(PackOutput output) {
        this.outputFolder = output.getOutputFolder();
    }

    @Override
    public CompletableFuture<?> run(CachedOutput cachedOutput) {
        Path root = findRepoRoot(outputFolder);
        if (root == null) {
            Constants.LOG.warn("Skipping spice meta datagen, no settings.gradle found above {}", outputFolder);
            return CompletableFuture.completedFuture(null);
        }

        return CompletableFuture.runAsync(() -> {
            Path target = root.resolve("dev").resolve("spice_meta.json");
            String content = GSON.toJson(buildJson()) + "\n";
            try {
                if (Files.exists(target) && Files.readString(target, StandardCharsets.UTF_8).equals(content))
                    return;
                Files.createDirectories(target.getParent());
                Files.writeString(target, content, StandardCharsets.UTF_8);
            } catch (IOException e) {
                throw new IllegalStateException("Couldn't write " + target, e);
            }
        });
    }

    @Override
    public String getName() {
        return "Spice Meta (dev)";
    }

    // #region json

    /** @return The whole meta document. */
    private static JsonObject buildJson() {
        JsonObject json = new JsonObject();

        JsonArray axes = new JsonArray();
        for (FlavorAxis axis : FlavorAxis.values()) {
            JsonObject entry = new JsonObject();
            entry.addProperty("id", axis.getId());
            entry.addProperty("positive_color", hex(axis.getPositiveColor()));
            entry.addProperty("negative_color", hex(axis.getNegativeColor()));
            axes.add(entry);
        }
        json.add("axes", axes);

        JsonArray tiers = new JsonArray();
        for (Tier tier : Tier.values()) {
            JsonObject entry = new JsonObject();
            entry.addProperty("id", tier.getSerializedName());
            Integer color = tier.getRarity().color().getColor();
            entry.addProperty("color", hex(color != null ? color : 0xFFFFFF));
            tiers.add(entry);
        }
        json.add("tiers", tiers);

        JsonArray items = new JsonArray();
        for (Spice spice : Spice.values())
            items.add(itemJson(spice.getId(), spice, null));
        for (ProcessedSpice processed : ProcessedSpice.values())
            items.add(itemJson(processed.getId(), processed.getSource(), processed.getMethod().getId()));
        json.add("items", items);

        return json;
    }

    /**
     * @param path       The item's registry path.
     * @param spice      The Spice the item is, or is made from.
     * @param processing The processing method's ID, or {@code null} for a raw
     *                   item.
     * @return One item entry.
     */
    private static JsonObject itemJson(String path, Spice spice, @Nullable String processing) {
        JsonObject entry = new JsonObject();
        entry.addProperty("item", Constants.MOD_ID + ":" + path);
        entry.addProperty("spice", spice.getId());
        if (processing != null)
            entry.addProperty("processing", processing);
        entry.addProperty("tier", spice.getTier().getSerializedName());
        entry.addProperty("harvest_difficulty", spice.getHarvestDifficulty());
        entry.addProperty("climate", spice.getClimate().getId());
        JsonArray seasons = new JsonArray();
        for (Season season : spice.getSeasons())
            seasons.add(season.getId());
        entry.add("seasons", seasons);
        entry.addProperty("source_type", spice.getSourceType().name().toLowerCase(Locale.ROOT));
        entry.addProperty("harvest_action", spice.getHarvestAction().name().toLowerCase(Locale.ROOT));
        entry.addProperty("aquatic", spice.isAquatic());
        entry.addProperty("hand_pick", spice.requiresHandPick());
        return entry;
    }

    /**
     * @param rgb A color as {@code 0xRRGGBB}.
     * @return The color as {@code #rrggbb}.
     */
    private static String hex(int rgb) {
        return String.format(Locale.ROOT, "#%06x", rgb & 0xFFFFFF);
    }

    // #region paths

    /**
     * @param start The folder to start searching from.
     * @return The closest folder at or above {@code start} holding a
     *         {@code settings.gradle}, or {@code null} if there is none.
     */
    private static @Nullable Path findRepoRoot(Path start) {
        for (Path dir = start.toAbsolutePath(); dir != null; dir = dir.getParent()) {
            if (Files.isRegularFile(dir.resolve("settings.gradle")))
                return dir;
        }
        return null;
    }
}
