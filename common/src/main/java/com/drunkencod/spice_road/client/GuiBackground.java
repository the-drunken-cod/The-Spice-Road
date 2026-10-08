package com.drunkencod.spice_road.client;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;

import com.drunkencod.spice_road.Constants;
import com.drunkencod.spice_road.platform.Services;

/**
 * The background shared by all of the mod's GUIs, in a light and a dark
 * variant picked by the client config.
 */
public final class GuiBackground {

    private static final ResourceLocation LIGHT = texture("background");
    private static final ResourceLocation DARK = texture("background_dark");
    /** Size of the background texture; it is stretched to the GUI's size. */
    private static final int TEXTURE_SIZE = 9;
    /** Width of the background texture's border, which is never stretched. */
    private static final int BORDER = 4;

    private GuiBackground() {
    }

    private static ResourceLocation texture(String name) {
        return ResourceLocation.fromNamespaceAndPath(Constants.MOD_ID, "textures/gui/spice_grinder/" + name + ".png");
    }

    /**
     * Draws the background stretched to the given size, keeping its
     * {@value #BORDER} px border unstretched: the corners as they are, the
     * edges and the center stretched.
     *
     * @param graphics The graphics to draw with.
     * @param x        Left edge.
     * @param y        Top edge.
     * @param width    Width to cover.
     * @param height   Height to cover.
     */
    public static void draw(GuiGraphics graphics, int x, int y, int width, int height) {
        ResourceLocation texture = Services.CONFIG.isDarkMode() ? DARK : LIGHT;
        int b = BORDER;
        int[] destX = { x, x + b, x + width - b };
        int[] destW = { b, width - 2 * b, b };
        int[] destY = { y, y + b, y + height - b };
        int[] destH = { b, height - 2 * b, b };
        // The texture is square, so its columns and rows are cut the same way.
        int[] srcStart = { 0, b, TEXTURE_SIZE - b };
        int[] srcSpan = { b, TEXTURE_SIZE - 2 * b, b };
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 3; col++)
                graphics.blit(texture, destX[col], destY[row], destW[col], destH[row], srcStart[col], srcStart[row],
                        srcSpan[col], srcSpan[row], TEXTURE_SIZE, TEXTURE_SIZE);
        }
    }
}
