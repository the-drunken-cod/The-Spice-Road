package com.drunkencod.spice_road.datagen;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mojang.serialization.JsonOps;

import net.minecraft.data.CachedOutput;
import net.minecraft.data.DataProvider;
import net.minecraft.data.PackOutput;
import net.minecraft.resources.ResourceLocation;

import com.drunkencod.spice_road.Constants;
import com.drunkencod.spice_road.registry.ModVillagers;
import com.drunkencod.spice_road.spice.ProcessedSpice;
import com.drunkencod.spice_road.spice.Spice;
import com.drunkencod.spice_road.spice.Tier;
import com.drunkencod.spice_road.villager.trader.SpiceTrade;
import com.drunkencod.spice_road.villager.trader.SpiceTradeDirection;
import com.drunkencod.spice_road.villager.trader.SpiceTradeEntry;
import com.drunkencod.spice_road.villager.trader.SpiceTradeReloadListener;
import com.drunkencod.spice_road.villager.trader.SpiceTradeSource;
import com.drunkencod.spice_road.worldgen.village.VillageBiome;
import com.drunkencod.spice_road.worldgen.village.VillageHouseInjection;

/**
 * Writes the Spice Trader's data: its trade catalog, the loot tables that stock
 * the Spice Racks of its village houses, and the village house files that add
 * those houses to the vanilla template pools. Raw JSON, so it runs unchanged on
 * both loaders. The trade catalog is built and written through the real codec,
 * so datagen fails on an entry the game would reject.
 */
public class SpiceTraderProvider implements DataProvider {

    /** Weight of a Spice Trader house in its village style's pool; vanilla houses weigh 1 to 5. */
    private static final int HOUSE_WEIGHT = 3;

    /** Chance that a stocked rack also holds a Processed Spice. */
    private static final double PROCESSED_CHANCE = 0.1;

    /** Villager XP per trade, by villager level. */
    private static final int[] XP_BY_LEVEL = { 2, 5, 10, 15, 20 };

    /** Uses before an offer locks, by villager level. */
    private static final int[] USES_BY_LEVEL = { 16, 12, 10, 8, 8 };

    /**
     * Mix Presets cheap enough to be sold at the Apprentice level, by their
     * IDs. Their exact prices still come from the Tiers of their spices.
     */
    private static final List<ResourceLocation> CHEAP_PRESETS = List.of(
            ResourceLocation.fromNamespaceAndPath(Constants.MOD_ID, "salad"),
            ResourceLocation.fromNamespaceAndPath(Constants.MOD_ID, "mediterranean"),
            ResourceLocation.fromNamespaceAndPath(Constants.MOD_ID, "mexican"));

    /**
     * The vanilla point of interest type tags the Spice Trader's workstation
     * joins: jobless villagers only look for job sites in {@code acquirable_job_site},
     * and {@code village} marks where a village is (for raids and golems).
     * Neither loader adds modded types to them.
     */
    private static final List<String> POI_TAGS = List.of("acquirable_job_site", "village");

    private final PackOutput.PathProvider poiTagPathProvider;
    private final PackOutput.PathProvider tradePathProvider;
    private final PackOutput.PathProvider lootPathProvider;
    private final PackOutput.PathProvider housePathProvider;

    /** @param output The pack output to write into. */
    public SpiceTraderProvider(PackOutput output) {
        this.poiTagPathProvider = output.createPathProvider(PackOutput.Target.DATA_PACK,
                "tags/point_of_interest_type");
        this.tradePathProvider = output.createPathProvider(PackOutput.Target.DATA_PACK,
                SpiceTradeReloadListener.DIRECTORY);
        this.lootPathProvider = output.createPathProvider(PackOutput.Target.DATA_PACK, "loot_table");
        this.housePathProvider = output.createPathProvider(PackOutput.Target.DATA_PACK,
                VillageHouseInjection.DIRECTORY);
    }

