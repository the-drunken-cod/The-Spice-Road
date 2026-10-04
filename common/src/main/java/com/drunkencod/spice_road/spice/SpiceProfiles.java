package com.drunkencod.spice_road.spice;

import java.util.Optional;

import net.minecraft.world.item.ItemStack;

import com.drunkencod.spice_road.platform.Services;

/**
 * Single choke point for reading an {@link ItemStack}'s {@link SpiceProfile}.
 * Only Spice Items have one: the datapack-registered default for their item
 * ({@link SpiceProfileRegistry}). Seasoned food carries counted contributors
 * instead (see {@link Seasoning}).
 * <p>
 * Every place that needs a stack's flavor axis values (tooltips, the Spice
 * Grinder) should go through this rather than reading the registry directly,
 * so a datapack-registered spice behaves identically everywhere.
 */
public final class SpiceProfiles {

    private SpiceProfiles() {
    }

    /**
     * @param stack The stack to look up.
     * @return The datapack-registered {@link SpiceProfile} of
     *         {@code stack.getItem()}. Empty if {@code stack} isn't a Spice Item.
     */
    public static Optional<SpiceProfile> get(ItemStack stack) {
        return SpiceProfileRegistry.getDefault(stack.getItem());
    }

    /**
     * @param stack The stack to look up.
     * @return The Effective Profile of {@code stack}'s {@link #get profile},
     *         which is what effects and tooltips should use.
     */
    public static Optional<SpiceProfile> getEffective(ItemStack stack) {
        return get(stack).map(SpiceProfiles::effective);
    }

    /**
     * Derives an Effective Profile: each axis saturates towards the
     * configured soft cap ({@code cap * tanh(value / cap)}), giving
     * diminishing returns, and any non-zero axis counts as at least the
     * configured minimum magnitude. Never stored, so it can't feed back into
     * crafting.
     *
     * @param profile A stored profile.
     * @return The corresponding Effective Profile.
     */
    public static SpiceProfile effective(SpiceProfile profile) {
        return profile.map(value -> Math.copySign(effectiveValue(Math.abs(value)), value));
    }

    /**
     * The saturation of {@link #effective(SpiceProfile)} for one magnitude.
     *
     * @param magnitude A raw, non-negative value of one axis.
     * @return Its effective value: {@code 0} for {@code 0}, otherwise
     *         saturated towards the soft cap but at least the minimum.
     */
    public static double effectiveValue(double magnitude) {
        if (magnitude <= 0D)
            return 0D;
        double cap = Services.CONFIG.getFlavorSoftCap();
        double minimum = Services.CONFIG.getFlavorMinimumAxisValue();
        return Math.max(cap * Math.tanh(magnitude / cap), minimum);
    }
}
