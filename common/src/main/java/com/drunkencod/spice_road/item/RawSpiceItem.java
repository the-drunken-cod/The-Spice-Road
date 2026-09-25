package com.drunkencod.spice_road.item;

import com.drunkencod.spice_road.spice.Spice;
import com.drunkencod.spice_road.spice.SpiceProfile;

/**
 * The raw (freshly harvested) form of a {@link Spice}. Tooltip shows
 * {@link Spice#getRawProfile()}, which every Spice always has.
 */
public class RawSpiceItem extends SpiceItem {

    public RawSpiceItem(Properties properties, Spice spice) {
        super(properties, spice);
    }

    @Override
    protected SpiceProfile getProfile() {
        return getSpice().getRawProfile();
    }
}
