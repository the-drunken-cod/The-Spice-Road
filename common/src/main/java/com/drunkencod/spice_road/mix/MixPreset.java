package com.drunkencod.spice_road.mix;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;

/**
 * A named, datapack-defined combination of spices in fixed proportions. A grid
 * of a jar and spices holding exactly these proportions, at any multiple, is
 * crafted into this preset's Spice Mix instead of a custom one. Its name comes
 * from the lang key {@code mix_preset.<namespace>.<path>} of its ID.
 *
 * @param slots           The proportions, reduced to their smallest whole
 *                        numbers (2:2:2 is stored as 1:1:1). A slot may be an
 *                        item tag, which any combination of its members fills.
 * @param customModelData The {@code minecraft:custom_model_data} its mixes are
 *                        given, so the item model can pick their look.
 */
public record MixPreset(List<PresetSlot> slots, Optional<Integer> customModelData) {

    /** Persistent (JSON) codec. */
    public static final Codec<MixPreset> CODEC = RecordCodecBuilder.<MixPreset>create(instance -> instance.group(
            PresetSlot.CODEC.listOf().fieldOf("spices").forGetter(MixPreset::slots),
            Codec.INT.optionalFieldOf("custom_model_data").forGetter(MixPreset::customModelData))
            .apply(instance, MixPreset::new))
            .validate(preset -> preset.slots.isEmpty()
                    ? DataResult.error(() -> "A Mix Preset needs at least one spice")
                    : DataResult.success(preset));

    /** Network codec. */
    public static final StreamCodec<RegistryFriendlyByteBuf, MixPreset> STREAM_CODEC = StreamCodec.composite(
            PresetSlot.STREAM_CODEC.apply(ByteBufCodecs.list()), MixPreset::slots,
            ByteBufCodecs.optional(ByteBufCodecs.VAR_INT), MixPreset::customModelData,
            MixPreset::new);

    /**
     * Merges slots of the same source, reduces the proportions to their
     * smallest whole numbers and puts the slots in a fixed order.
     *
     * @param slots           The proportions.
     * @param customModelData The custom model data of its mixes.
     */
    public MixPreset {
        Map<Either<Item, TagKey<Item>>, Integer> merged = new LinkedHashMap<>();
        for (PresetSlot slot : slots)
            merged.merge(slot.source(), slot.count(), Integer::sum);
        int divisor = 0;
        for (int count : merged.values())
            divisor = gcd(divisor, count);
        int reduceBy = Math.max(1, divisor);
        slots = merged.entrySet().stream()
                .map(entry -> new PresetSlot(entry.getKey(), entry.getValue() / reduceBy))
                .sorted(PresetSlot.ORDER)
                .toList();
    }

    /**
     * Pooled matching: every spice in the grid fills a slot that accepts it, and
     * each slot ends up holding exactly its proportion times the multiple.
     * Where one spice could fill several slots (overlapping tags), whichever
     * assignment makes everything fit is used.
     *
     * @param counts How many of each spice a grid holds.
     * @return The multiple of this preset's proportions {@code counts} is, or
     *         {@code 0} if it isn't one (other spices, or other proportions).
     */
    public int multipleOf(Map<Item, Integer> counts) {
        int supplied = counts.values().stream().mapToInt(Integer::intValue).sum();
        int batch = total();
        if (batch <= 0 || supplied <= 0 || supplied % batch != 0)
            return 0;
        int multiple = supplied / batch;
        List<Item> items = List.copyOf(counts.keySet());
        int[] supply = items.stream().mapToInt(counts::get).toArray();
        int[] demand = slots.stream().mapToInt(slot -> slot.count() * multiple).toArray();
        boolean[][] fits = new boolean[items.size()][slots.size()];
        for (int i = 0; i < items.size(); i++) {
            for (int s = 0; s < slots.size(); s++)
                fits[i][s] = slots.get(s).accepts(items.get(i));
        }
        return SlotAssignment.exact(supply, demand, fits) ? multiple : 0;
    }

    /** @return How many spices one batch of the proportions holds. */
    public int total() {
        return slots.stream().mapToInt(PresetSlot::count).sum();
    }

    /**
     * @return One batch of the proportions as concrete spices, taking the first
     *         member of each tag, or empty if a tag has none (yet).
     */
    public Optional<Map<Item, Integer>> representativeSpices() {
        Map<Item, Integer> spices = new LinkedHashMap<>();
        for (PresetSlot slot : slots) {
            Optional<Item> item = slot.representative();
            if (item.isEmpty())
                return Optional.empty();
            spices.merge(item.get(), slot.count(), Integer::sum);
        }
        return Optional.of(spices);
    }

    private static int gcd(int a, int b) {
        return b == 0 ? a : gcd(b, a % b);
    }
}
