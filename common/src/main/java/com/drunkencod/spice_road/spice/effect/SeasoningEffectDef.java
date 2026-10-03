package com.drunkencod.spice_road.spice.effect;

import java.util.Optional;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.core.Holder;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.effect.MobEffect;

import com.drunkencod.spice_road.spice.FlavorAxis;

/**
 * One entry of the Seasoning Effect catalog, loaded from
 * {@code data/<namespace>/seasoning_effect/*.json}: which mob effect a food
 * effect applies and how long, strong and stackable it is. The file ID is the
 * entry's ID, which is what a seasoned food stores.
 *
 * @param effect       The mob effect applied when the food is eaten.
 * @param kind         Whether the effect helps or hurts the eater.
 * @param pole         The Flavor Axis pole this entry is the effect of, if it is
 *                     one (an axis zone of the Seasoning Board gives its pole's
 *                     boon and bane).
 * @param randomWeight Weight in the vanilla-random pool, {@code 0} if the entry
 *                     isn't part of it.
 * @param baseDuration Duration in ticks at level 1, before the configured
 *                     multiplier.
 * @param maxLevel     The highest level the entry can stack to.
 * @param scaling      What a higher level does to the mob effect.
 */
public record SeasoningEffectDef(
        Holder<MobEffect> effect,
        EffectKind kind,
        Optional<Pole> pole,
        int randomWeight,
        int baseDuration,
        int maxLevel,
        LevelScaling scaling) {

    /** Codec reading catalog files. */
    public static final Codec<SeasoningEffectDef> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            MobEffect.CODEC.fieldOf("effect").forGetter(SeasoningEffectDef::effect),
            EffectKind.CODEC.fieldOf("kind").forGetter(SeasoningEffectDef::kind),
            Pole.CODEC.optionalFieldOf("pole").forGetter(SeasoningEffectDef::pole),
            Codec.intRange(0, Integer.MAX_VALUE).optionalFieldOf("random_weight", 0)
                    .forGetter(SeasoningEffectDef::randomWeight),
            Codec.intRange(1, Integer.MAX_VALUE).fieldOf("base_duration").forGetter(SeasoningEffectDef::baseDuration),
            Codec.intRange(1, Integer.MAX_VALUE).optionalFieldOf("max_level", 1)
                    .forGetter(SeasoningEffectDef::maxLevel),
            LevelScaling.CODEC.optionalFieldOf("level_scaling", LevelScaling.AMPLIFIER)
                    .forGetter(SeasoningEffectDef::scaling))
            .apply(instance, SeasoningEffectDef::new));

    /** Network codec. */
    public static final StreamCodec<RegistryFriendlyByteBuf, SeasoningEffectDef> STREAM_CODEC = ByteBufCodecs
            .fromCodecWithRegistries(CODEC);

    /**
     * One pole of a Flavor Axis.
     *
     * @param axis     The Flavor Axis.
     * @param positive Whether the positive pole of {@code axis} is meant,
     *                 otherwise the negative one.
     */
    public record Pole(FlavorAxis axis, boolean positive) {

        /** Codec reading and writing a pole. */
        public static final Codec<Pole> CODEC = RecordCodecBuilder
                .create(instance -> instance.group(
                        FlavorAxis.CODEC.fieldOf("axis").forGetter(Pole::axis),
                        Codec.BOOL.fieldOf("positive").forGetter(Pole::positive))
                        .apply(instance, Pole::new));
    }
}
