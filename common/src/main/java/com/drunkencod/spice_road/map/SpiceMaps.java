package com.drunkencod.spice_road.map;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.jetbrains.annotations.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.MapItem;
import net.minecraft.world.level.saveddata.maps.MapDecorationType;
import net.minecraft.world.level.saveddata.maps.MapItemSavedData;

import com.drunkencod.spice_road.registry.ModMapDecorations;
import com.drunkencod.spice_road.spice.Spice;
import com.drunkencod.spice_road.spice.Tier;
import com.drunkencod.spice_road.spice.region.RegionHeart;
import com.drunkencod.spice_road.spice.region.RegionHeartSearch;

/**
 * Builds Spice Maps: filled maps pointing to the nearest Region Heart of one
 * Spice, named after and styled by that Spice's {@link Tier}.
 */
public final class SpiceMaps {

    /**
     * Translation key of a Spice Map's name. Takes the Spice's name as
     * {@code %1$s}.
     */
    public static final String NAME_TRANSLATION_KEY = "filled_map.spice_road.spice";

    /** Vanilla's explorer map zoom level. */
    public static final byte DEFAULT_ZOOM = 2;

    private SpiceMaps() {
    }

    /**
     * Creates a Spice Map pointing to {@code heart}.
     *
     * @param level      The level the map belongs to.
     * @param heart      The Region Heart to point to.
     * @param zoom       The map's zoom level, see {@link MapItem#create}.
     * @param decoration Marker to use, or {@code null} for the Spice's
     *                   {@link Tier} marker.
     * @return The filled map.
     */
    public static ItemStack create(ServerLevel level, RegionHeart heart, byte zoom,
            @Nullable Holder<MapDecorationType> decoration) {
        Spice spice = heart.spice();
        BlockPos pos = heart.pos();
        ItemStack map = MapItem.create(level, pos.getX(), pos.getZ(), zoom, true, true);
        MapItem.renderBiomePreviewMap(level, map);
        MapItemSavedData.addTargetDecoration(map, pos, "+",
                decoration != null ? decoration : ModMapDecorations.spiceHeart(spice.getTier()));
        map.set(DataComponents.ITEM_NAME, Component.translatable(NAME_TRANSLATION_KEY, spice.getDisplayName()));
        map.set(DataComponents.RARITY, spice.getTier().getRarity());
        return map;
    }

    /**
     * Creates a Spice Map pointing to the nearest Region Heart of
     * {@code spice}.
     *
     * @param level      The level to search in.
     * @param origin     The position to search from.
     * @param radius     Maximum distance to the heart, in blocks.
     * @param spice      The Spice to map.
     * @param zoom       The map's zoom level.
     * @param decoration Marker override, or {@code null} for the default.
     * @return The map, or empty if no heart of {@code spice} is in range.
     */
    public static Optional<ItemStack> createForSpice(ServerLevel level, BlockPos origin, int radius, Spice spice,
            byte zoom, @Nullable Holder<MapDecorationType> decoration) {
        return RegionHeartSearch.findNearest(level, origin, radius, spice)
                .map(heart -> create(level, heart, zoom, decoration));
    }

    /**
     * Creates a Spice Map for a random Spice of {@code tier}, picked among
     * those with a Region Heart in range.
     *
     * @param level      The level to search in.
     * @param origin     The position to search from.
     * @param radius     Maximum distance to the heart, in blocks.
     * @param tier       The {@link Tier} to pick a Spice from.
     * @param random     Picks the Spice.
     * @param zoom       The map's zoom level.
     * @param decoration Marker override, or {@code null} for the default.
     * @return The map, or empty if no Spice of {@code tier} has a heart in
     *         range.
     */
    public static Optional<ItemStack> createForTier(ServerLevel level, BlockPos origin, int radius, Tier tier,
            RandomSource random, byte zoom, @Nullable Holder<MapDecorationType> decoration) {
        List<Spice> candidates = Arrays.stream(Spice.values()).filter(spice -> spice.getTier() == tier).toList();
        Map<Spice, RegionHeart> hearts = RegionHeartSearch.findNearestOfEach(level, origin, radius, candidates);
        if (hearts.isEmpty())
            return Optional.empty();

        // Keep enum order before picking, so the result only depends on random.
        List<Spice> found = new ArrayList<>(candidates);
        found.retainAll(hearts.keySet());
        Spice picked = found.get(random.nextInt(found.size()));
        return Optional.of(create(level, hearts.get(picked), zoom, decoration));
    }
}
