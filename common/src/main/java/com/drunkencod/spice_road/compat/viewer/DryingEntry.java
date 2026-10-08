package com.drunkencod.spice_road.compat.viewer;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import net.minecraft.ChatFormatting;
import net.minecraft.Util;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeManager;

import com.drunkencod.spice_road.block.SpiceBlockTags;
import com.drunkencod.spice_road.drying.DryingRackBlock;
import com.drunkencod.spice_road.drying.DryingRecipe;
import com.drunkencod.spice_road.platform.Services;
import com.drunkencod.spice_road.registry.ModRecipeTypes;
import com.drunkencod.spice_road.tooltip.SpiceFlavorTooltips;

/**
 * An entry of {@link ViewerCategory#DRYING}: one drying recipe, with its
 * results' chances and its drying time with and without heat at the
 * configured speed.
 *
 * @param recipe The recipe.
 */
public record DryingEntry(RecipeHolder<DryingRecipe> recipe) implements ViewerEntry {

    /** GUI sprite of the arrow between input and results. */
    private static final ResourceLocation ARROW = ResourceLocation.withDefaultNamespace("container/furnace/burn_progress");

    /** GUI sprite marking the heated drying time. */
    private static final ResourceLocation FLAME = ResourceLocation.withDefaultNamespace("container/furnace/lit_progress");

    /** Ticks per second the drying time is shown against. */
    private static final double TICKS_PER_SECOND = 20D;

    private static final int RESULT_X = 54;
    private static final int SECONDARY_X = RESULT_X + ViewerLayout.SLOT_SIZE + 4;
    private static final int SLOT_Y = 4;
    private static final int TIME_Y = 34;
    private static final int HEATED_Y = TIME_Y + 14;

    /**
     * @param recipes The client's recipe manager.
     * @return One entry per drying recipe, in recipe ID order.
     */
    public static List<DryingEntry> all(RecipeManager recipes) {
        return recipes.getAllRecipesFor(ModRecipeTypes.DRYING.get()).stream()
                .sorted((a, b) -> a.id().toString().compareTo(b.id().toString()))
                .map(DryingEntry::new)
                .toList();
    }

    @Override
    public ViewerCategory category() {
        return ViewerCategory.DRYING;
    }

    @Override
    public ResourceLocation id() {
        return recipe.id();
    }

    @Override
    public ViewerLayout layout(HolderLookup.Provider registries) {
        DryingRecipe drying = recipe.value();
        ViewerLayout.Builder layout = ViewerLayout.builder()
                .slot(ViewerLayout.Role.INPUT, 0, SLOT_Y, List.of(drying.getIngredient().getItems()), List.of())
                .sprite(ARROW, 24, SLOT_Y + 1, 24, 16);
        addResult(layout, drying.getResult(), RESULT_X, List.of());
        drying.getSecondaryResult().ifPresent(secondary -> addResult(layout, secondary, SECONDARY_X,
                List.of(ViewerText.translatable("drying.secondary").withStyle(ChatFormatting.GRAY))));

        double speed = Services.CONFIG.getDryingRackSpeedMultiplier();
        double seconds = drying.getDryingTime() / TICKS_PER_SECOND / speed;
        int width = ViewerCategory.DRYING.getWidth();
        Component time = ViewerText.translatable("drying.time", ViewerText.duration(seconds));
        layout.text(time, 0, TIME_Y, ViewerText.DARK_TEXT_COLOR, false, width, 1F);
        layout.tooltip(0, TIME_Y, Math.min(SpiceFlavorTooltips.measureWidth(time), width),
                ViewerText.LINE_HEIGHT - 1, List.of(ViewerText.translatable("drying.time.tooltip")));

        double heatedSeconds = seconds / Services.CONFIG.getDryingRackHeatedSpeedMultiplier();
        Component heated = ViewerText.translatable("drying.heated", ViewerText.duration(heatedSeconds));
        layout.sprite(FLAME, 0, HEATED_Y - 3, 14, 14);
        layout.text(heated, 17, HEATED_Y, ViewerText.DARK_TEXT_COLOR, false, width - 17, 1F);
        layout.tooltip(0, HEATED_Y - 3, Math.min(17 + SpiceFlavorTooltips.measureWidth(heated), width), 14,
                heatTooltip(registries));
        return layout.build();
    }

    /**
     * Adds a result's slot, and its chance below it if it isn't certain.
     *
     * @param layout  The layout to add to.
     * @param output  The result.
     * @param x       Left edge of the slot.
     * @param tooltip Lines to start the slot's extra tooltip with.
     */
    private static void addResult(ViewerLayout.Builder layout, DryingRecipe.Output output, int x,
            List<Component> tooltip) {
        List<Component> lines = new ArrayList<>(tooltip);
        if (output.chance() < 1D) {
            String percent = ViewerText.percent(output.chance());
            lines.add(ViewerText.translatable("drying.chance", percent).withStyle(ChatFormatting.GRAY));
            Component text = Component.literal(percent);
            int width = SpiceFlavorTooltips.measureWidth(text);
            layout.text(text, x + (ViewerLayout.SLOT_SIZE - width) / 2, SLOT_Y + ViewerLayout.SLOT_SIZE + 2,
                    ViewerText.DARK_TEXT_COLOR, false);
        }
        layout.slot(ViewerLayout.Role.OUTPUT, x, SLOT_Y, List.of(output.stack().copy()), lines);
    }

    /**
     * @param registries The client's registries, for the biome tag.
     * @return What heats a Drying Rack: the heat source blocks and the biomes
     *         that always heat it, by name.
     */
    private static List<Component> heatTooltip(HolderLookup.Provider registries) {
        List<Component> lines = new ArrayList<>();
        lines.add(ViewerText.translatable("drying.heated.tooltip"));

        List<Component> blocks = BuiltInRegistries.BLOCK.getTag(SpiceBlockTags.HEAT_SOURCES)
                .map(tag -> tag.stream().map(holder -> (Component) holder.value().getName()).toList())
                .orElse(List.of());
        if (!blocks.isEmpty()) {
            lines.add(ViewerText.translatable("drying.heated.blocks").withStyle(ChatFormatting.GRAY));
            lines.addAll(ViewerText.listed(blocks));
        }

        List<Component> biomes = registries.lookup(Registries.BIOME)
                .flatMap(lookup -> lookup.get(DryingRackBlock.ALWAYS_HEATED_BIOMES))
                .map(tag -> tag.stream()
                        .map(Holder::unwrapKey)
                        .flatMap(Optional::stream)
                        .map(key -> (Component) Component.translatable(Util.makeDescriptionId("biome", key.location())))
                        .toList())
                .orElse(List.of());
        if (!biomes.isEmpty()) {
            lines.add(ViewerText.translatable("drying.heated.biomes").withStyle(ChatFormatting.GRAY));
            lines.addAll(ViewerText.listed(biomes));
        }
        return lines;
    }
}
