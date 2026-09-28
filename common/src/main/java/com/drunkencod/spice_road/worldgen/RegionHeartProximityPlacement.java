package com.drunkencod.spice_road.worldgen;

import java.util.stream.Stream;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.levelgen.placement.PlacementContext;
import net.minecraft.world.level.levelgen.placement.PlacementModifier;
import net.minecraft.world.level.levelgen.placement.PlacementModifierType;

import com.drunkencod.spice_road.spice.region.RegionHeart;
import com.drunkencod.spice_road.spice.region.RegionHeartSearch;

/**
 * Placement modifier {@code spice_road:near_region_heart}: a rarity filter
 * whose chance fades out with the distance to the Region Heart of the Spice
 * Region the tested position lies in. Used to ring a Heart Grove with satellite
 * patches, so arriving at a heart reads as a spice-rich area rather than a
 * single patch.
 * <p>
 * The chance is {@code 1 / chance} at the heart itself and falls off linearly
 * to zero at {@code max_distance} blocks, which keeps the extra plants tied to
 * the heart instead of raising the Spice Plant rate everywhere.
 */
public final class RegionHeartProximityPlacement extends PlacementModifier {

    /** Codec of this modifier's JSON fields. */
    public static final MapCodec<RegionHeartProximityPlacement> CODEC = RecordCodecBuilder
            .mapCodec(instance -> instance.group(
                    ExtraCodecs.POSITIVE_INT.fieldOf("max_distance")
                            .forGetter(placement -> placement.maxDistance),
                    ExtraCodecs.POSITIVE_INT.fieldOf("chance").forGetter(placement -> placement.chance))
                    .apply(instance, RegionHeartProximityPlacement::new));

    private final int maxDistance;
    private final int chance;

    private RegionHeartProximityPlacement(int maxDistance, int chance) {
        this.maxDistance = maxDistance;
        this.chance = chance;
    }

    @Override
    public Stream<BlockPos> getPositions(PlacementContext context, RandomSource random, BlockPos pos) {
        ServerLevel level = context.getLevel().getLevel();
        RegionHeart heart = RegionHeartSearch.heartAt(level, pos).orElse(null);
        if (heart == null)
            return Stream.of();

        double distance = Math.sqrt(distanceSq(heart.pos(), pos));
        if (distance >= maxDistance)
            return Stream.of();

        float probability = (float) (1.0 - (distance / maxDistance)) / chance;
        return random.nextFloat() < probability ? Stream.of(pos) : Stream.of();
    }

    /** @return The squared horizontal distance between {@code a} and {@code b}. */
    private static double distanceSq(BlockPos a, BlockPos b) {
        return Mth.square((double) a.getX() - b.getX()) + Mth.square((double) a.getZ() - b.getZ());
    }

    @Override
    public PlacementModifierType<?> type() {
        return ModPlacementModifiers.NEAR_REGION_HEART.get();
    }
}
