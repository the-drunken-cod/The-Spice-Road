package com.drunkencod.spice_road.registry;

import java.util.EnumMap;
import java.util.Locale;
import java.util.Map;

import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.saveddata.maps.MapDecorationType;

import com.drunkencod.spice_road.Constants;
import com.drunkencod.spice_road.platform.Services;
import com.drunkencod.spice_road.spice.Tier;

/**
 * Custom {@link MapDecorationType}s, via {@link Services#REGISTRY}: one Region
 * Heart marker per {@link Tier}, e.g. {@code spice_road:spice_heart_epic}.
 */
public final class ModMapDecorations {

    /** Region Heart markers per {@link Tier}. */
    private static final Map<Tier, Holder<MapDecorationType>> SPICE_HEARTS = new EnumMap<>(Tier.class);

    static {
        for (Tier tier : Tier.values()) {
            String id = "spice_heart_" + tier.name().toLowerCase(Locale.ROOT);
            SPICE_HEARTS.put(tier, Services.REGISTRY.registerMapDecorationType(id,
                    () -> new MapDecorationType(ResourceLocation.fromNamespaceAndPath(Constants.MOD_ID, id), true,
                            mapColorOf(tier), true, false)));
        }
    }

    private ModMapDecorations() {
    }

    /**
     * @param tier The mapped Spice's {@link Tier}.
     * @return The Region Heart marker for that tier.
     */
    public static Holder<MapDecorationType> spiceHeart(Tier tier) {
        return SPICE_HEARTS.get(tier);
    }

    /** @return The tint of a Spice Map item's markings, matching the tier's rarity color. */
    private static int mapColorOf(Tier tier) {
        return switch (tier) {
            case COMMON -> 0x8C7A5B;
            case UNCOMMON -> 0xC9A227;
            case RARE -> 0x3FA7B5;
            case EPIC -> 0x9B4FC9;
        };
    }

    /**
     * No-op other than forcing this class (and therefore its static
     * initializer) to load.
     */
    public static void register() {
    }
}
