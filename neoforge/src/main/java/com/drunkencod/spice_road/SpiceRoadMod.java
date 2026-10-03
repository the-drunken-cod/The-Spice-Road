package com.drunkencod.spice_road;

import java.util.List;

import com.drunkencod.spice_road.client.NeoForgeConfigScreenHandler;
import com.drunkencod.spice_road.client.NeoForgeSpiceRegionDebugOverlay;
import com.drunkencod.spice_road.command.SpiceLocateCommand;
import com.drunkencod.spice_road.client.NeoForgeSpiceTooltipHandler;
import com.drunkencod.spice_road.config.NeoForgeConfigHelper;
import com.drunkencod.spice_road.datagen.NeoForgeBlockStateProvider;
import com.drunkencod.spice_road.datagen.NeoForgeItemModelProvider;
import com.drunkencod.spice_road.datagen.NeoForgeSpiceDataMapProvider;
import com.drunkencod.spice_road.datagen.NeoForgeSpiceLootProvider;
import com.drunkencod.spice_road.datagen.BoardLayoutProvider;
import com.drunkencod.spice_road.datagen.SeasoningEffectProvider;
import com.drunkencod.spice_road.datagen.SpiceBlockTagProvider;
import com.drunkencod.spice_road.datagen.SpiceItemTagProvider;
import com.drunkencod.spice_road.datagen.SpiceRoadAdvancements;
import com.drunkencod.spice_road.datagen.SpiceTreeCompatRecipeProvider;
import com.drunkencod.spice_road.datagen.SpiceTreePlanksRecipeProvider;
import com.drunkencod.spice_road.loot.LootInjections;
import com.drunkencod.spice_road.platform.Services;
import com.drunkencod.spice_road.registry.NeoForgeCreativeTabHelper;
import com.drunkencod.spice_road.registry.NeoForgeRegistryHelper;
import com.drunkencod.spice_road.spice.SpiceProfileRegistry;
import com.drunkencod.spice_road.spice.SpiceProfileSync;
import com.drunkencod.spice_road.spice.board.SeasoningWorld;
import com.drunkencod.spice_road.spice.effect.SeasoningEffectSync;
import com.drunkencod.spice_road.villager.SpiceMapTrade;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.minecraft.data.advancements.AdvancementProvider;
import net.minecraft.world.entity.npc.VillagerProfession;
import net.neoforged.neoforge.data.event.GatherDataEvent;
import net.neoforged.neoforge.event.LootTableLoadEvent;
import net.neoforged.neoforge.event.OnDatapackSyncEvent;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.TagsUpdatedEvent;
import net.neoforged.neoforge.event.server.ServerAboutToStartEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.event.village.VillagerTradesEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;

/**
 * NeoForge mod entry point.
 */
@Mod(Constants.MOD_ID)
public class SpiceRoadMod {

    private final ModContainer modContainer;

    /**
     * Wires up registries, configs, and lifecycle/datagen listeners, then runs
     * the shared {@link SpiceRoad#init()}.
     *
     * @param eventBus     The mod event bus.
     * @param modContainer This mod's container, used to register configs and the
     *                     config screen.
     */
    public SpiceRoadMod(IEventBus eventBus, ModContainer modContainer) {
        this.modContainer = modContainer;

        // Wire DeferredRegisters
        ((NeoForgeRegistryHelper) Services.REGISTRY).initialize(eventBus);
        ((NeoForgeCreativeTabHelper) Services.CREATIVE_TAB).initialize(eventBus);

        // Register configs
        ((NeoForgeConfigHelper) Services.CONFIG).register(modContainer);

        eventBus.addListener(this::onGatherData);
        eventBus.addListener(this::onCommonSetup);
        eventBus.addListener(this::onClientSetup);
        eventBus.addListener(SpiceRoadMod::onRegisterPayloadHandlers);
        NeoForge.EVENT_BUS.addListener(SpiceRoadMod::onTagsUpdated);
        NeoForge.EVENT_BUS.addListener(SpiceRoadMod::onServerAboutToStart);
        NeoForge.EVENT_BUS.addListener(SpiceRoadMod::onServerStopped);
        NeoForge.EVENT_BUS.addListener(SpiceRoadMod::onDatapackSync);
        NeoForge.EVENT_BUS.addListener(SpiceRoadMod::onRegisterCommands);
        NeoForge.EVENT_BUS.addListener(SpiceRoadMod::onVillagerTrades);
        NeoForge.EVENT_BUS.addListener(SpiceRoadMod::onLootTableLoad);

        SpiceRoad.init();
    }

    private void onCommonSetup(FMLCommonSetupEvent event) {
        event.enqueueWork(SpiceRoad::commonSetup);
    }

