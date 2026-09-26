package com.drunkencod.spice_road.block;

import java.util.ArrayList;
import java.util.List;

import org.jetbrains.annotations.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Stripped log of a {@link SpiceTree}. For {@link SpiceTree.HarvestPart#BARK}
 * trees, it drops the tree's Spice whenever it replaces its unstripped log.
 */
public class StrippedSpiceLogBlock extends RotatedPillarBlock {

    private static final ThreadLocal<Player> ITEM_USER = new ThreadLocal<>();

    private final SpiceTree tree;

    /**
     * @param properties Block properties.
     * @param tree       The tree this stripped log belongs to. Only stored, not
     *                   read during construction.
     */
    public StrippedSpiceLogBlock(BlockBehaviour.Properties properties, SpiceTree tree) {
        super(properties);
        this.tree = tree;
    }

    /**
     * Sets the player currently using an item on a block, so a strip performed
     * by a creative-mode player doesn't drop bark. Set and cleared around every
     * {@code ItemStack#useOn} call by {@code ItemStackMixin}.
     *
     * @param player The player using the item, or {@code null} once the use
     *               is over (or if it isn't performed by a player).
     */
    public static void setItemUser(@Nullable Player player) {
        ITEM_USER.set(player);
    }

    /**
     * Drops the tree's Spice when this block replaces its unstripped log,
     * unless a creative-mode player stripped it.
     */
    @Override
    protected void onPlace(BlockState state, Level level, BlockPos pos,
            BlockState oldState, boolean movedByPiston) {
        super.onPlace(state, level, pos, oldState, movedByPiston);

        if (level.isClientSide() || movedByPiston || tree.getHarvestPart() != SpiceTree.HarvestPart.BARK
                || !oldState.is(tree.getLog().get()))
            return;

        Player user = ITEM_USER.get();
        if (user != null && user.isCreative())
            return;

        ItemStack bark = tree.rollHarvest(level.getRandom());
        if (bark.isEmpty())
            return;

        dropFromOpenFace(level, pos, bark);
    }

    /**
     * Drops {@code stack} from a random face of this block that isn't covered
     * by a solid block, since the face the log was stripped from isn't known
     * here. Falls back to popping it from the block's center.
     */
    private static void dropFromOpenFace(Level level, BlockPos pos, ItemStack stack) {
        List<Direction> openFaces = new ArrayList<>(Direction.values().length);
        for (Direction direction : Direction.values()) {
            if (!level.getBlockState(pos.relative(direction)).isSolidRender(level, pos.relative(direction)))
                openFaces.add(direction);
        }

        if (openFaces.isEmpty())
            Block.popResource(level, pos, stack);
        else
            Block.popResourceFromFace(level, pos, openFaces.get(level.getRandom().nextInt(openFaces.size())), stack);
    }
}
