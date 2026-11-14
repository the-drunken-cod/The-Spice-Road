package com.drunkencod.spice_road.spice;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.EnumSet;
import java.util.Set;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;

class SpiceHandPickTest {

    @Test
    void everyEpicSpiceRequiresHandPickingAndNoOtherDoes() {
        Set<Spice> handPicked = EnumSet.noneOf(Spice.class);
        for (Spice spice : Spice.values()) {
            if (spice.requiresHandPick())
                handPicked.add(spice);
        }

        Set<Spice> epic = EnumSet.of(Spice.CINNAMON, Spice.CLOVE, Spice.NUTMEG, Spice.VANILLA, Spice.SAFFRON,
                Spice.WASABI);
        assertEquals(epic, handPicked,
                () -> "Hand-pick Spices: " + handPicked.stream().map(Spice::getId).collect(Collectors.toList()));
    }
}