    @Override
    public CompletableFuture<?> run(CachedOutput cachedOutput) {
        List<CompletableFuture<?>> writes = new ArrayList<>();
        // #region trades
        for (SpiceTrade trade : trades()) {
            JsonElement json = SpiceTrade.CODEC.encodeStart(JsonOps.INSTANCE, trade).getOrThrow(
                    error -> new IllegalStateException("Invalid Spice Trader trades: " + error));
            ResourceLocation id = ResourceLocation.fromNamespaceAndPath(Constants.MOD_ID,
                    trade.direction().getSerializedName() + "_" + trade.level());
            writes.add(DataProvider.saveStable(cachedOutput, json, tradePathProvider.json(id)));
        }
        // #region workstation tags
        for (String tag : POI_TAGS) {
            writes.add(DataProvider.saveStable(cachedOutput, poiTag(),
                    poiTagPathProvider.json(ResourceLocation.withDefaultNamespace(tag))));
        }
        // #region village houses
        for (VillageBiome biome : VillageBiome.values()) {
            ResourceLocation id = ResourceLocation.fromNamespaceAndPath(Constants.MOD_ID, biome.getId());
            writes.add(DataProvider.saveStable(cachedOutput, villageHouse(biome), housePathProvider.json(id)));
            writes.add(DataProvider.saveStable(cachedOutput, rackLootTable(biome),
                    lootPathProvider.json(biome.getRackLootTable())));
        }
        return CompletableFuture.allOf(writes.toArray(CompletableFuture[]::new));
    }

    /**
     * @return One file per villager level and direction: what the trader buys
     *         from level 1 to 5 (the loose spices of one Tier per level, and
     *         at all levels the Mix Presets no rarer than that Tier), and what
     *         it sells.
     */
    private static List<SpiceTrade> trades() {
        List<SpiceTrade> trades = new ArrayList<>();
        Tier[] tiers = Tier.values();
        for (int level = 1; level <= 5; level++) {
            int slot = level - 1;
            Tier buyTier = level == 5 ? Tier.EPIC : tiers[slot];
            List<Tier> buyTiers = level == 5 ? List.of(tiers) : List.of(buyTier);
            trades.add(new SpiceTrade(level, SpiceTradeDirection.BUY, List.of(
                    entry(slot, 2, new SpiceTradeSource.Spices(buyTiers, true)),
                    entry(slot, 1, new SpiceTradeSource.Presets(List.of(), buyTier)))));
        }
        SpiceTradeSource cheapPresets = new SpiceTradeSource.Presets(CHEAP_PRESETS, Tier.EPIC);
        SpiceTradeSource common = new SpiceTradeSource.Spices(List.of(Tier.COMMON), true);
        SpiceTradeSource uncommon = new SpiceTradeSource.Spices(List.of(Tier.UNCOMMON), true);
        trades.add(new SpiceTrade(1, SpiceTradeDirection.SELL, List.of(entry(0, 1, common))));
        trades.add(new SpiceTrade(2, SpiceTradeDirection.SELL, List.of(entry(1, 1, cheapPresets))));
        trades.add(new SpiceTrade(3, SpiceTradeDirection.SELL, List.of(entry(2, 1, uncommon))));
        trades.add(new SpiceTrade(4, SpiceTradeDirection.SELL, List.of(entry(3, 1, uncommon))));
        trades.add(new SpiceTrade(5, SpiceTradeDirection.SELL, List.of(
                entry(4, 2, common), entry(4, 2, uncommon), entry(4, 1, cheapPresets))));
        return trades;
    }

    /** @return A tag file adding the Spice Trader's workstation to a vanilla tag. */
    private static JsonObject poiTag() {
        JsonArray values = new JsonArray();
        values.add(ResourceLocation.fromNamespaceAndPath(Constants.MOD_ID, ModVillagers.SPICE_TRADER_ID).toString());
        JsonObject tag = new JsonObject();
        tag.addProperty("replace", false);
        tag.add("values", values);
        return tag;
    }

