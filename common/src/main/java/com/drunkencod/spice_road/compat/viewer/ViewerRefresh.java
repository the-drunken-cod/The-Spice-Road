package com.drunkencod.spice_road.compat.viewer;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;

import com.drunkencod.spice_road.Constants;
import com.drunkencod.spice_road.mix.MixPreset;
import com.drunkencod.spice_road.mix.MixPresetRegistry;
import com.drunkencod.spice_road.spice.SpiceProfile;
import com.drunkencod.spice_road.spice.SpiceProfileRegistry;

/**
 * Keeps Recipe Viewers in step with the synced data their entries are built
 * from. Spice Profiles and Mix Presets reach the client through the mod's own
 * payloads, which may arrive after a viewer already built its entries, both on
 * joining and after {@code /reload}. Each viewer adapter therefore owns a
 * {@link Tracker}, and the sync handlers call {@link #onDataChanged()} after
 * applying a payload.
 */
public final class ViewerRefresh {

    /** Guards every tracker's {@link Tracker#used} against concurrent builds. */
    private static final Object LOCK = new Object();

    private static final List<Tracker> TRACKERS = new CopyOnWriteArrayList<>();

    private ViewerRefresh() {
    }

    /**
     * The synced data viewer entries are built from.
     *
     * @param profiles Every Spice Item's Default Profile.
     * @param presets  Every Mix Preset, keyed by ID.
     */
    public record Snapshot(Map<Item, SpiceProfile> profiles, Map<ResourceLocation, MixPreset> presets) {

        /** @return The data as currently synced. */
        public static Snapshot current() {
            return new Snapshot(SpiceProfileRegistry.getAll(), MixPresetRegistry.getAll());
        }
    }

    /**
     * Creates and registers the tracker of one viewer adapter.
     *
     * @param viewer  The viewer's name, for logging.
     * @param refresh Rebuilds the viewer's synced-data entries; called on the
     *                client thread. It must build them from
     *                {@link Tracker#begin()} again.
     * @return The new tracker.
     */
    public static Tracker register(String viewer, Runnable refresh) {
        Tracker tracker = new Tracker(viewer, refresh);
        TRACKERS.add(tracker);
        return tracker;
    }

    /**
     * Refreshes every viewer that built its entries from data other than the
     * current one. Called on the client thread after a sync payload is applied.
     */
    public static void onDataChanged() {
        for (Tracker tracker : TRACKERS)
            tracker.refreshIfStale();
    }

    /** Remembers which data one viewer built its entries from. */
    public static final class Tracker {

        private final String viewer;
        private final Runnable refresh;
        /** The data last built from, or {@code null} before the first build. */
        private Snapshot used;

        private Tracker(String viewer, Runnable refresh) {
            this.viewer = viewer;
            this.refresh = refresh;
        }

        /**
         * Marks the start of a build and returns the data to build from. Since
         * reading and remembering happen under one lock, a payload applied
         * meanwhile is either already part of the returned data or caught by
         * the following {@link ViewerRefresh#onDataChanged()}.
         *
         * @return The data to build the entries from.
         */
        public Snapshot begin() {
            synchronized (LOCK) {
                used = Snapshot.current();
                return used;
            }
        }

        /** Forgets the last build, e.g. when the viewer unloads. */
        public void reset() {
            synchronized (LOCK) {
                used = null;
            }
        }

        private void refreshIfStale() {
            synchronized (LOCK) {
                // A viewer that hasn't built yet will read the current data anyway
                if (used == null || used.equals(Snapshot.current()))
                    return;
            }
            try {
                refresh.run();
            } catch (RuntimeException | LinkageError e) {
                // A viewer API that changed under us must never break the sync itself
                Constants.LOG.error("Could not refresh the {} entries of {}", viewer, Constants.MOD_NAME, e);
            }
        }
    }
}
