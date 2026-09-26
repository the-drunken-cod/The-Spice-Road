package com.drunkencod.spice_road.registry;

import com.drunkencod.spice_road.Constants;

/**
 * Cross-loader service interface for registering the mod's creative tabs.
 */
public interface ICreativeTabHelper {
    /** Registry path of the generic creative tab. */
    public static final String TAB_GENERIC_KEY = Constants.MOD_ID + "_0_generic";
    /** Registry path of the spices creative tab. */
    public static final String TAB_SPICES_KEY = Constants.MOD_ID + "_1_spices";

    /** Translation key of the generic creative tab's title. */
    public static final String TAB_GENERIC_TR_KEY = "itemGroup." + Constants.MOD_ID + ".generic";
    /** Translation key of the spices creative tab's title. */
    public static final String TAB_SPICES_TR_KEY = "itemGroup." + Constants.MOD_ID + ".spices";

    /**
     * Registers the mod's creative tab with the platform's registry system.
     * On Fabric this performs the registration eagerly; on NeoForge it is a no-op
     * because registration is driven by the event bus (see
     * {@code com.drunkencod.spice_road.registry.NeoForgeCreativeTabHelper#initialize})
     */
    void register();
}
