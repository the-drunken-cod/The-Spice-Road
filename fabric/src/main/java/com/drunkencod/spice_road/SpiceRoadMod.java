package com.drunkencod.spice_road;

import com.drunkencod.spice_road.block.SpiceTrees;
import com.drunkencod.spice_road.command.SpiceLocateCommand;
import com.drunkencod.spice_road.config.ConfigSync;
import com.drunkencod.spice_road.config.FabricConfigHelper;
import com.drunkencod.spice_road.grinder.GrinderIntentPayload;
import com.drunkencod.spice_road.grinder.GrinderViewPayload;
import com.drunkencod.spice_road.loot.LootInjections;
import com.drunkencod.spice_road.platform.Services;
import com.drunkencod.spice_road.registry.FabricCreativeTabHelper;
import com.drunkencod.spice_road.spice.SpiceProfileRegistry;
import com.drunkencod.spice_road.spice.SpiceProfileSync;
import com.drunkencod.spice_road.spice.board.SeasoningWorld;
import com.drunkencod.spice_road.spice.effect.SeasoningEffectSync;
import com.drunkencod.spice_road.villager.SpiceMapTrade;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.registry.StrippableBlockRegistry;
import net.fabricmc.fabric.api.biome.v1.BiomeModifications;
import net.fabricmc.fabric.api.biome.v1.BiomeSelectors;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.CommonLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.loot.v3.LootTableEvents;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.fabricmc.fabric.api.object.builder.v1.trade.TradeOfferHelper;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.npc.VillagerProfession;
import net.minecraft.world.level.levelgen.GenerationStep;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;

/**
 * Fabric main entry point.
 */
public class SpiceRoadMod implements ModInitializer {

        @Override
        public void onInitialize() {
                // Register Cloth Config configs
                ((FabricConfigHelper) Services.CONFIG).register();
                ((FabricCreativeTabHelper) Services.CREATIVE_TAB).register();

                SpiceRoad.init();
                SpiceRoad.commonSetup();

                registerSpiceProfileSync();
                registerSeasoningWorld();
                registerGrinderPayloads();
                registerConfigSync();

                // NeoForge gets this mapping from the datagenned neoforge:strippables data map.
                SpiceTrees.getRegistered().values().forEach(
                                tree -> StrippableBlockRegistry.register(tree.getLog().get(),
                                                tree.getStrippedLog().get()));

                // NeoForge does the equivalent via the data-driven biome_modifier JSON
                // (neoforge/src/main/resources/data/spice_road/neoforge/biome_modifier/);
                // Fabric has no JSON-based equivalent, so this is done in code instead.
                BiomeModifications.addFeature(BiomeSelectors.foundInOverworld(),
                                GenerationStep.Decoration.VEGETAL_DECORATION, placedFeature("spice_plant"));
                // Before any vegetation, so Heart Groves find bare ground instead of forest
                // canopies.
                BiomeModifications.addFeature(BiomeSelectors.foundInOverworld(),
                                GenerationStep.Decoration.LOCAL_MODIFICATIONS, placedFeature("spice_heart_grove"));
                BiomeModifications.addFeature(BiomeSelectors.foundInOverworld(),
                                GenerationStep.Decoration.VEGETAL_DECORATION, placedFeature("spice_heart_satellite"));
                BiomeModifications.addFeature(BiomeSelectors.foundInOverworld(),
                                GenerationStep.Decoration.VEGETAL_DECORATION,
                                placedFeature("spice_heart_satellite_hilly"));
                BiomeModifications.addFeature(BiomeSelectors.foundInOverworld(),
                                GenerationStep.Decoration.VEGETAL_DECORATION,
                                placedFeature("spice_heart_satellite_barren"));

                registerSpiceMaps();
        }

        private static ResourceKey<PlacedFeature> placedFeature(String id) {
                return ResourceKey.create(Registries.PLACED_FEATURE,
                                ResourceLocation.fromNamespaceAndPath(Constants.MOD_ID, id));
        }

        /**
         * Registers {@code /locate spice}/{@code spice_climate}, the
         * cartographer's Spice Map trades, and the loot table injections.
         * NeoForge does the same through its own events.
         */
        private static void registerSpiceMaps() {
                CommandRegistrationCallback.EVENT
                                .register((dispatcher, registryAccess, environment) -> SpiceLocateCommand
                                                .register(dispatcher));
                SpiceMapTrade.TIER_BY_LEVEL.forEach((level, tier) -> TradeOfferHelper.registerVillagerOffers(
                                VillagerProfession.CARTOGRAPHER, level,
                                factories -> factories.add(new SpiceMapTrade(tier))));
                LootTableEvents.MODIFY.register((key, tableBuilder, source, registries) -> LootInjections
                                .poolFor(key.location()).ifPresent(tableBuilder::pool));
        }

        /**
         * Resolves Default Profiles once the server's item tags are bound, and
         * syncs them and the Seasoning Effect catalog to each player on join and to everyone after
         * {@code /reload}. NeoForge does the same through its own events.
         */
        private static void registerSpiceProfileSync() {
                PayloadTypeRegistry.playS2C().register(SpiceProfileSync.TYPE, SpiceProfileSync.STREAM_CODEC);
                PayloadTypeRegistry.playS2C().register(SeasoningEffectSync.TYPE, SeasoningEffectSync.STREAM_CODEC);
                CommonLifecycleEvents.TAGS_LOADED.register((registries, client) -> {
                        if (!client)
                                SpiceProfileRegistry.resolve();
                });
                ServerLifecycleEvents.SYNC_DATA_PACK_CONTENTS.register((player, joined) -> {
                        ServerPlayNetworking.send(player, SpiceProfileSync.current());
                        ServerPlayNetworking.send(player, SeasoningEffectSync.current());
                });
        }

        /**
         * Registers the Spice Grinder's payloads and the receiver of what players
         * do in its GUI. NeoForge does the same through its own event.
         */
        private static void registerGrinderPayloads() {
                PayloadTypeRegistry.playC2S().register(GrinderIntentPayload.TYPE, GrinderIntentPayload.STREAM_CODEC);
                PayloadTypeRegistry.playS2C().register(GrinderViewPayload.TYPE, GrinderViewPayload.STREAM_CODEC);
                ServerPlayNetworking.registerGlobalReceiver(GrinderIntentPayload.TYPE,
                                (payload, context) -> payload.handle(context.player()));
        }

        /**
         * Remembers the world seed for Automatic Seasoning, which can't reach a
         * level from a recipe. NeoForge does the same through its own events.
         */
        private static void registerSeasoningWorld() {
                ServerLifecycleEvents.SERVER_STARTING.register(
                                server -> SeasoningWorld.set(server.getWorldData().worldGenOptions().seed()));
                ServerLifecycleEvents.SERVER_STOPPED.register(server -> SeasoningWorld.clear());
        }

        /**
         * Registers and sends {@link ConfigSync}, syncing SERVER config values to
         * each player on join and to everyone after {@code /reload}. NeoForge
         * needs no equivalent: its built-in {@code ModConfigSpec} sync already
         * reaches NeoForge clients.
         */
        private static void registerConfigSync() {
                PayloadTypeRegistry.playS2C().register(ConfigSync.TYPE, ConfigSync.STREAM_CODEC);
                ServerLifecycleEvents.SYNC_DATA_PACK_CONTENTS
                                .register((player, joined) -> ServerPlayNetworking.send(player, ConfigSync.current()));
        }
}
