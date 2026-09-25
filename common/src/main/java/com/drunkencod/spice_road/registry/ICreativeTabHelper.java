package com.drunkencod.spice_road.registry;

import com.drunkencod.spice_road.Constants;

public interface ICreativeTabHelper {
    public static final String TAB_GENERIC_KEY = Constants.MOD_ID + "_0_generic";
    public static final String TAB_SPICES_KEY = Constants.MOD_ID + "_1_spices";

    public static final String TAB_GENERIC_TR_KEY = "itemGroup." + Constants.MOD_ID + ".generic";
    public static final String TAB_SPICES_TR_KEY = "itemGroup." + Constants.MOD_ID + ".spices";

    /**
     * Registers the mod's creative tab with the platform's registry system.
     * On Fabric this performs the registration eagerly; on NeoForge it is a no-op
     * because registration is driven by the event bus (see
     * {@code com.drunkencod.spice_road.registry.NeoForgeCreativeTabHelper#initialize})
     */
    void register();
}
