package com.drunkencod.spice_road.soil;

import java.util.Optional;

import org.jetbrains.annotations.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import com.drunkencod.spice_road.Constants;
import com.drunkencod.spice_road.platform.Services;
import com.drunkencod.spice_road.registry.ModItems;
import com.drunkencod.spice_road.spice.ProcessedSpice;
import com.drunkencod.spice_road.spice.Spice;
import com.drunkencod.spice_road.spice.SpiceProfileRegistry;
import com.drunkencod.spice_road.spice.Tier;
import com.drunkencod.spice_road.spice.region.SpiceRegionLayout;
import com.drunkencod.spice_road.spice.region.SpiceRegionResult;
import com.drunkencod.spice_road.stats.SpiceFindings;

/**
 * What a finished set of Soil Samples tells the player: the Spice that
 * resolves at their centroid with the climate found there, if the player has
 * found it and it isn't too rare for the analyzer to identify.
 */
public final class SoilAnalysis {

    private static final String KEY_PREFIX = "spice_road.message.soil_analyzer.result.";

    private SoilAnalysis() {
    }

    /** What the analysis can say. */
    public enum Outcome {
        /** The Spice is named. */
        FOUND,
        /** There is a Spice, but the player hasn't found it or it is too rare to identify. */
        UNKNOWN,
        /** No Spice grows there. */
        NONE
    }

    /**
     * @param resolved      The Spice that resolved at the centroid, if any.
     * @param maxTierOrdinal The ordinal of the highest {@link Tier} the analyzer identifies.
     * @param found         Whether the player has found the resolved Spice.
     * @return What to tell the player.
     */
    public static Outcome outcome(Optional<Spice> resolved, int maxTierOrdinal, boolean found) {
        if (resolved.isEmpty())
            return Outcome.NONE;
        if (resolved.get().getTier().ordinal() > maxTierOrdinal || !found)
            return Outcome.UNKNOWN;
        return Outcome.FOUND;
    }

    /**
     * Picks the item whose tooltip to show for a Spice, so the tooltip has a
     * Spice Profile. Some Spices only have one on their {@link ProcessedSpice}
     * items, not on the raw item.
     *
     * @param spice The Spice to show.
     * @return The raw item if it has a Spice Profile, else the first Processed
     *         Spice item that does, else the raw item. {@code null} before
     *         registration.
     */
    private static @Nullable Item tooltipItem(Spice spice) {
        Item raw = ModItems.byId(spice.getId());
        if (raw != null && SpiceProfileRegistry.getDefault(raw).isPresent())
            return raw;
        for (ProcessedSpice processed : ProcessedSpice.bySource(spice)) {
            Item item = processed.getItem();
            if (item != null && SpiceProfileRegistry.getDefault(item).isPresent())
                return item;
        }
        return raw;
    }

    /**
     * @param level   The level the samples were taken in.
     * @param samples A finished set.
     * @return The Spice that resolves at the centroid of {@code samples} with
     *         the climate found there. Empty if no Spice grows there.
     */
    public static Optional<Spice> resolve(ServerLevel level, SoilSamples samples) {
        BlockPos centroid = samples.centroid();
        return SpiceRegionLayout.of(level).map(layout -> {
            SpiceRegionResult result = layout.resolve(level.getBiome(centroid), centroid);
            return result.spice();
        }).flatMap(spice -> spice);
    }

    /**
     * Words what a finished set of samples tells the player.
     *
     * @param player   The player who took them.
     * @param resolved The result of {@link #resolve}.
     * @return The message to send the player. A found Spice's name carries its
     *         Tier color and shows the tooltip of the item that has its Spice
     *         Profile on hover. Creative players always get the Spice's name,
     *         whether found and whatever its Tier.
     */
    public static Component report(ServerPlayer player, Optional<Spice> resolved) {
        boolean creative = player.isCreative();
        boolean found = creative || resolved.isPresent() && SpiceFindings.of(player.server).itemsOf(player.getUUID())
                .contains(ResourceLocation.fromNamespaceAndPath(Constants.MOD_ID, resolved.get().getId()));
        int maxTier = creative ? Tier.EPIC.ordinal() : Services.CONFIG.getSoilAnalyzerMaxIdentifiableTier();
        Outcome outcome = outcome(resolved, maxTier, found);
        if (outcome == Outcome.FOUND) {
            Item raw = ModItems.byId(resolved.get().getId());
            Item tooltipItem = tooltipItem(resolved.get());
            if (raw != null && tooltipItem != null) {
                // The raw item names the Spice, the variant with a profile fills the hover tooltip
                MutableComponent name = new ItemStack(raw).getDisplayName().copy().withStyle(style -> style
                        .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_ITEM,
                                new HoverEvent.ItemStackInfo(new ItemStack(tooltipItem)))));
                return Component.translatable(KEY_PREFIX + "found", name);
            }
            outcome = Outcome.UNKNOWN;
        }
        return Component.translatable(KEY_PREFIX + (outcome == Outcome.NONE ? "none" : "unknown"));
    }
}
