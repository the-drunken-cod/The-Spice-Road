package com.drunkencod.spice_road.compat.viewer;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

import net.minecraft.ChatFormatting;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import com.drunkencod.spice_road.Constants;
import com.drunkencod.spice_road.mix.MixPreset;
import com.drunkencod.spice_road.mix.PresetSlot;
import com.drunkencod.spice_road.mix.SpiceMixes;
import com.drunkencod.spice_road.spice.SpiceProfile;
import com.drunkencod.spice_road.spice.SpiceProfiles;
import com.drunkencod.spice_road.tooltip.SpiceFlavorTooltips;

/**
 * An entry of {@link ViewerCategory#SPICE_PROFILE}: a Spice Item or a Mix
 * Preset, and the Effective Profile it brings into the Spice Grinder.
 */
public sealed interface SpiceProfileEntry extends ViewerEntry {

    /** Score magnitude at which a flavor bar is full, as on item tooltips. */
    double BAR_SCALE = 1D;

    /** Most header rows a preset's slots take; {@link ViewerCategory} is sized for them. */
    int MAX_ROWS = 2;

    /**
     * @param width Width the entry is laid out for.
     * @return How many preset slots fit in one header row, leaving room for the jar.
     */
    static int slotsPerRow(int width) {
        return Math.max(1, (width - 2 * ViewerLayout.SLOT_SIZE + 4) / ViewerLayout.SLOT_SIZE);
    }

    @Override
    default ViewerCategory category() {
        return ViewerCategory.SPICE_PROFILE;
    }

    /**
     * @param data The synced data to build from.
     * @return One entry per Spice Item, in item ID order, followed by one per
     *         Mix Preset that has spices to show, in preset ID order.
     */
    static List<SpiceProfileEntry> all(ViewerRefresh.Snapshot data) {
        List<SpiceProfileEntry> entries = new ArrayList<>();
        data.profiles().entrySet().stream()
                .sorted(Comparator.comparing(entry -> BuiltInRegistries.ITEM.getKey(entry.getKey()).toString()))
                .forEach(entry -> entries.add(new OfItem(entry.getKey(), entry.getValue())));
        data.presets().forEach((id, preset) -> SpiceMixes.ofPreset(id, preset)
                .ifPresent(jar -> entries.add(new OfPreset(id, preset, jar))));
        return entries;
    }

    /**
     * A Spice Item's Default Profile.
     *
     * @param item    The Spice Item.
     * @param profile Its Default Profile.
     */
    record OfItem(Item item, SpiceProfile profile) implements SpiceProfileEntry {

        @Override
        public ResourceLocation id() {
            ResourceLocation itemId = BuiltInRegistries.ITEM.getKey(item);
            return ResourceLocation.fromNamespaceAndPath(Constants.MOD_ID,
                    "/spice_profile/item/" + itemId.getNamespace() + "/" + itemId.getPath());
        }

        @Override
        public ViewerLayout layout(HolderLookup.Provider registries, int width) {
            ItemStack stack = item.getDefaultInstance();
            int nameX = ViewerLayout.SLOT_SIZE + 4;
            ViewerLayout.Builder layout = ViewerLayout.builder()
                    .slot(ViewerLayout.Role.INPUT, 0, 0, List.of(stack), List.of())
                    .text(stack.getHoverName(), nameX, 5, ViewerText.DARK_TEXT_COLOR, false, width - nameX, 1F);

            return addPanel(layout, width, ViewerLayout.SLOT_SIZE, Optional.empty(),
                    SpiceFlavorTooltips.formatFlavorAxes(SpiceProfiles.effective(profile), BAR_SCALE, false),
                    List.of());
        }
    }

    /**
     * A Mix Preset, with one batch of its proportions.
     *
     * @param presetId The preset's ID.
     * @param preset   The preset.
     * @param jar      The Spice Mix of one batch.
     */
    record OfPreset(ResourceLocation presetId, MixPreset preset, ItemStack jar) implements SpiceProfileEntry {

