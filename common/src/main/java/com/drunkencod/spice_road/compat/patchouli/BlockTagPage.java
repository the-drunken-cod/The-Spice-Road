package com.drunkencod.spice_road.compat.patchouli;

import java.util.List;

import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;

import com.drunkencod.spice_road.compat.viewer.ViewerLayout;
import com.drunkencod.spice_road.compat.viewer.ViewerText;

/**
 * Page type {@code spice_road:block_tag}: the items of every block in a block
 * tag as a grid of slots, so a list of supported blocks stays in step with
 * the datapack that defines it. Blocks without an item are left out, and so
 * is whatever doesn't fit in {@link #MAX_ROWS} rows, which a trailing line
 * counts.
 * <p>
 * JSON: {@code "tag": "<block tag id>"}, e.g. {@code "spice_road:spice_storage"}.
 */
public class BlockTagPage extends ViewerEntryPage {

    /** Most rows of slots shown. */
    private static final int MAX_ROWS = 6;

    private String tag;

    @Override
    protected List<ViewerLayout> layouts(Level level, int width) {
        ResourceLocation id = tag == null ? null : ResourceLocation.tryParse(tag);
        if (id == null)
            return List.of();
        List<ItemStack> stacks = BuiltInRegistries.BLOCK.getTag(TagKey.create(Registries.BLOCK, id))
                .map(members -> members.stream().map(Holder::value).map(Block::asItem)
                        .filter(item -> item != Items.AIR).map(Item::getDefaultInstance).toList())
                .orElse(List.of());
        if (stacks.isEmpty())
            return List.of();

        int columns = Math.max(1, width / ViewerLayout.SLOT_SIZE);
        int shown = Math.min(stacks.size(), columns * MAX_ROWS);
        ViewerLayout.Builder layout = ViewerLayout.builder();
        for (int i = 0; i < shown; i++)
            layout.slot(ViewerLayout.Role.INPUT, (i % columns) * ViewerLayout.SLOT_SIZE,
                    (i / columns) * ViewerLayout.SLOT_SIZE, List.of(stacks.get(i)), List.of());
        if (shown < stacks.size()) {
            int rows = (shown + columns - 1) / columns;
            layout.text(ViewerText.translatable("and_more", stacks.size() - shown), 0,
                    rows * ViewerLayout.SLOT_SIZE + 2, ViewerText.DARK_TEXT_COLOR, false);
        }
        return List.of(layout.build());
    }

    @Override
    protected Component missingText() {
        return Component.translatable("spice_road.guide.unknown_tag", String.valueOf(tag));
    }
}
