package com.drunkencod.spice_road.spice;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.Item;

/**
 * What a seasoned food stack carries: its Flavor Contributors as a counted
 * multiset (item and amount). Only Spice Items keep a Spice Profile - a food's
 * profile is derived from these contributors' Default Profiles when needed,
 * never stored. Having this component at all is what marks a food as seasoned.
 * <p>
 * Amounts are fractional shares kept to {@value Flavoring#DECIMAL_PLACES}
 * decimal places, and entries are held in registry-ID order, so equal
 * contributions are always equal records and their stacks keep stacking.
 *
 * @param contributors The amount of each contributing item, all above zero.
 */
public record Seasoning(Map<Item, Double> contributors) {

    private static final Codec<Map<Item, Double>> CONTRIBUTORS_CODEC = Entry.CODEC.listOf()
            .xmap(entries -> canonical(entries.stream().map(entry -> Map.entry(entry.item(), entry.amount())).toList()),
                    contributors -> contributors.entrySet().stream()
                            .map(entry -> new Entry(entry.getKey(), entry.getValue())).toList());

    /** Persistent (NBT) codec. */
    public static final Codec<Seasoning> CODEC = CONTRIBUTORS_CODEC.xmap(Seasoning::new, Seasoning::contributors);

    /** Network codec. */
    public static final StreamCodec<RegistryFriendlyByteBuf, Seasoning> STREAM_CODEC = ByteBufCodecs
            .<RegistryFriendlyByteBuf, Item, Double, Map<Item, Double>>map(LinkedHashMap::new,
                    ByteBufCodecs.registry(Registries.ITEM), ByteBufCodecs.DOUBLE)
            .map(Seasoning::new, Seasoning::contributors);

    /**
     * Normalizes {@code contributors}: entries are put in registry-ID order,
     * amounts are rounded to {@value Flavoring#DECIMAL_PLACES} decimal places
     * and entries without a positive amount are dropped.
     *
     * @param contributors The contributors to normalize.
     */
    public Seasoning {
        contributors = canonical(List.copyOf(contributors.entrySet()));
    }

    /**
     * @param item   The contributing item.
     * @param amount Its amount.
     * @return A Seasoning of just that contributor.
     */
    public static Seasoning of(Item item, double amount) {
        return new Seasoning(Map.of(item, amount));
    }

    /**
     * @param item The item to look up.
     * @return The amount {@code item} contributed, {@code 0} if it didn't.
     */
    public double amountOf(Item item) {
        return contributors.getOrDefault(item, 0D);
    }

    /**
     * @return The summed amount of every contributor.
     */
    public double totalAmount() {
        return Flavoring.round(contributors.values().stream().mapToDouble(Double::doubleValue).sum());
    }

    /**
     * @param other The Seasoning to merge in.
     * @return A Seasoning with each contributor's amounts from both summed.
     */
    public Seasoning plus(Seasoning other) {
        List<Map.Entry<Item, Double>> entries = new ArrayList<>(contributors.entrySet());
        entries.addAll(other.contributors.entrySet());
        return new Seasoning(canonical(entries));
    }

    /**
     * @param entries Contributor entries, possibly with repeated items.
     * @return Their amounts summed per item and rounded, entries sorted by
     *         registry ID and non-positive ones dropped; unmodifiable.
     */
    private static Map<Item, Double> canonical(List<? extends Map.Entry<Item, Double>> entries) {
        Map<Item, Double> sums = new LinkedHashMap<>();
        for (Map.Entry<Item, Double> entry : entries)
            sums.merge(entry.getKey(), entry.getValue(), Double::sum);
        List<Map.Entry<Item, Double>> sorted = new ArrayList<>(sums.entrySet());
        sorted.sort((a, b) -> BuiltInRegistries.ITEM.getKey(a.getKey())
                .compareTo(BuiltInRegistries.ITEM.getKey(b.getKey())));
        Map<Item, Double> result = new LinkedHashMap<>();
        for (Map.Entry<Item, Double> entry : sorted) {
            double amount = Flavoring.round(entry.getValue());
            if (amount > 0D)
                result.put(entry.getKey(), amount);
        }
        return Collections.unmodifiableMap(result);
    }

    /** One contributor as stored in NBT. */
    private record Entry(Item item, double amount) {

        private static final Codec<Entry> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                BuiltInRegistries.ITEM.byNameCodec().fieldOf("item").forGetter(Entry::item),
                Codec.DOUBLE.fieldOf("amount").forGetter(Entry::amount))
                .apply(instance, Entry::new));
    }
}
