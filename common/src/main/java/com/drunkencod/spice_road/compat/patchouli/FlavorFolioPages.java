package com.drunkencod.spice_road.compat.patchouli;

import net.minecraft.resources.ResourceLocation;

import vazkii.patchouli.client.book.BookPage;
import vazkii.patchouli.client.book.ClientBookRegistry;

import com.drunkencod.spice_road.Constants;

/**
 * Registers the Flavor Folio's own page types with Patchouli. Only loaded
 * through {@link FlavorFolio#registerPageTypes()}, which checks that Patchouli
 * is present. Client-only.
 */
final class FlavorFolioPages {

    private FlavorFolioPages() {
    }

    /** Registers every page type under {@code spice_road:<name>}. */
    static void register() {
        add("spice_info", SpiceInfoPage.class);
        add("spice_profile", SpiceProfilePage.class);
        add("drying_recipe", DryingRecipePage.class);
        add("mix_preset", MixPresetPage.class);
        add("block_tag", BlockTagPage.class);
    }

    private static void add(String name, Class<? extends BookPage> page) {
        ClientBookRegistry.INSTANCE.pageTypes.put(ResourceLocation.fromNamespaceAndPath(Constants.MOD_ID, name), page);
    }
}
