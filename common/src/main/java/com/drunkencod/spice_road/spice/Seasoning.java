package com.drunkencod.spice_road.spice;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
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
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;

import com.drunkencod.spice_road.spice.effect.SeasoningEffect;

/**
 * What a seasoned food stack carries: its Flavor Contributors as a counted
 * multiset (item and amount) and its Seasoning Effects. Only Spice Items keep a
 * Spice Profile - a food's profile is derived from its contributors' Default
 * Profiles when needed, never stored. Having this component at all is what
 * marks a food as seasoned, with or without effects.
 * <p>
 * Contributor amounts are fractional shares kept to
 * {@value Flavoring#DECIMAL_PLACES} decimal places. Contributors are held in
 * registry-ID order and effects in entry-ID order, so equal contents are always
 * equal records and their stacks keep stacking.
 *
 * @param contributors The amount of each contributing item, all above zero.
 * @param effects      The effects eating the food applies, one entry per catalog
 *                     entry.
 */
public record Seasoning(Map<Item, Double> contributors, List<SeasoningEffect> effects) {

    private record Entry(Item item, double amount) {

        private static final Codec<Entry> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                BuiltInRegistries.ITEM.byNameCodec().fieldOf("item").forGetter(Entry::item),
                Codec.DOUBLE.fieldOf("amount").forGetter(Entry::amount))
                .apply(instance, Entry::new));
    }

    private static final Codec<Map<Item, Double>> CONTRIBUTORS_CODEC = Entry.CODEC.listOf()
            .xmap(entries -> canonicalContributors(
                    entries.stream().map(entry -> Map.entry(entry.item(), entry.amount())).toList()),
                    contributors -> contributors.entrySet().stream()
                            .map(entry -> new Entry(entry.getKey(), entry.getValue())).toList());

    /** Persistent (NBT) codec. */
    public static final Codec<Seasoning> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            CONTRIBUTORS_CODEC.optionalFieldOf("contributors", Map.of()).forGetter(Seasoning::contributors),
            SeasoningEffect.CODEC.listOf().optionalFieldOf("effects", List.of()).forGetter(Seasoning::effects))
            .apply(instance, Seasoning::new));

    /** Network codec. */
    public static final StreamCodec<RegistryFriendlyByteBuf, Seasoning> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.<RegistryFriendlyByteBuf, Item, Double, Map<Item, Double>>map(LinkedHashMap::new,
                    ByteBufCodecs.registry(Registries.ITEM), ByteBufCodecs.DOUBLE),
            Seasoning::contributors,
            SeasoningEffect.STREAM_CODEC.apply(ByteBufCodecs.list()), Seasoning::effects,
            Seasoning::new);

    /**
     * Normalizes both parts: contributors are put in registry-ID order,
     * amounts are rounded to {@value Flavoring#DECIMAL_PLACES} decimal places
     * and contributors without a positive amount are dropped; effects are
     * merged per entry ID by summing their levels and put in entry-ID order.
     *
     * @param contributors The contributors to normalize.
     * @param effects      The effects to normalize.
     */
    public Seasoning {
        contributors = canonicalContributors(List.copyOf(contributors.entrySet()));
        effects = canonicalEffects(effects);
    }

    /**
     * @param contributors The contributors of the new Seasoning.
     */
    public Seasoning(Map<Item, Double> contributors) {
        this(contributors, List.of());
    }

    /**
     * @param item   The contributing item.
     * @param amount Its amount.
     * @return A Seasoning of just that contributor, without effects.
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
     * Merges more spices in. Effects don't carry over: they are re-solved from
     * the new total, so the result has none.
     *
     * @param other The Seasoning to merge in.
     * @return A Seasoning with each contributor's amounts from both summed.
     */
    public Seasoning plus(Seasoning other) {
        List<Map.Entry<Item, Double>> entries = new ArrayList<>(contributors.entrySet());
        entries.addAll(other.contributors.entrySet());
        return new Seasoning(canonicalContributors(entries));
    }

    /**
     * @param newEffects The effects of the new Seasoning.
     * @return A Seasoning with these contributors and {@code newEffects}.
     */
    public Seasoning withEffects(List<SeasoningEffect> newEffects) {
        return new Seasoning(contributors, newEffects);
    }

    private static Map<Item, Double> canonicalContributors(List<? extends Map.Entry<Item, Double>> entries) {
        Map<Item, Double> sums = new LinkedHashMap<>();
        for (Map.Entry<Item, Double> entry : entries)
            sums.merge(entry.getKey(), entry.getValue(), Double::sum);
        List<Map.Entry<Item, Double>> sorted = new ArrayList<>(sums.entrySet());
        sorted.sort(Comparator.comparing(entry -> BuiltInRegistries.ITEM.getKey(entry.getKey())));
        Map<Item, Double> result = new LinkedHashMap<>();
        for (Map.Entry<Item, Double> entry : sorted) {
            double amount = Flavoring.round(entry.getValue());
            if (amount > 0D)
                result.put(entry.getKey(), amount);
        }
        return Collections.unmodifiableMap(result);
    }

    private static List<SeasoningEffect> canonicalEffects(List<SeasoningEffect> effects) {
        Map<ResourceLocation, Integer> sums = new LinkedHashMap<>();
        for (SeasoningEffect effect : effects)
            sums.merge(effect.id(), effect.level(), Integer::sum);
        return sums.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .map(entry -> new SeasoningEffect(entry.getKey(), entry.getValue()))
                .toList();
    }
}
