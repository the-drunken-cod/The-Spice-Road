package com.drunkencod.spice_road.spice.effect;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;

/**
 * A Seasoning Effect as stored on a seasoned food: which catalog entry, and at
 * which level. Duration and strength come from the entry when the food is
 * eaten, never from the food.
 *
 * @param id    The ID of the {@link SeasoningEffectDef catalog entry}.
 * @param level The stacked level, at least {@code 1}.
 */
public record SeasoningEffect(ResourceLocation id, int level) {

    /** Persistent (NBT) codec. */
    public static final Codec<SeasoningEffect> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            ResourceLocation.CODEC.fieldOf("id").forGetter(SeasoningEffect::id),
            Codec.intRange(1, Integer.MAX_VALUE).optionalFieldOf("level", 1).forGetter(SeasoningEffect::level))
            .apply(instance, SeasoningEffect::new));

    /** Network codec. */
    public static final StreamCodec<FriendlyByteBuf, SeasoningEffect> STREAM_CODEC = StreamCodec.composite(
            ResourceLocation.STREAM_CODEC, SeasoningEffect::id,
            ByteBufCodecs.VAR_INT, SeasoningEffect::level,
            SeasoningEffect::new);

    /**
     * @param id    The ID of the catalog entry.
     * @param level The level, at least {@code 1}.
     */
    public SeasoningEffect {
        level = Math.max(1, level);
    }
}
