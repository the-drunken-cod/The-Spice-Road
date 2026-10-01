package com.drunkencod.spice_road.worldgen;

import java.util.stream.Stream;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.tags.TagKey;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.levelgen.placement.PlacementContext;
import net.minecraft.world.level.levelgen.placement.PlacementModifier;
import net.minecraft.world.level.levelgen.placement.PlacementModifierType;

/**
 * Placement modifier {@code spice_road:biome_tag}: only lets a position
 * through if its biome is (or, with {@code invert}, isn't) in a biome tag. Lets
 * one feature have biome-specific variants, e.g. denser satellites in hills.
 */
public final class BiomeTagPlacement extends PlacementModifier {

    /** Codec of this modifier's JSON fields. */
    public static final MapCodec<BiomeTagPlacement> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            TagKey.hashedCodec(Registries.BIOME).fieldOf("tag").forGetter(placement -> placement.tag),
            Codec.BOOL.optionalFieldOf("invert", false)
                    .forGetter(placement -> placement.invert))
            .apply(instance, BiomeTagPlacement::new));

    private final TagKey<Biome> tag;
    private final boolean invert;

    private BiomeTagPlacement(TagKey<Biome> tag, boolean invert) {
        this.tag = tag;
        this.invert = invert;
    }

    @Override
    public Stream<BlockPos> getPositions(PlacementContext context, RandomSource random, BlockPos pos) {
        boolean inTag = context.getLevel().getBiome(pos).is(tag);
        return inTag != invert ? Stream.of(pos) : Stream.of();
    }

    @Override
    public PlacementModifierType<?> type() {
        return ModPlacementModifiers.BIOME_TAG.get();
    }
}
