package com.drunkencod.spice_road.client.rack;

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
import net.minecraft.world.level.block.state.properties.AttachFace;

import com.drunkencod.spice_road.platform.Services;
import com.drunkencod.spice_road.rack.SpiceRackBlock;
import com.drunkencod.spice_road.rack.SpiceRackBlockEntity;

/**
 * Draws the items on a Spice Rack, one per slot, upright and facing outward.
 * The rack's own model is a plain block model; this only places items, at the
 * constants below, which are measured in blocks.
 * <p>
 * On a wall each shelf holds a row of {@value SpiceRackBlockEntity#TIER_SIZE}
 * items, the first slot on the viewer's left. On the floor or ceiling each
 * tier holds a ring of them: every item is moved out from the pole and then
 * turned a quarter further than the one before. The ring on the larger
 * platform is a further 45 degrees round and further out than the one on the
 * smaller platform. The larger platform is the lower one on a floor rack and
 * the upper one on a ceiling rack, so the two tiers swap places there. Items
 * stand on the platforms of a floor rack and hang beneath those of a ceiling
 * rack.
 */
public class SpiceRackRenderer implements BlockEntityRenderer<SpiceRackBlockEntity> {

    // #region wall

    /** Height of the top of each shelf, lower first. */
    private static final float[] SHELF_TOPS = { 3 / 16f, 9 / 16f };
    /**
     * How far in front of the middle of the block an item on a shelf stands, away
     * from the backboard.
     */
    private static final float SHELF_DEPTH = 5.5f / 16;
    /** Distance between the middles of neighboring items in a row. */
    private static final float ROW_SPACING = 3.6f / 16;
    private static final float ROW_ITEM_SCALE = 0.24f;
    /**
     * How far an odd-numbered item of a row is moved toward the viewer and
     * upward, so it doesn't z-fight with its overlapping neighbors.
     */
    private static final float ROW_ODD_LIFT = 0.001f;

    // #region floor and ceiling

    /** Height of the top of each platform of a floor rack, lower first. */
    private static final float[] PLATFORM_TOPS = { 1 / 16f, 9 / 16f };
    /** Height of the underside of each platform of a ceiling rack, lower first. */
    private static final float[] PLATFORM_UNDERSIDES = { 7 / 16f, 15 / 16f };
    /**
     * How far from the pole the middle of an item on the larger platform stands.
     */
    private static final float LARGE_RING_RADIUS = 4.25f / 16;
    /**
     * How far from the pole the middle of an item on the smaller platform stands.
     */
    private static final float SMALL_RING_RADIUS = 2.5f / 16;
    /** How far each tier's ring is turned on a floor rack, lower tier first; a ceiling rack swaps the tiers. */
    private static final float[] RING_OFFSETS = { 45, 0 };
    private static final float RING_ITEM_SCALE = 0.3f;

    private static final int TIERS_LAST = SpiceRackBlockEntity.TIERS - 1;

    private final ItemRenderer itemRenderer;

    /** @param context The renderer's provider context. */
    public SpiceRackRenderer(BlockEntityRendererProvider.Context context) {
        this.itemRenderer = context.getItemRenderer();
    }

    @Override
    public int getViewDistance() {
        return Services.CONFIG.getSpiceRackItemRenderDistance();
    }

    @Override
    public void render(SpiceRackBlockEntity rack, float partialTick, PoseStack pose, MultiBufferSource buffers,
            int light, int overlay) {
        BlockState state = rack.getBlockState();
        if (rack.isEmpty() || !state.hasProperty(SpiceRackBlock.FACE) || !Services.CONFIG.areSpiceRackItemsRendered())
            return;
        pose.pushPose();
        pose.translate(0.5, 0, 0.5);
        AttachFace face = state.getValue(SpiceRackBlock.FACE);
        if (face == AttachFace.WALL) {
            Direction facing = state.getValue(SpiceRackBlock.FACING);
            // Same rotation the blockstate gives the model: 0 for north, clockwise from
            // there.
            pose.mulPose(Axis.YP.rotationDegrees(-((facing.toYRot() + 180) % 360)));
            renderShelves(rack, pose, buffers, light, overlay);
        } else {
            renderRings(rack, face == AttachFace.CEILING, pose, buffers, light, overlay);
        }
        pose.popPose();
    }

    /**
     * Draws the rows of a wall rack, the rack facing north and the pose at the
     * middle of its block's floor.
     */
    private void renderShelves(SpiceRackBlockEntity rack, PoseStack pose, MultiBufferSource buffers, int light,
            int overlay) {
        for (int tier = 0; tier < SpiceRackBlockEntity.TIERS; tier++) {
            for (int index = 0; index < SpiceRackBlockEntity.TIER_SIZE; index++) {
                pose.pushPose();
                // Slot 0 is on the viewer's left, who looks south at a north-facing rack: east.
                // The viewer's side is north, so moving an item toward them is -Z.
                float lift = index % 2 == 1 ? ROW_ODD_LIFT : 0;
                pose.translate((1.5f - index) * ROW_SPACING, SHELF_TOPS[tier] + ROW_ITEM_SCALE / 2 + lift,
                        SHELF_DEPTH - lift);
                renderItem(rack, tier * SpiceRackBlockEntity.TIER_SIZE + index, ROW_ITEM_SCALE, pose, buffers, light,
                        overlay);
                pose.popPose();
            }
        }
    }

    /**
     * Draws the rings of a floor or ceiling rack, with the pose at the middle of
     * its block's floor.
     */
    private void renderRings(SpiceRackBlockEntity rack, boolean hanging, PoseStack pose, MultiBufferSource buffers,
            int light, int overlay) {
        for (int tier = 0; tier < SpiceRackBlockEntity.TIERS; tier++) {
            float y = hanging ? PLATFORM_UNDERSIDES[tier] - RING_ITEM_SCALE / 2
                    : PLATFORM_TOPS[tier] + RING_ITEM_SCALE / 2;
            float radius = (hanging ? tier == 1 : tier == 0) ? LARGE_RING_RADIUS : SMALL_RING_RADIUS;
            float offset = RING_OFFSETS[hanging ? TIERS_LAST - tier : tier];
            for (int index = 0; index < SpiceRackBlockEntity.TIER_SIZE; index++) {
                pose.pushPose();
                // A turn about the pole, then out from it: item 0 is north of the pole, the
                // rest clockwise.
                pose.mulPose(Axis.YP.rotationDegrees(-(index * 90 + offset)));
                pose.translate(0, y, -radius);
                renderItem(rack, tier * SpiceRackBlockEntity.TIER_SIZE + index, RING_ITEM_SCALE, pose, buffers, light,
                        overlay);
                pose.popPose();
            }
        }
    }

    private void renderItem(SpiceRackBlockEntity rack, int slot, float scale, PoseStack pose,
            MultiBufferSource buffers, int light, int overlay) {
        ItemStack stack = rack.getItem(slot);
        if (stack.isEmpty())
            return;
        pose.scale(scale, scale, scale);
        itemRenderer.renderStatic(stack, ItemDisplayContext.FIXED, light, overlay, pose, buffers, rack.getLevel(),
                (int) rack.getBlockPos().asLong() + slot);
    }
}
