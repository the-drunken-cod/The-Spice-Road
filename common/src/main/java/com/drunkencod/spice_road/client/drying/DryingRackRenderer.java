package com.drunkencod.spice_road.client.drying;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;

import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;

import com.drunkencod.spice_road.drying.DryingRackBlock;
import com.drunkencod.spice_road.drying.DryingRackBlockEntity;

/**
 * Draws the items on a Drying Rack: each side's input hangs from its hook, and
 * its outputs lie on the shelf, the secondary output stacked just above the
 * primary one. The rack's own model is a plain block model; this only places
 * items, at the constants below, which are measured in blocks from the centre
 * of the rack's footprint with the rack facing north (its front toward -Z).
 */
public class DryingRackRenderer implements BlockEntityRenderer<DryingRackBlockEntity> {

    /**
     * How far each hook and shelf spot is from the middle of the rack, along it.
     */
    private static final float SIDE_OFFSET = 3 / 16f;
    /** Height and depth of the centre of an input hanging from its hook. */
    private static final float HANGING_Y = 8.4f / 16;
    private static final float HANGING_Z = -0.75f / 16;
    /** Height of the shelf's top, where outputs lie. */
    private static final float SHELF_Y = 4.2f / 16;
    /**
     * How far the secondary output sits above (positive) or below the primary one,
     * and how far behind it.
     */
    private static final float SECONDARY_LIFT = 0.4f / 16;
    private static final float SECONDARY_SHIFT = 1.5f / 16;
    private static final float ITEM_SCALE = 0.4f;

    private final ItemRenderer itemRenderer;

    /** @param context The renderer's provider context. */
    public DryingRackRenderer(BlockEntityRendererProvider.Context context) {
        this.itemRenderer = context.getItemRenderer();
    }

    @Override
    public void render(DryingRackBlockEntity rack, float partialTick, PoseStack pose, MultiBufferSource buffers,
            int light, int overlay) {
        BlockState state = rack.getBlockState();
        if (rack.isEmpty() || !state.hasProperty(DryingRackBlock.FACING))
            return;
        Direction facing = state.getValue(DryingRackBlock.FACING);
        pose.pushPose();
        pose.translate(0.5, 0, 0.5);
        // Same rotation the blockstate gives the model: 0 for north, clockwise from
        // there.
        pose.mulPose(Axis.YP.rotationDegrees(-((facing.toYRot() + 180) % 360)));
        int seed = (int) rack.getBlockPos().asLong();
        for (int side = 0; side < DryingRackBlockEntity.SIDES; side++) {
            float x = side == DryingRackBlockEntity.LEFT ? SIDE_OFFSET : -SIDE_OFFSET;
            renderItem(pose, buffers, rack.getItem(DryingRackBlockEntity.input(side)), x, HANGING_Y, HANGING_Z, false,
                    light, overlay, seed + side);
            renderItem(pose, buffers, rack.getItem(DryingRackBlockEntity.output(side)), x, SHELF_Y, 0, true, light,
                    overlay, seed + side);
            renderItem(pose, buffers, rack.getItem(DryingRackBlockEntity.secondary(side)), x,
                    SHELF_Y + SECONDARY_LIFT, SECONDARY_SHIFT, true, light, overlay, seed + side);
        }
        pose.popPose();
    }

    private void renderItem(PoseStack pose, MultiBufferSource buffers, ItemStack stack, float x, float y, float z,
            boolean lyingFlat, int light, int overlay, int seed) {
        if (stack.isEmpty())
            return;
        pose.pushPose();
        pose.translate(x, y, z);
        // FIXED already turns the item half way around the vertical axis, so +90 degrees lays it face up,
        // its top pointing away from someone standing at the rack's front.
        if (lyingFlat)
            pose.mulPose(Axis.XP.rotationDegrees(90));
        pose.scale(ITEM_SCALE, ITEM_SCALE, ITEM_SCALE);
        itemRenderer.renderStatic(stack, ItemDisplayContext.FIXED, light, overlay, pose, buffers, null, seed);
        pose.popPose();
    }
}
