package com.drunkencod.spice_road.registry;

import java.util.function.Supplier;

import net.minecraft.world.inventory.MenuType;

import com.drunkencod.spice_road.grinder.SpiceGrinderMenu;
import com.drunkencod.spice_road.platform.Services;
import com.drunkencod.spice_road.rack.SpiceRackMenu;

/**
 * Custom {@link MenuType}s, via {@link Services#REGISTRY}.
 */
public final class ModMenus {

    /** The menu of the Spice Grinder GUI. */
    public static final Supplier<MenuType<SpiceGrinderMenu>> SPICE_GRINDER = Services.REGISTRY
            .registerMenuType("spice_grinder", SpiceGrinderMenu::new);

    /** The menu of the Spice Rack GUI. */
    public static final Supplier<MenuType<SpiceRackMenu>> SPICE_RACK = Services.REGISTRY
            .registerMenuType("spice_rack", SpiceRackMenu::new);

    private ModMenus() {
    }

    /**
     * No-op other than forcing this class (and therefore its static
     * initializers) to load.
     */
    public static void register() {
    }
}
