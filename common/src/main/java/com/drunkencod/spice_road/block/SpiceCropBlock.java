package com.drunkencod.spice_road.block;

import java.util.function.Supplier;

import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.block.state.BlockBehaviour;

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

    public SpiceCropBlock(BlockBehaviour.Properties properties, Supplier<? extends ItemLike> seedItem) {

        super(properties, seedItem);
    }
}
