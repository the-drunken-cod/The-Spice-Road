package com.drunkencod.spice_road.compat.patchouli;

import com.drunkencod.spice_road.Constants;
import com.drunkencod.spice_road.platform.Services;

/**
 * Entry point of the Flavor Folio's Patchouli integration. The book itself is
 * plain data, inert without Patchouli; only its custom page types need code.
 */
public final class FlavorFolio {

    /** Mod ID of Patchouli. */
    public static final String PATCHOULI_MOD_ID = "patchouli";

    /** ID of the book, as the {@code patchouli:book} component names it. */
    public static final String BOOK_ID = Constants.MOD_ID + ":flavor_folio";

    private FlavorFolio() {
    }

    /**
     * Registers the book's page types if Patchouli is installed; a no-op
     * otherwise, and if Patchouli's API changed in a way that breaks them. Call
     * from each loader's client setup.
     */
    public static void registerPageTypes() {
        if (!Services.PLATFORM.isModLoaded(PATCHOULI_MOD_ID))
            return;
        try {
            FlavorFolioPages.register();
        } catch (RuntimeException | LinkageError e) {
            // A Patchouli that changed under us must never stop the game from starting
            Constants.LOG.error("Could not register the {} page types with Patchouli", BOOK_ID, e);
        }
    }
}
