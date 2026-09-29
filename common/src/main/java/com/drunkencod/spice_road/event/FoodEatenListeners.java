package com.drunkencod.spice_road.event;

import java.util.ArrayList;
import java.util.List;

import com.drunkencod.spice_road.Constants;

/**
 * Registry of {@link FoodEatenListener}s, fired by a common mixin on
 * {@code LivingEntity#eat} for every eaten food item, on both logical sides.
 * <p>
 * Consumers (advancement triggers, the eventual Spice Buffs and stats
 * screen) each {@link #register} their own listener at mod init and decide
 * for themselves whether an event is relevant - e.g. filtering to
 * {@code ServerPlayer}, server-side only, or a non-zero
 * {@link com.drunkencod.spice_road.spice.SpiceProfile} - rather than this
 * registry pre-filtering for them.
 */
public final class FoodEatenListeners {

    private static final List<FoodEatenListener> LISTENERS = new ArrayList<>();

    private FoodEatenListeners() {
    }

    /**
     * @param listener The listener to add. Called for every future
     *                 {@link FoodEatenEvent}, in registration order.
     */
    public static void register(FoodEatenListener listener) {
        LISTENERS.add(listener);
    }

    /**
     * Notifies every registered listener. A listener that throws is logged
     * and skipped, so one broken consumer can't break the others or the
     * vanilla eat call itself.
     *
     * @param event The event to dispatch.
     */
    public static void fire(FoodEatenEvent event) {
        for (FoodEatenListener listener : LISTENERS) {
            try {
                listener.onFoodEaten(event);
            } catch (Exception e) {
                Constants.LOG.error("FoodEatenListener {} threw while handling {}", listener.getClass().getName(),
                        event, e);
            }
        }
    }
}
