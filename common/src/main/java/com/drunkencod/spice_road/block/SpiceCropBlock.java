package com.drunkencod.spice_road.block;

import java.util.Optional;
import java.util.function.Supplier;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;

import com.drunkencod.spice_road.platform.Services;
import com.drunkencod.spice_road.spice.Climate;
import com.drunkencod.spice_road.spice.Spice;
import com.drunkencod.spice_road.spice.region.SpiceRegionResolver;

/**
 * {@code CROP} template, covering both the {@code PICK} (Chili-Pepper-like)
 * and {@code BREAK} (Cumin-like) {@code HarvestAction}s.
 * <p>
 * A single class covers both harvest actions here because, for a
 * crop-shaped block, "pick" and "break" resolve to the exact same in-world
 * mechanic: the player breaks the mature block and the loot table decides
 * what falls out. The {@code HarvestAction} distinction only produces an
 * actual behavioural difference for Source Types harvested without
 * breaking a block (e.g. {@code TREE} strip/shear). If a future Spice needs
 * PICK/BREAK to diverge for crop-shaped blocks (e.g. a tool requirement on one
 * but not the other), split this class then rather than pre-emptively
 * duplicating it now.
 * <p>
 * Uses a distinct, non-flower model line from {@link FlowerPatchBlock}.
 */
public class SpiceCropBlock extends SpicePlantBlock {

    public SpiceCropBlock(BlockBehaviour.Properties properties, Supplier<? extends ItemLike> seedItem, Spice spice) {

        super(properties, seedItem, spice);
    }

    /**
     * Ground-type check only - see {@link #canGrow} for Spice Region gating.
     */
    @Override
    public boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {

        return super.canSurvive(state, level, pos);
    }

    /**
     * Gates growth by Spice Region support (see {@code SpiceRegionResolver}):
     * <ol>
     * <li>Hardy Spices (harvest/cultivation difficulty at or below
     * {@code IConfigHelper#getSpiceHardyHarvestDifficulty()}) are exempt -
     * they can be cultivated anywhere the ground allows.</li>
     * <li>Otherwise, if the restriction is enabled
     * ({@code IConfigHelper#isSpiceRegionPlantingRestricted()}), this Spice
     * must be the one the position's Spice Region actually resolves to.</li>
     * <li>If the restriction is disabled, growth is never gated here.</li>
     * </ol>
     * A Spice that fails this check isn't destroyed (see {@link #canSurvive}) -
     * it's simply never allowed to advance past stage 0.
     */
    @Override
    public boolean canGrow(ServerLevel level, BlockPos pos) {

        Spice spice = getSpice();
        if (spice.getHarvestDifficulty() <= Services.CONFIG.getSpiceHardyHarvestDifficulty())
            return true;

        if (!Services.CONFIG.isSpiceRegionPlantingRestricted())
            return true;

        double cellScale = Services.CONFIG.getSpiceRegionCellScale();
        double clusteringStrength = Services.CONFIG.getSpiceRegionClusteringStrength();
        Climate climate = Climate.fromBiome(level.getBiome(pos), pos);

        Optional<Spice> resolved = SpiceRegionResolver
                .resolve(level.getSeed(), cellScale, clusteringStrength, climate, pos.getX(), pos.getZ())
                .spice();
        return resolved.isPresent() && resolved.get() == spice;
    }
}
