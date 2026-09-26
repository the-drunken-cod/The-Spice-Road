package com.drunkencod.spice_road.debug;

import java.util.List;

/**
 * A flexible, labeled set of debug values describing what the Spice Region
 * (cell) system resolved at a given position - assigned Spice, Climate Bucket,
 * cell id, raw noise value, etc.
 *
 * @param lines The ordered debug lines to display, top to bottom.
 */
public record SpiceRegionDebugInfo(List<Line> lines) {

    /**
     * A single labeled debug value (e.g. {@code "Climate Bucket" -> "TEMPERATE"}).
     *
     * @param label Short human-readable name of the value.
     * @param value The value itself, already formatted as display text.
     */
    public record Line(String label, String value) {

        @Override
        public String toString() {
            return label + ": " + value;
        }
    }
}
