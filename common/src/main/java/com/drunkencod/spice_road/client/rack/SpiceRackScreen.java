package com.drunkencod.spice_road.client.rack;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;

import com.drunkencod.spice_road.client.GuiBackground;
import com.drunkencod.spice_road.platform.Services;
import com.drunkencod.spice_road.rack.SpiceRackMenu;

/**
 * The Spice Rack GUI: the rack's two tiers of four slots above the player's
 * inventory, drawn with the mod's shared background and plain slot frames.
 */
public class SpiceRackScreen extends AbstractContainerScreen<SpiceRackMenu> {

    private static final int COLOR_LABEL_LIGHT = 0xFF404040;
    private static final int COLOR_LABEL_DARK = 0xFFE0E0E0;
    private static final int COLOR_SLOT_BORDER = 0xFF555555;
    private static final int COLOR_SLOT = 0xFF8B8B8B;

    /**
     * @param menu      The rack's menu.
     * @param inventory The player's inventory.
     * @param title     The rack's name.
     */
    public SpiceRackScreen(SpiceRackMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        imageWidth = SpiceRackMenu.WIDTH;
        imageHeight = SpiceRackMenu.HEIGHT;
        inventoryLabelY = SpiceRackMenu.INVENTORY_LABEL_Y;
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics, mouseX, mouseY, partialTick);
        super.render(graphics, mouseX, mouseY, partialTick);
        renderTooltip(graphics, mouseX, mouseY);
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        GuiBackground.draw(graphics, leftPos, topPos, imageWidth, imageHeight);
        for (Slot slot : menu.slots) {
            graphics.fill(leftPos + slot.x - 1, topPos + slot.y - 1, leftPos + slot.x + 17, topPos + slot.y + 17,
                    COLOR_SLOT_BORDER);
            graphics.fill(leftPos + slot.x, topPos + slot.y, leftPos + slot.x + 16, topPos + slot.y + 16, COLOR_SLOT);
        }
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        int color = Services.CONFIG.isDarkMode() ? COLOR_LABEL_DARK : COLOR_LABEL_LIGHT;
        graphics.drawString(font, title, titleLabelX, titleLabelY, color, false);
        graphics.drawString(font, playerInventoryTitle, inventoryLabelX, inventoryLabelY, color, false);
    }
}
