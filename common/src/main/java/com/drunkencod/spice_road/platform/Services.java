package com.drunkencod.spice_road.platform;

import java.util.ServiceLoader;

import com.drunkencod.spice_road.Constants;
import com.drunkencod.spice_road.config.IConfigHelper;
import com.drunkencod.spice_road.platform.services.IPlatformHelper;
import com.drunkencod.spice_road.registry.IRegistryHelper;

public class Services {

    public static final IPlatformHelper PLATFORM = load(IPlatformHelper.class);
    public static final IRegistryHelper REGISTRY = load(IRegistryHelper.class);
    public static final IConfigHelper CONFIG = load(IConfigHelper.class);

    public static <T> T load(Class<T> clazz) {

        final T loadedService = ServiceLoader.load(clazz)
                .findFirst()
                .orElseThrow(() -> new NullPointerException("Failed to load service for " + clazz.getName()));
        Constants.LOG.debug("Loaded {} for service {}", loadedService, clazz);
        return loadedService;
    }
}
