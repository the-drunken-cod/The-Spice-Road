package com.drunkencod.spice_road.worldgen.village;

import java.io.IOException;
import java.io.Reader;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.mojang.serialization.JsonOps;

import net.minecraft.core.Registry;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.FileToIdConverter;
import net.minecraft.resources.RegistryOps;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.world.level.levelgen.structure.pools.StructureTemplatePool;

import com.drunkencod.spice_road.Constants;
import com.drunkencod.spice_road.platform.Services;

/**
 * Adds every {@link VillageHouse} to its template pool when a server starts.
 * Vanilla offers no way to append to a pool, only to replace it, so the pools
 * are changed in memory after they load. Both loaders call {@link #inject} from
 * their server-starting event.
 */
public final class VillageHouseInjection {

    /** Datapack directory the village house files are read from. */
    public static final String DIRECTORY = "village_house";

    /**
     * The house files already added to each pool. A pool's weights are baked in
     * as repeated elements, so adding a house twice would double its weight.
     */
    private static final Map<StructureTemplatePool, Set<ResourceLocation>> INJECTED = new WeakHashMap<>();

    private VillageHouseInjection() {
    }

    /**
     * Adds the houses to the server's template pools, unless the config turns
     * them off. A malformed file, or one naming a pool that doesn't exist, is
     * logged and skipped.
     *
     * @param server The starting server.
     */
    public static void inject(MinecraftServer server) {
        if (!Services.CONFIG.isSpiceTraderVillageHousesEnabled())
            return;
        RegistryAccess registries = server.registryAccess();
        Registry<StructureTemplatePool> pools = registries.registryOrThrow(Registries.TEMPLATE_POOL);
        RegistryOps<JsonElement> ops = RegistryOps.create(JsonOps.INSTANCE, registries);
        Map<ResourceLocation, Resource> files = FileToIdConverter.json(DIRECTORY)
                .listMatchingResources(server.getResourceManager());
        int added = 0;
        for (Map.Entry<ResourceLocation, Resource> file : files.entrySet().stream()
                .sorted(Map.Entry.comparingByKey(Comparator.comparing(ResourceLocation::toString))).toList()) {
            if (add(file.getKey(), file.getValue(), pools, ops))
                added++;
        }
        Constants.LOG.info("Added {} village houses to the template pools", added);
    }

    /**
     * @return Whether the house was added to its pool.
     */
    private static boolean add(ResourceLocation id, Resource resource, Registry<StructureTemplatePool> pools,
            RegistryOps<JsonElement> ops) {
        try (Reader reader = resource.openAsReader()) {
            VillageHouse house = VillageHouse.CODEC.parse(ops, JsonParser.parseReader(reader))
                    .getOrThrow(IllegalStateException::new);
            StructureTemplatePool pool = pools.get(house.pool());
            if (pool == null) {
                Constants.LOG.warn("Village house {} names the template pool {}, which doesn't exist", id,
                        house.pool());
                return false;
            }
            synchronized (INJECTED) {
                if (!INJECTED.computeIfAbsent(pool, key -> new HashSet<>()).add(id))
                    return false;
                for (int i = 0; i < house.weight(); i++)
                    pool.templates.add(house.element());
            }
            return true;
        } catch (IOException | RuntimeException e) {
            Constants.LOG.error("Couldn't load village house {}: {}", id, e.getMessage());
            return false;
        }
    }
}
