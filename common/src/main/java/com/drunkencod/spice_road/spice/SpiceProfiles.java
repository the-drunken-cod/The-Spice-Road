package com.drunkencod.spice_road.spice;

import java.util.Optional;

import net.minecraft.world.item.ItemStack;

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
}
