package com.drunkencod.spice_road.compat.viewer.jei;

import java.util.List;

import org.jetbrains.annotations.Nullable;

import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.builder.IRecipeSlotBuilder;
import mezz.jei.api.gui.builder.ITooltipBuilder;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.gui.widgets.IRecipeExtrasBuilder;
import mezz.jei.api.gui.widgets.IRecipeWidget;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.recipe.category.IRecipeCategory;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.navigation.ScreenPosition;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

import com.drunkencod.spice_road.compat.viewer.ViewerCategory;
import com.drunkencod.spice_road.compat.viewer.ViewerDrawing;
import com.drunkencod.spice_road.compat.viewer.ViewerEntry;
import com.drunkencod.spice_road.compat.viewer.ViewerLayout;

/**
 * Shows the entries of one {@link ViewerCategory} in JEI: the layout's slots
 * as JEI slots, everything else through one widget drawn by
 * {@link ViewerDrawing}.
 *
 * @param <T> The category's entry type.
 */
final class JeiViewerCategory<T extends ViewerEntry> implements IRecipeCategory<T> {

    private final ViewerCategory category;
    private final RecipeType<T> type;
    private final IDrawable icon;

    /**
     * @param category The category shown.
     * @param type     Its JEI recipe type.
     * @param icon     Its icon.
     */
    JeiViewerCategory(ViewerCategory category, RecipeType<T> type, IDrawable icon) {
        this.category = category;
        this.type = type;
        this.icon = icon;
    }

    @Override
    public RecipeType<T> getRecipeType() {
        return type;
    }

    @Override
    public Component getTitle() {
        return category.getTitle();
    }

    @Override
    public int getWidth() {
        return category.getWidth();
    }

    @Override
    public int getHeight() {
        return category.getHeight();
    }

    @Override
    public @Nullable IDrawable getIcon() {
        return icon;
    }

    @Override
    public void setRecipe(IRecipeLayoutBuilder builder, T entry, IFocusGroup focuses) {
        for (ViewerLayout.Slot slot : entry.layout(ViewerDrawing.registries()).slots()) {
            RecipeIngredientRole role = slot.role() == ViewerLayout.Role.INPUT ? RecipeIngredientRole.INPUT
                    : RecipeIngredientRole.OUTPUT;
            // JEI positions the item, not the slot border around it
            IRecipeSlotBuilder jeiSlot = builder.addSlot(role, slot.x() + 1, slot.y() + 1)
                    .setStandardSlotBackground()
                    .addItemStacks(slot.stacks());
            List<Component> lines = slot.tooltip();
            if (!lines.isEmpty())
                jeiSlot.addRichTooltipCallback((view, tooltip) -> tooltip.addAll(lines));
        }
    }

    @Override
    public void createRecipeExtras(IRecipeExtrasBuilder builder, T entry, IFocusGroup focuses) {
        builder.addWidget(new LayoutWidget(entry.layout(ViewerDrawing.registries())));
    }

    @Override
    public @Nullable ResourceLocation getRegistryName(T entry) {
        return entry.id();
    }

    /**
     * Draws a layout's decorations and shows its tooltip areas, laid out once
     * per shown entry.
     *
     * @param layout The layout.
     */
    private record LayoutWidget(ViewerLayout layout) implements IRecipeWidget {

        private static final ScreenPosition ORIGIN = new ScreenPosition(0, 0);

        @Override
        public ScreenPosition getPosition() {
            return ORIGIN;
        }

        @Override
        public void drawWidget(GuiGraphics graphics, double mouseX, double mouseY) {
            ViewerDrawing.draw(graphics, layout);
        }

        @Override
        public void getTooltip(ITooltipBuilder tooltip, double mouseX, double mouseY) {
            tooltip.addAll(ViewerDrawing.tooltipAt(layout, mouseX, mouseY));
        }
    }
}
