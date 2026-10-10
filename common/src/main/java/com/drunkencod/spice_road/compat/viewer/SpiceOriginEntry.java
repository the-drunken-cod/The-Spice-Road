package com.drunkencod.spice_road.compat.viewer;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

import org.jetbrains.annotations.Nullable;

import net.minecraft.ChatFormatting;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import com.drunkencod.spice_road.Constants;
import com.drunkencod.spice_road.block.SpiceTree;
import com.drunkencod.spice_road.block.SpiceTrees;
import com.drunkencod.spice_road.block.SpiceVines;
import com.drunkencod.spice_road.platform.Services;
import com.drunkencod.spice_road.registry.ModItems;
import com.drunkencod.spice_road.spice.Season;
import com.drunkencod.spice_road.spice.SourceType;
import com.drunkencod.spice_road.spice.Spice;
import com.drunkencod.spice_road.tooltip.SpiceFlavorTooltips;
import com.drunkencod.spice_road.tooltip.SpiceProfileTooltips;

/**
 * An entry of {@link ViewerCategory#SPICE_ORIGIN}: a Spice's planting item
 * growing into its raw item, with its Tier, hardiness, Climate, yield and how
 * its plant is grown and harvested. Everything that depends on the config is
 * read when laid out.
 *
 * @param spice The Spice.
 */
public record SpiceOriginEntry(Spice spice) implements ViewerEntry {

    /** GUI sprite of the arrow between the planting and the raw item. */
    private static final ResourceLocation ARROW = ResourceLocation
            .withDefaultNamespace("container/furnace/burn_progress");

    /**
     * @return One entry per Spice that has a raw item, in enum order.
     */
    public static List<SpiceOriginEntry> all() {
        return Arrays.stream(Spice.values())
                .filter(spice -> Spice.byId(spice.getId()) != null)
                .map(SpiceOriginEntry::new)
                .toList();
    }

    @Override
    public ViewerCategory category() {
        return ViewerCategory.SPICE_ORIGIN;
    }

    @Override
    public ResourceLocation id() {
        return ResourceLocation.fromNamespaceAndPath(Constants.MOD_ID, "/spice_origin/" + spice.getId());
    }

    @Override
    public ViewerLayout layout(HolderLookup.Provider registries, int width) {
        ViewerLayout.Builder layout = ViewerLayout.builder();
        Item planting = plantingItem(spice);
        if (planting != null)
            layout.slot(ViewerLayout.Role.INPUT, 0, 0, List.of(planting.getDefaultInstance()), List.of());
        layout.sprite(ARROW, 22, 1, 24, 16);
        layout.slot(ViewerLayout.Role.OUTPUT, 50, 0,
                List.of(Objects.requireNonNull(ModItems.byId(spice.getId())).getDefaultInstance()), List.of());
        if (spice.requiresHarvestTool()) {
            List<ItemStack> tools = BuiltInRegistries.ITEM.getTag(spice.getHarvestToolTag())
                    .map(tag -> tag.stream().map(Holder::value).map(Item::getDefaultInstance).toList())
                    .orElse(List.of());
            layout.slot(ViewerLayout.Role.INPUT, width - ViewerLayout.SLOT_SIZE, 0, tools,
                    List.of(ViewerText.translatable("spice_origin.tool.slot").withStyle(ChatFormatting.GRAY)));
        }

        List<Line> lines = new ArrayList<>();
        lines.add(tierLine());
        lines.add(new Line(ViewerText.labeled("spice_origin.climate", spice.getClimate().getDisplayName()),
                List.of(ViewerText.translatable("spice_origin.climate.tooltip"))));
        lines.add(growthLine());
        lines.add(yieldLine());
        lines.add(plantLine());
        lines.add(harvestLine());
        lines.add(seasonsLine());

        int x = ViewerDrawing.PANEL_BORDER;
        int y = ViewerLayout.SLOT_SIZE + 2 + ViewerDrawing.PANEL_BORDER;
        int panelWidth = width - 2 * ViewerDrawing.PANEL_BORDER;
        // Scaled down as one block where the widest line wouldn't fit, so narrow hosts
        // keep the lines readable and aligned instead of shortening them
        int widest = lines.stream().mapToInt(line -> SpiceFlavorTooltips.measureWidth(line.text())).max().orElse(0);
        float scale = ViewerText.fitScale(widest, panelWidth);
        int lineHeight = Math.round(ViewerText.LINE_HEIGHT * scale);
        layout.panel(x, y, panelWidth, (int) Math.ceil((lines.size() * ViewerText.LINE_HEIGHT - 1) * scale));
        for (int i = 0; i < lines.size(); i++) {
            int lineY = y + i * lineHeight;
            layout.text(lines.get(i).text(), x, lineY, ViewerText.PANEL_TEXT_COLOR, true, panelWidth, scale);
            layout.tooltip(x, lineY, panelWidth, lineHeight - 1, lines.get(i).tooltip());
        }
        return layout.build();
    }

