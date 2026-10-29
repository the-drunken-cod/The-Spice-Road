package com.drunkencod.spice_road.villager.trader;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.mojang.serialization.JsonOps;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;

import com.drunkencod.spice_road.Constants;

/**
 * Loads {@code data/<namespace>/spice_trade/*.json} into the
 * {@link SpiceTradeCatalog}. A malformed file is logged and skipped rather than
 * failing the whole reload.
 */
public class SpiceTradeReloadListener extends SimpleJsonResourceReloadListener {

    /** Datapack directory the catalog files are loaded from. */
    public static final String DIRECTORY = "spice_trade";
    /** ID of this reload listener. */
    public static final ResourceLocation ID = ResourceLocation.fromNamespaceAndPath(Constants.MOD_ID, DIRECTORY);

    /** Creates the listener for the {@link #DIRECTORY} directory. */
    public SpiceTradeReloadListener() {
        super(new Gson(), DIRECTORY);
    }

    @Override
    protected void apply(Map<ResourceLocation, JsonElement> object, ResourceManager resourceManager,
            ProfilerFiller profiler) {
        List<SpiceTrade> trades = new ArrayList<>(object.size());
        object.entrySet().stream()
                .sorted(Map.Entry.comparingByKey(Comparator.comparing(ResourceLocation::toString)))
                .forEach(entry -> SpiceTrade.CODEC.parse(JsonOps.INSTANCE, entry.getValue())
                        .resultOrPartial(error -> Constants.LOG.error("Couldn't load Spice Trader trades {}: {}",
                                entry.getKey(), error))
                        .ifPresent(trades::add));
        SpiceTradeCatalog.set(trades);
        Constants.LOG.info("Loaded {} Spice Trader trade files", trades.size());
    }
}
