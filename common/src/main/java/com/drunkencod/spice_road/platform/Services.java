package com.drunkencod.spice_road.platform;

import java.util.ServiceLoader;

import com.drunkencod.spice_road.Constants;
import com.drunkencod.spice_road.config.IConfigHelper;
import com.drunkencod.spice_road.platform.services.IPlatformHelper;
import com.drunkencod.spice_road.registry.ICreativeTabHelper;
import com.drunkencod.spice_road.registry.IRegistryHelper;

/**
 * Entry point to the loader-specific service implementations, which each
 * loader provides through {@code META-INF/services} files.
 */
public class Services {

    /** Platform/environment queries. */
    public static final IPlatformHelper PLATFORM = load(IPlatformHelper.class);
    /** Block, item, and other registry entry registration. */
    public static final IRegistryHelper REGISTRY = load(IRegistryHelper.class);
    /** Mod config values. */
    public static final IConfigHelper CONFIG = load(IConfigHelper.class);
    /** Creative tab registration. */
    public static final ICreativeTabHelper CREATIVE_TAB = load(ICreativeTabHelper.class);

    /**
     * Loads the first available implementation of a service interface via
     * {@link ServiceLoader}.
     *
     * @param clazz The service interface.
     * @param <T>   The service type.
     * @return The loaded service implementation.
     * @throws NullPointerException If no implementation is available.
     */
    public static <T> T load(Class<T> clazz) {
        final T loadedService = ServiceLoader.load(clazz)
                .findFirst()
                .orElseThrow(() -> new NullPointerException("Failed to load service for " + clazz.getName()));
        Constants.LOG.debug("Loaded {} for service {}", loadedService, clazz);
        return loadedService;
    }
}
