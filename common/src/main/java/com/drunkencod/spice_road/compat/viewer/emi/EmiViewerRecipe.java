package com.drunkencod.spice_road.compat.viewer.emi;

import java.util.List;

import org.jetbrains.annotations.Nullable;

import dev.emi.emi.api.recipe.EmiRecipe;
import dev.emi.emi.api.recipe.EmiRecipeCategory;
import dev.emi.emi.api.stack.EmiIngredient;
import dev.emi.emi.api.stack.EmiStack;
import dev.emi.emi.api.widget.Bounds;
import dev.emi.emi.api.widget.SlotWidget;
import dev.emi.emi.api.widget.Widget;
import dev.emi.emi.api.widget.WidgetHolder;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

import com.drunkencod.spice_road.compat.viewer.ViewerCategory;
import com.drunkencod.spice_road.compat.viewer.ViewerDrawing;
import com.drunkencod.spice_road.compat.viewer.ViewerEntry;
import com.drunkencod.spice_road.compat.viewer.ViewerLayout;

/**
 * Shows one {@link ViewerEntry} in EMI: the layout's slots as EMI slots,
 * everything else through one widget drawn by {@link ViewerDrawing}.
 */
final class EmiViewerRecipe implements EmiRecipe {

    private final ViewerEntry entry;
    private final EmiRecipeCategory category;
    private final List<EmiIngredient> inputs;
    private final List<EmiStack> outputs;
    private final int height;

    /**
     * Lays the entry out once to index its slots; the widgets are laid out
     * again whenever EMI shows the entry.
     *
     * @param entry    The entry.
     * @param category The EMI category of its {@link ViewerCategory}.
     */
    EmiViewerRecipe(ViewerEntry entry, EmiRecipeCategory category) {
        this.entry = entry;
        this.category = category;
        ViewerLayout layout = entry.layout(ViewerDrawing.registries());
        this.inputs = layout.stacksOf(ViewerLayout.Role.INPUT).stream().map(EmiViewerRecipe::ingredient).toList();
        this.outputs = layout.stacksOf(ViewerLayout.Role.OUTPUT).stream()
                .flatMap(stacks -> stacks.stream().map(EmiStack::of))
                .toList();
        this.height = layout.height();
    }

    @Override
    public EmiRecipeCategory getCategory() {
        return category;
    }

    @Override
    public @Nullable ResourceLocation getId() {
        return entry.id();
    }

    @Override
    public List<EmiIngredient> getInputs() {
        return inputs;
    }

    @Override
    public List<EmiStack> getOutputs() {
        return outputs;
    }

    @Override
    public int getDisplayWidth() {
        return entry.category().getWidth();
    }

    @Override
    public int getDisplayHeight() {
        return height;
    }

    @Override
    public boolean supportsRecipeTree() {
        // Only drying is a way of making its output; the other entries just describe theirs
        return entry.category() == ViewerCategory.DRYING;
    }

    @Override
    public void addWidgets(WidgetHolder widgets) {
        ViewerLayout layout = entry.layout(ViewerDrawing.registries());
        // Added first, so it draws below the slots and lets their tooltips through
        widgets.add(new LayoutWidget(layout, widgets.getWidth(), widgets.getHeight()));
        for (ViewerLayout.Slot slot : layout.slots()) {
            SlotWidget emiSlot = widgets.addSlot(ingredient(slot.stacks()), slot.x(), slot.y());
            for (Component line : slot.tooltip())
                emiSlot.appendTooltip(line);
            if (slot.role() == ViewerLayout.Role.OUTPUT)
                emiSlot.recipeContext(this);
        }
    }

    private static EmiIngredient ingredient(List<ItemStack> stacks) {
        return EmiIngredient.of(stacks.stream().map(EmiStack::of).toList());
    }

    /** Draws a layout's decorations and shows its tooltip areas. */
    private static final class LayoutWidget extends Widget {

        private final ViewerLayout layout;
        private final Bounds bounds;

        LayoutWidget(ViewerLayout layout, int width, int height) {
            this.layout = layout;
            this.bounds = new Bounds(0, 0, width, height);
        }

        @Override
        public Bounds getBounds() {
            return bounds;
        }

        @Override
        public void render(GuiGraphics graphics, int mouseX, int mouseY, float delta) {
            ViewerDrawing.draw(graphics, layout);
        }

        @Override
        public List<ClientTooltipComponent> getTooltip(int mouseX, int mouseY) {
            return ViewerDrawing.tooltipAt(layout, mouseX, mouseY).stream()
                    .map(line -> ClientTooltipComponent.create(line.getVisualOrderText()))
                    .toList();
        }
    }
}