    @SuppressWarnings("deprecation")
    private void onGatherData(GatherDataEvent event) {
        event.getGenerator().addProvider(
                event.includeClient(),
                new NeoForgeItemModelProvider(event.getGenerator().getPackOutput(), event.getExistingFileHelper()));
        event.getGenerator().addProvider(
                event.includeClient(),
                new NeoForgeBlockStateProvider(event.getGenerator().getPackOutput(), event.getExistingFileHelper()));
        event.getGenerator().addProvider(
                event.includeServer(),
                new NeoForgeSpiceLootProvider(event.getGenerator().getPackOutput(), event.getLookupProvider()));
        event.getGenerator().addProvider(
                event.includeServer(),
                new NeoForgeSpiceDataMapProvider(event.getGenerator().getPackOutput(), event.getLookupProvider()));
        event.getGenerator().addProvider(
                event.includeServer(),
                new SpiceTreeCompatRecipeProvider(event.getGenerator().getPackOutput()));
        event.getGenerator().addProvider(
                event.includeServer(),
                new SpiceTreePlanksRecipeProvider(event.getGenerator().getPackOutput()));
        event.getGenerator().addProvider(
                event.includeServer(),
                new SpiceItemTagProvider(event.getGenerator().getPackOutput()));
        event.getGenerator().addProvider(
                event.includeServer(),
                new SpiceBlockTagProvider(event.getGenerator().getPackOutput()));
        event.getGenerator().addProvider(
                event.includeServer(),
                new SeasoningEffectProvider(event.getGenerator().getPackOutput()));
        event.getGenerator().addProvider(
                event.includeServer(),
                new BoardLayoutProvider(event.getGenerator().getPackOutput()));
        event.getGenerator().addProvider(
                event.includeServer(),
                new AdvancementProvider(event.getGenerator().getPackOutput(), event.getLookupProvider(),
                        List.of(new SpiceRoadAdvancements())));
    }

    private void onClientSetup(FMLClientSetupEvent event) {
        // Debug-only F3 overlay; registerIfDevelopment() itself gates on
        // Services.PLATFORM.isDevelopmentEnvironment(), so this is a no-op in
        // production.
        NeoForgeSpiceRegionDebugOverlay.registerIfDevelopment();
        NeoForgeSpiceTooltipHandler.register();
        NeoForgeConfigScreenHandler.register(modContainer);
    }

    private static void onRegisterPayloadHandlers(RegisterPayloadHandlersEvent event) {
        event.registrar("1")
                .playToClient(SpiceProfileSync.TYPE, SpiceProfileSync.STREAM_CODEC,
                        (payload, context) -> payload.handle())
                .playToClient(SeasoningEffectSync.TYPE, SeasoningEffectSync.STREAM_CODEC,
                        (payload, context) -> payload.handle());
    }

    /** Remembers the world seed for Automatic Seasoning, which can't reach a level from a recipe. */
    private static void onServerAboutToStart(ServerAboutToStartEvent event) {
        SeasoningWorld.set(event.getServer().getWorldData().worldGenOptions().seed());
    }

    private static void onServerStopped(ServerStoppedEvent event) {
        SeasoningWorld.clear();
    }

    /** Resolves Default Profiles once the server's item tags are bound. */
    private static void onTagsUpdated(TagsUpdatedEvent event) {
        if (event.getUpdateCause() == TagsUpdatedEvent.UpdateCause.SERVER_DATA_LOAD)
            SpiceProfileRegistry.resolve();
    }

    private static void onRegisterCommands(RegisterCommandsEvent event) {
        SpiceLocateCommand.register(event.getDispatcher());
    }

    /** Adds the Spice Map listings to the cartographer's level pools. */
    private static void onVillagerTrades(VillagerTradesEvent event) {
        if (event.getType() != VillagerProfession.CARTOGRAPHER)
            return;

        SpiceMapTrade.TIER_BY_LEVEL
                .forEach((level, tier) -> event.getTrades().get(level.intValue()).add(new SpiceMapTrade(tier)));
    }

    /** Appends the matching inject table, if any, to each loaded loot table. */
    private static void onLootTableLoad(LootTableLoadEvent event) {
        LootInjections.poolFor(event.getName()).ifPresent(event.getTable()::addPool);
    }

    /**
     * Syncs Default Profiles and the Seasoning Effect catalog to each player on
     * join and to everyone after
     * {@code /reload}.
     */
    private static void onDatapackSync(OnDatapackSyncEvent event) {
        SpiceProfileSync profiles = SpiceProfileSync.current();
        SeasoningEffectSync effects = SeasoningEffectSync.current();
        event.getRelevantPlayers().forEach(player -> {
            PacketDistributor.sendToPlayer(player, profiles);
            PacketDistributor.sendToPlayer(player, effects);
        });
    }
}