    private static SpiceTradeEntry entry(int slot, int weight, SpiceTradeSource source) {
        return new SpiceTradeEntry(weight, source, USES_BY_LEVEL[slot], XP_BY_LEVEL[slot]);
    }

    /**
     * @return The village house file adding the style's Spice Trader house to
     *         its template pool, in the format of {@code VillageHouse}.
     */
    private static JsonObject villageHouse(VillageBiome biome) {
        JsonObject element = new JsonObject();
        element.addProperty("element_type", "minecraft:legacy_single_pool_element");
        element.addProperty("location", biome.getHouseStructure().toString());
        element.addProperty("processors", biome.getProcessors());
        element.addProperty("projection", "rigid");
        JsonObject house = new JsonObject();
        house.addProperty("pool", biome.getPool().toString());
        house.addProperty("weight", HOUSE_WEIGHT);
        house.add("element", element);
        return house;
    }

    /**
     * @return A chest-type loot table for the house's rack: a few common and
     *         uncommon raw spices of the Climates of the village style, and a
     *         small chance at a Processed Spice of those Climates.
     */
    private static JsonObject rackLootTable(VillageBiome biome) {
        JsonArray raw = new JsonArray();
        for (Spice spice : Spice.values()) {
            if (!biome.getClimates().contains(spice.getClimate()) || spice.getTier().ordinal() > Tier.UNCOMMON.ordinal())
                continue;
            raw.add(lootEntry(spice.getId(), spice.getTier() == Tier.COMMON ? 3 : 1));
        }
        JsonArray pools = new JsonArray();
        pools.add(lootPool(uniform(3, 5), raw, null));

        JsonArray processed = new JsonArray();
        Arrays.stream(ProcessedSpice.values())
                .filter(spice -> biome.getClimates().contains(spice.getSource().getClimate()))
                .forEach(spice -> processed.add(lootEntry(spice.getId(), 1)));
        if (!processed.isEmpty()) {
            JsonObject chance = new JsonObject();
            chance.addProperty("condition", "minecraft:random_chance");
            chance.addProperty("chance", PROCESSED_CHANCE);
            pools.add(lootPool(constant(1), processed, chance));
        }

        JsonObject table = new JsonObject();
        table.addProperty("type", "minecraft:chest");
        table.add("pools", pools);
        return table;
    }

    private static JsonObject lootPool(JsonObject rolls, JsonArray entries, JsonObject condition) {
        JsonObject pool = new JsonObject();
        pool.add("rolls", rolls);
        pool.add("entries", entries);
        if (condition != null) {
            JsonArray conditions = new JsonArray();
            conditions.add(condition);
            pool.add("conditions", conditions);
        }
        return pool;
    }

    private static JsonObject lootEntry(String item, int weight) {
        JsonObject setCount = new JsonObject();
        setCount.addProperty("function", "minecraft:set_count");
        setCount.add("count", uniform(1, 3));
        JsonArray functions = new JsonArray();
        functions.add(setCount);
        JsonObject entry = new JsonObject();
        entry.addProperty("type", "minecraft:item");
        entry.addProperty("name", ResourceLocation.fromNamespaceAndPath(Constants.MOD_ID, item).toString());
        entry.addProperty("weight", weight);
        entry.add("functions", functions);
        return entry;
    }

    private static JsonObject uniform(int min, int max) {
        JsonObject provider = new JsonObject();
        provider.addProperty("type", "minecraft:uniform");
        provider.addProperty("min", min);
        provider.addProperty("max", max);
        return provider;
    }

    private static JsonObject constant(int value) {
        JsonObject provider = new JsonObject();
        provider.addProperty("type", "minecraft:constant");
        provider.addProperty("value", value);
        return provider;
    }

    @Override
    public String getName() {
        return "Spice Trader";
    }
}
