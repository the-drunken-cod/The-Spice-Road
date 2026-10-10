package com.drunkencod.spice_road.stats;

import java.util.ArrayList;
import java.util.List;

import org.jetbrains.annotations.Nullable;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.stats.StatFormatter;
import net.minecraft.stats.Stats;
import net.minecraft.world.entity.Entity;

import com.drunkencod.spice_road.block.SpiceHarvesting;
import com.drunkencod.spice_road.event.FoodEatenListeners;
import com.drunkencod.spice_road.platform.Services;
import com.drunkencod.spice_road.registry.ModDataComponents;

/**
 * The mod's custom player stats, shown in the vanilla Statistics screen
 * (names under {@code stat.spice_road.<id>}) and readable by scoreboards and
 * advancements like any vanilla custom stat.
 */
public final class ModStats {

    private static final List<ResourceLocation> IDS = new ArrayList<>();

    /** Distinct Spices a player has found, see {@link FoundSpiceTracker}. */
    public static final ResourceLocation SPICES_FOUND = register("spices_found");
    /** Right-click harvests of Spice Plants, Vines and fruiting leaves. */
    public static final ResourceLocation HAND_PICKED_HARVESTS = register("hand_picked_harvests");
    /** Effect cells locked in that the player's discovery log didn't know yet. */
    public static final ResourceLocation CELLS_UNLOCKED = register("cells_unlocked");
    /** Accepted Spice Grinder runs that ended with effects and no bane. */
    public static final ResourceLocation FLAWLESS_RUNS = register("flawless_runs");
    /** Seasoned food items eaten. */
    public static final ResourceLocation SEASONED_FOODS_EATEN = register("seasoned_foods_eaten");
    /** Steps made on the Seasoning Board. */
    public static final ResourceLocation GRINDER_MOVES = register("grinder_moves");
    /** Items that finished drying on a Drying Rack the player put them on by hand. */
    public static final ResourceLocation ITEMS_DRIED = register("items_dried");

    private ModStats() {
    }

    /**
     * Registers every stat above (by loading this class) and the listener
     * awarding {@link #SEASONED_FOODS_EATEN}.
     */
    public static void register() {
        FoodEatenListeners.register(event -> {
            if (event.level().isClientSide() || !event.stack().has(ModDataComponents.SEASONING.get()))
                return;
            award(event.entity(), SEASONED_FOODS_EATEN);
        });
    }

    /**
     * Adds one to a stat, unless {@code entity} isn't a genuinely connected
     * player (a fake player, say) or is on the client.
     *
     * @param entity Who earned it.
     * @param stat   One of this class' stats.
     */
    public static void award(@Nullable Entity entity, ResourceLocation stat) {
        if (entity instanceof ServerPlayer player && SpiceHarvesting.isConnectedPlayer(player))
            player.awardStat(stat);
    }

    /**
     * @param player A player.
     * @param stat   One of this class' stats.
     * @return The player's current value of the stat.
     */
    public static int get(ServerPlayer player, ResourceLocation stat) {
        return player.getStats().getValue(Stats.CUSTOM.get(stat));
    }

    /**
     * Sets a stat back to zero. Scoreboard objectives tracking it keep their
     * score, as vanilla has no way to lower one.
     *
     * @param player A player.
     * @param stat   One of this class' stats.
     */
    public static void reset(ServerPlayer player, ResourceLocation stat) {
        player.getStats().setValue(player, Stats.CUSTOM.get(stat), 0);
    }

    /**
     * Creates the {@code Stat} of every stat above, which is what makes them
     * list in the Statistics screen even before they are first awarded. Must
     * run once registries are populated: a {@code Stat} derives its name from
     * its registry key, and NeoForge registers later than mod construction.
     */
    public static void bootstrap() {
        for (ResourceLocation id : IDS)
            Stats.CUSTOM.get(id, StatFormatter.DEFAULT);
    }

    private static ResourceLocation register(String id) {
        ResourceLocation location = Services.REGISTRY.registerCustomStat(id);
        IDS.add(location);
        return location;
    }
}
