package com.drunkencod.spice_road.compat.viewer;

import java.util.List;

import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import com.drunkencod.spice_road.Constants;
import com.drunkencod.spice_road.platform.Services;
import com.drunkencod.spice_road.rack.SpiceRackWood;
import com.drunkencod.spice_road.registry.ModBlocks;
import com.drunkencod.spice_road.registry.ModItems;
import com.drunkencod.spice_road.spice.Spice;

/**
 * One of the mod's own Recipe Viewer categories, shared by every supported
 * viewer. Each can be switched off in the client config.
 * <p>
 * Safe to load without any viewer installed, since the config schema
 * references it.
 */
public enum ViewerCategory {

    /** A Spice Item or Mix Preset and its Effective Profile. */
    SPICE_PROFILE("spice_profile", "spiceProfile", 160, 135),
    /** A Spice's growth and rarity facts. */
    SPICE_ORIGIN("spice_origin", "spiceOrigin", 160, 97),
    /** A drying recipe of the Drying Rack. */
    DRYING("drying", "drying", 120, 60);

    private final String path;
    private final String configKey;
    private final int width;
    private final int height;

    ViewerCategory(String path, String configKey, int width, int height) {
        this.path = path;
        this.configKey = configKey;
        this.width = width;
        this.height = height;
    }

    /** @return The category's ID, e.g. {@code spice_road:spice_profile}. */
    public ResourceLocation getId() {
        return ResourceLocation.fromNamespaceAndPath(Constants.MOD_ID, path);
    }

    /** @return The key of the category's config toggle within its section. */
    public String getConfigKey() {
        return configKey;
    }

    /** @return The width of every entry of this category, in pixels. */
    public int getWidth() {
        return width;
    }

    /**
     * @return The height of the tallest entry of this category, in pixels,
     *         which viewers with a fixed height per category use for all.
     */
    public int getHeight() {
        return height;
    }

    /**
     * @return The translation key of the category's title, in the form EMI
     *         looks up on its own: {@code emi.category.<namespace>.<path>}.
     */
    public String getTitleKey() {
        return "emi.category." + Constants.MOD_ID + "." + path;
    }

    /** @return The category's translatable title. */
    public Component getTitle() {
        return Component.translatable(getTitleKey());
    }

    /**
     * @return The stack the category is shown with: the Spice Grinder, a
     *         Spice's planting item, or the Drying Rack.
     */
    public ItemStack getIcon() {
        return switch (this) {
            case SPICE_PROFILE -> ModItems.SPICE_GRINDER.get().getDefaultInstance();
            case SPICE_ORIGIN -> {
                Item planting = SpiceOriginEntry.plantingItem(Spice.CINNAMON);
                yield planting != null ? planting.getDefaultInstance() : ItemStack.EMPTY;
            }
            case DRYING -> ModBlocks.DRYING_RACK_ITEMS.get(SpiceRackWood.OAK).get().getDefaultInstance();
        };
    }

    /**
     * @return The stacks that "use" this category, which viewers list as its
     *         workstations; empty for a category no single block or item
     *         stands for.
     */
    public List<ItemStack> getCatalysts() {
        return switch (this) {
            case SPICE_PROFILE -> List.of(ModItems.SPICE_GRINDER.get().getDefaultInstance());
            case SPICE_ORIGIN -> List.of();
            case DRYING -> ModBlocks.DRYING_RACK_ITEMS.values().stream()
                    .map(rack -> rack.get().getDefaultInstance()).toList();
        };
    }

    /** @return Whether the client config currently shows this category. */
    public boolean isEnabled() {
        return Services.CONFIG.isRecipeViewerCategoryEnabled(this);
    }
}