    /**
     * One line of the panel.
     *
     * @param text    The line.
     * @param tooltip What hovering it explains; may be empty.
     */
    private record Line(Component text, List<Component> tooltip) {
    }

    private Line tierLine() {
        MutableComponent tier = Component.empty()
                .append(SpiceProfileTooltips.tierIcon(spice.getTier()))
                .append(" ")
                .append(SpiceProfileTooltips.tierName(spice.getTier()).withStyle(spice.getTier().getRarity().color()));
        return new Line(ViewerText.labeled("spice_origin.tier", tier),
                List.of(ViewerText.translatable("spice_origin.tier.tooltip")));
    }

    private Line growthLine() {
        String growth;
        if (spice.getHarvestDifficulty() <= Services.CONFIG.getSpiceHardyHarvestDifficulty())
            growth = "hardy";
        else if (Services.CONFIG.isSpiceRegionPlantingRestricted())
            growth = "region_bound";
        else
            growth = "unrestricted";
        String key = "spice_origin.growth." + growth;
        return new Line(ViewerText.labeled("spice_origin.growth", ViewerText.translatable(key)),
                List.of(ViewerText.translatable(key + ".tooltip")));
    }

    private Line yieldLine() {
        double multiplier = spice.getSourceType() == SourceType.TREE
                ? Services.CONFIG.getSpiceTreeHarvestYieldMultiplier()
                : Services.CONFIG.getSpicePlantHarvestYieldMultiplier();
        Component amount = ViewerText.translatable("spice_origin.yield.value",
                ViewerText.amount(spice.getDropAmount() * multiplier));
        return new Line(ViewerText.labeled("spice_origin.yield", amount),
                List.of(ViewerText.translatable("spice_origin.yield.tooltip")));
    }

    private Line plantLine() {
        MutableComponent plant = ViewerText.translatable(
                "source_type." + spice.getSourceType().name().toLowerCase(Locale.ROOT));
        if (spice.isAquatic())
            plant = ViewerText.translatable("spice_origin.plant.aquatic", plant);
        return new Line(ViewerText.labeled("spice_origin.plant", plant), List.of());
    }

    private Line harvestLine() {
        MutableComponent action = ViewerText.translatable(
                "harvest_action." + spice.getHarvestAction().name().toLowerCase(Locale.ROOT));
        List<Component> tooltip = new ArrayList<>();
        if (spice.requiresHarvestTool())
            tooltip.add(ViewerText.translatable("spice_origin.tool.tooltip"));
        if (spice.requiresHandPick()) {
            action = ViewerText.translatable("spice_origin.harvest.hand_pick", action);
            tooltip.add(ViewerText.translatable("spice_origin.harvest.hand_pick.tooltip"));
        }
        return new Line(ViewerText.labeled("spice_origin.harvest", action), tooltip);
    }

    private Line seasonsLine() {
        Component seasons;
        if (spice.getSeasons().equals(EnumSet.allOf(Season.class))) {
            seasons = ViewerText.translatable("spice_origin.seasons.all");
        } else {
            MutableComponent joined = Component.empty();
            for (Season season : Season.values()) {
                if (!spice.getSeasons().contains(season))
                    continue;
                if (!joined.getSiblings().isEmpty())
                    joined.append(", ");
                joined.append(ViewerText.translatable("season." + season.getId()));
            }
            seasons = joined;
        }
        return new Line(ViewerText.labeled("spice_origin.seasons", seasons),
                List.of(ViewerText.translatable("spice_origin.seasons.tooltip")));
    }

    /**
     * @param spice A Spice.
     * @return The item it's planted from: its sapling, vine or seeds/cuttings,
     *         whichever its Source Type uses.
     */
    public static @Nullable Item plantingItem(Spice spice) {
        SpiceTree tree = SpiceTrees.getRegistered().get(spice);
        if (tree != null)
            return tree.getSaplingItem().get();
        SpiceVines.RegisteredSpiceVine vine = SpiceVines.getRegistered().get(spice);
        if (vine != null)
            return vine.vineItem().get();
        return Spice.getSeedsById(spice.getId());
    }
}