        @Override
        public ResourceLocation id() {
            return ResourceLocation.fromNamespaceAndPath(Constants.MOD_ID,
                    "/spice_profile/preset/" + presetId.getNamespace() + "/" + presetId.getPath());
        }

        @Override
        public ViewerLayout layout(HolderLookup.Provider registries, int width) {
            ViewerLayout.Builder layout = ViewerLayout.builder();
            List<PresetSlot> slots = preset.slots();
            int perRow = slotsPerRow(width);
            int shown = Math.min(slots.size(), perRow * MAX_ROWS);
            for (int i = 0; i < shown; i++) {
                PresetSlot slot = slots.get(i);
                List<ItemStack> stacks = slot.members().stream().map(item -> new ItemStack(item, slot.count()))
                        .toList();
                List<Component> tooltip = slot.source().right().isPresent()
                        ? List.of(ViewerText.translatable("spice_profile.any_of_tag").withStyle(ChatFormatting.GRAY))
                        : List.of();
                layout.slot(ViewerLayout.Role.INPUT, (i % perRow) * ViewerLayout.SLOT_SIZE,
                        (i / perRow) * ViewerLayout.SLOT_SIZE, stacks, tooltip);
            }
            layout.slot(ViewerLayout.Role.OUTPUT, width - ViewerLayout.SLOT_SIZE, 0, List.of(jar), List.of());

            int rows = Math.max(1, (shown + perRow - 1) / perRow);
            Component name = Component.translatable(SpiceMixes.presetTranslationKey(presetId))
                    .withStyle(ChatFormatting.YELLOW);
            List<Component> axes = SpiceProfiles.getEffectiveMix(jar)
                    .map(effective -> SpiceFlavorTooltips.formatFlavorAxes(effective, BAR_SCALE, false))
                    .orElse(List.of());
            return addPanel(layout, width, rows * ViewerLayout.SLOT_SIZE, Optional.of(name), axes,
                    List.of(ViewerText.translatable("spice_profile.averaged")));
        }
    }

    /**
     * Adds the panel below the header and builds the layout. The Flavor Axis
     * lines are scaled down as one block where the widest one wouldn't fit
     * (e.g. with both axis labels and the values shown), so their bars stay
     * aligned; the title is shortened instead.
     *
     * @param layout       The layout so far.
     * @param entryWidth   Width the entry is laid out for.
     * @param headerHeight Height of the slot rows above the panel.
     * @param title        The line above the axes, if any.
     * @param axes         The Flavor Axis lines.
     * @param titleTooltip Tooltip of the title; may be empty.
     * @return The finished layout.
     */
    private static ViewerLayout addPanel(ViewerLayout.Builder layout, int entryWidth, int headerHeight,
            Optional<Component> title, List<Component> axes, List<Component> titleTooltip) {
        int x = ViewerDrawing.PANEL_BORDER;
        int y = headerHeight + 2 + ViewerDrawing.PANEL_BORDER;
        int width = entryWidth - 2 * ViewerDrawing.PANEL_BORDER;

        int lineY = y;
        if (title.isPresent()) {
            layout.text(title.get(), x, lineY, ViewerText.PANEL_TEXT_COLOR, true, width, 1F);
            layout.tooltip(x, lineY, width, ViewerText.LINE_HEIGHT - 1, titleTooltip);
            lineY += ViewerText.LINE_HEIGHT;
        }

        int widest = axes.stream().mapToInt(SpiceFlavorTooltips::measureWidth).max().orElse(0);
        float scale = ViewerText.fitScale(widest, width);
        for (int i = 0; i < axes.size(); i++)
            layout.text(axes.get(i), x, lineY + Math.round(i * ViewerText.LINE_HEIGHT * scale),
                    ViewerText.PANEL_TEXT_COLOR, true, width, scale);
        int bottom = axes.isEmpty() ? lineY - 1
                : lineY + (int) Math.ceil((axes.size() * ViewerText.LINE_HEIGHT - 1) * scale);
        layout.panel(x, y, width, Math.max(bottom - y, ViewerText.LINE_HEIGHT - 1));
        return layout.build();
    }
}
