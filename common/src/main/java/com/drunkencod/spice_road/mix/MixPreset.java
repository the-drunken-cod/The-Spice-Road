package com.drunkencod.spice_road.mix;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.Item;

/**
 * A named, datapack-defined combination of spices in fixed proportions. A grid
 * of a jar and spices holding exactly these proportions, at any multiple, is
 * crafted into this preset's Spice Mix instead of a custom one. Its name comes
 * from the lang key {@code mix_preset.<namespace>.<path>} of its ID.
 *
 * @param spices          The proportions, reduced to their smallest whole
 *                        numbers (2:2:2 is stored as 1:1:1).
 * @param customModelData The {@code minecraft:custom_model_data} its mixes are
 *                        given, so the item model can pick their look.
 */
public record MixPreset(Map<Item, Integer> spices, Optional<Integer> customModelData) {

    /** Persistent (JSON) codec. */
    public static final Codec<MixPreset> CODEC = RecordCodecBuilder.<MixPreset>create(instance -> instance.group(
            SpiceMix.SPICES_CODEC.fieldOf("spices").forGetter(MixPreset::spices),
            Codec.INT.optionalFieldOf("custom_model_data").forGetter(MixPreset::customModelData))
            .apply(instance, MixPreset::new))
            .validate(preset -> preset.spices.isEmpty()
                    ? DataResult.error(() -> "A Mix Preset needs at least one spice")
                    : DataResult.success(preset));

    /** Network codec. */
    public static final StreamCodec<RegistryFriendlyByteBuf, MixPreset> STREAM_CODEC = StreamCodec.composite(
            SpiceMix.SPICES_STREAM_CODEC, MixPreset::spices,
            ByteBufCodecs.optional(ByteBufCodecs.VAR_INT), MixPreset::customModelData,
            MixPreset::new);

    /**
     * Reduces the proportions to their smallest whole numbers.
     *
     * @param spices          The proportions.
     * @param customModelData The custom model data of its mixes.
     */
    public MixPreset {
        Map<Item, Integer> canonical = SpiceMix.canonical(spices);
        int divisor = 0;
        for (int count : canonical.values())
            divisor = gcd(divisor, count);
        Map<Item, Integer> reduced = new LinkedHashMap<>();
        for (Map.Entry<Item, Integer> entry : canonical.entrySet())
            reduced.put(entry.getKey(), entry.getValue() / Math.max(1, divisor));
        spices = Collections.unmodifiableMap(reduced);
    }

    /**
     * @param counts How many of each spice a grid holds.
     * @return The multiple of this preset's proportions {@code counts} is, or
     *         {@code 0} if it isn't one (other spices, or other proportions).
     */
    public int multipleOf(Map<Item, Integer> counts) {
        if (!counts.keySet().equals(spices.keySet()))
            return 0;
        int multiple = 0;
        for (Map.Entry<Item, Integer> entry : spices.entrySet()) {
            int count = counts.get(entry.getKey());
            if (count % entry.getValue() != 0)
                return 0;
            int here = count / entry.getValue();
            if (multiple != 0 && here != multiple)
                return 0;
            multiple = here;
        }
        return multiple;
    }

    /** @return How many spices one batch of the proportions holds. */
    public int total() {
        return spices.values().stream().mapToInt(Integer::intValue).sum();
    }

    private static int gcd(int a, int b) {
        return b == 0 ? a : gcd(b, a % b);
    }
}
