package com.drunkencod.spice_road.mix;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;

/**
 * What a Spice Mix holds: whole spices, counted per Spice Item, and the Mix
 * Preset it was crafted as, if any. A mix is a pure source of spices: it is
 * never seasoned and never carries Seasoning Effects. Spices are held in
 * registry-ID order, so equal contents are always equal records and their
 * stacks keep stacking.
 *
 * @param preset The ID of the Mix Preset it was crafted as, empty for a custom
 *               mix.
 * @param spices How many of each Spice Item it holds, all above zero.
 */
public record SpiceMix(Optional<ResourceLocation> preset, Map<Item, Integer> spices) {

    private record Entry(Item item, int count) {

        private static final Codec<Entry> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                BuiltInRegistries.ITEM.byNameCodec().fieldOf("item").forGetter(Entry::item),
                Codec.intRange(1, 1000).fieldOf("count").forGetter(Entry::count))
                .apply(instance, Entry::new));
    }

    /** Codec of a counted spice list, as {@code [{"item": ..., "count": ...}]}. */
    public static final Codec<Map<Item, Integer>> SPICES_CODEC = Entry.CODEC.listOf().xmap(
            entries -> {
                Map<Item, Integer> map = new LinkedHashMap<>();
                entries.forEach(entry -> map.merge(entry.item(), entry.count(), Integer::sum));
                return map;
            },
            map -> map.entrySet().stream().map(entry -> new Entry(entry.getKey(), entry.getValue())).toList());

    /** Network codec of a counted spice list. */
    public static final StreamCodec<RegistryFriendlyByteBuf, Map<Item, Integer>> SPICES_STREAM_CODEC = ByteBufCodecs
            .<RegistryFriendlyByteBuf, Item, Integer, Map<Item, Integer>>map(LinkedHashMap::new,
                    ByteBufCodecs.registry(Registries.ITEM), ByteBufCodecs.VAR_INT);

    /** Persistent (NBT) codec. */
    public static final Codec<SpiceMix> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            ResourceLocation.CODEC.optionalFieldOf("preset").forGetter(SpiceMix::preset),
            SPICES_CODEC.fieldOf("spices").forGetter(SpiceMix::spices))
            .apply(instance, SpiceMix::new));

    /** Network codec. */
    public static final StreamCodec<RegistryFriendlyByteBuf, SpiceMix> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.optional(ResourceLocation.STREAM_CODEC), SpiceMix::preset,
            SPICES_STREAM_CODEC, SpiceMix::spices,
            SpiceMix::new);

    /**
     * Puts the spices in registry-ID order and drops any without a positive
     * count.
     *
     * @param preset The ID of the Mix Preset, empty for a custom mix.
     * @param spices The counted spices.
     */
    public SpiceMix {
        spices = canonical(spices);
    }

    /**
     * @param spices Counted spices, in any order.
     * @return The same spices in registry-ID order, without any that aren't
     *         above zero.
     */
    public static Map<Item, Integer> canonical(Map<Item, Integer> spices) {
        List<Map.Entry<Item, Integer>> sorted = new ArrayList<>(spices.entrySet());
        sorted.sort(Map.Entry.comparingByKey(Comparator.comparing(BuiltInRegistries.ITEM::getKey)));
        Map<Item, Integer> result = new LinkedHashMap<>();
        for (Map.Entry<Item, Integer> entry : sorted) {
            if (entry.getValue() > 0)
                result.merge(entry.getKey(), entry.getValue(), Integer::sum);
        }
        return Collections.unmodifiableMap(result);
    }

    /** @return How many spices it holds in total. */
    public int total() {
        return spices.values().stream().mapToInt(Integer::intValue).sum();
    }
}
