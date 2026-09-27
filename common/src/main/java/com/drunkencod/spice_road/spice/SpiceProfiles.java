package com.drunkencod.spice_road.spice;

import java.util.Optional;

import net.minecraft.world.item.ItemStack;

import com.drunkencod.spice_road.platform.Services;
import com.drunkencod.spice_road.registry.ModDataComponents;

/**
 * Single choke point for reading an {@link ItemStack}'s {@link SpiceProfile},
 * regardless of whether it comes from a per-stack
 * {@code spice_road:spice_profile} data component override or from the
 * datapack-registered default for its item ({@link SpiceProfileRegistry}).
 * <p>
 * Every place that needs a stack's flavor axis values (tooltips, future
 * economy/quality mechanics) should go through this rather than reading the
 * component or the registry directly, so a datapack-registered spice behaves
 * identically everywhere.
 */
public final class SpiceProfiles {

    private SpiceProfiles() {
    }

    /**
     * @param stack The stack to look up.
     * @return {@code stack}'s {@link SpiceProfile}: its own data component
     *         value if present, otherwise the datapack-registered default
     *         for {@code stack.getItem()}. Empty if {@code stack} isn't
     *         registered as a spice at all.
     */
    public static Optional<SpiceProfile> get(ItemStack stack) {
        SpiceProfile override = stack.get(ModDataComponents.SPICE_PROFILE.get());
        if (override != null)
            return Optional.of(override);
        return SpiceProfileRegistry.getDefault(stack.getItem());
    }

    /**
     * @param stack The stack to look up.
     * @return The Effective Profile of {@code stack}'s {@link #get stored
     *         profile}, which is what effects and tooltips should use.
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
        double cap = Services.CONFIG.getFlavorSoftCap();
        double minimum = Services.CONFIG.getFlavorMinimumAxisValue();
        return profile.map(value -> {
            if (value == 0D)
                return 0D;
            double capped = cap * Math.tanh(value / cap);
            return Math.copySign(Math.max(Math.abs(capped), minimum), value);
        });
    }
}
