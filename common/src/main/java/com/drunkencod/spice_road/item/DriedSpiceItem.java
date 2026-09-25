package com.drunkencod.spice_road.item;

import com.drunkencod.spice_road.spice.Spice;
import com.drunkencod.spice_road.spice.SpiceProfile;

/**
 * The dried form of a {@link Spice} (produced by the Drying Rack). Tooltip
 * shows {@link Spice#getDriedProfile()}.
 * <p>
 * Not every {@link Spice} has a dried profile yet - only construct
 * this for a Spice whose {@link Spice#getDriedProfile()} is present.
 */
public class DriedSpiceItem extends SpiceItem {

    public DriedSpiceItem(Properties properties, Spice spice) {
        super(properties, spice);
        if (spice.getDriedProfile().isEmpty()) {
            throw new IllegalArgumentException(spice + " has no dried Spice Profile yet");
        }
    }

    @Override
    protected SpiceProfile getProfile() {
        return getSpice().getDriedProfile()
                .orElseThrow(() -> new IllegalStateException(getSpice() + " has no dried Spice Profile"));
    }
}
