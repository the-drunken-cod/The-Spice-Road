package com.drunkencod.spice_road.compat.viewer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;

import net.minecraft.network.chat.Component;

class ViewerLayoutTest {

    @Test
    void heightCountsThePanelBorder() {
        ViewerLayout layout = ViewerLayout.builder().panel(4, 24, 100, 89).build();
        assertEquals(24 + 89 + ViewerDrawing.PANEL_BORDER, layout.height());
    }

    @Test
    void heightIsTheLowestPart() {
        ViewerLayout layout = ViewerLayout.builder()
                .text(Component.literal("a"), 0, 40, 0, false)
                .tooltip(0, 0, 10, 60, List.of(Component.literal("b")))
                .build();
        assertEquals(60, layout.height());
    }

    @Test
    void emptyTooltipsAreDropped() {
        ViewerLayout layout = ViewerLayout.builder().tooltip(0, 0, 10, 10, List.of()).build();
        assertTrue(layout.tooltips().isEmpty());
    }

    @Test
    void theTopmostTooltipWins() {
        Component below = Component.literal("below");
        Component above = Component.literal("above");
        ViewerLayout layout = ViewerLayout.builder()
                .tooltip(0, 0, 20, 20, List.of(below))
                .tooltip(5, 5, 5, 5, List.of(above))
                .build();
        assertEquals(List.of(above), ViewerDrawing.tooltipAt(layout, 6, 6));
        assertEquals(List.of(below), ViewerDrawing.tooltipAt(layout, 1, 1));
        assertTrue(ViewerDrawing.tooltipAt(layout, 20, 20).isEmpty());
    }

    @Test
    void aShortenedLineShowsItsFullText() {
        // Without a client font, widths are measured in characters
        Component line = Component.literal("Spring, Summer, Autumn");
        Component explanation = Component.literal("explanation");
        ViewerLayout layout = ViewerLayout.builder()
                .text(line, 0, 0, 0, false, 10, 1F)
                .tooltip(0, 0, 10, 9, List.of(explanation))
                .build();
        assertEquals(List.of(line, explanation), ViewerDrawing.tooltipAt(layout, 1, 1));
    }

    @Test
    void aFittingLineAddsNothing() {
        ViewerLayout layout = ViewerLayout.builder().text(Component.literal("short"), 0, 0, 0, false, 10, 1F).build();
        assertTrue(ViewerDrawing.tooltipAt(layout, 1, 1).isEmpty());
    }

    @Test
    void scaledLinesShrinkTheirHeight() {
        ViewerLayout layout = ViewerLayout.builder().text(Component.literal("a"), 0, 0, 0, false, 0, 0.5F).build();
        assertEquals(5, layout.height());
    }

    @Test
    void durationsSplitIntoUnits() {
        assertEquals(List.of(part("s", "45")), ViewerText.durationParts(45));
        assertEquals(List.of(part("m", "2")), ViewerText.durationParts(120));
        assertEquals(List.of(part("m", "1"), part("s", "15")), ViewerText.durationParts(75));
        assertEquals(List.of(part("h", "1"), part("s", "5")), ViewerText.durationParts(3605));
        assertEquals(List.of(part("s", "7.5")), ViewerText.durationParts(7.5));
        assertEquals(List.of(part("m", "1")), ViewerText.durationParts(59.6));
    }

    private static ViewerText.DurationPart part(String unit, String amount) {
        return new ViewerText.DurationPart(unit, amount);
    }

    @Test
    void percentagesDropWholeFractions() {
        assertEquals("50%", ViewerText.percent(0.5));
        assertEquals("12.5%", ViewerText.percent(0.125));
        assertEquals("100%", ViewerText.percent(1));
    }

    @Test
    void amountsDropTrailingZeros() {
        assertEquals("2", ViewerText.amount(2.0));
        assertEquals("1.5", ViewerText.amount(1.5));
        assertEquals("0.33", ViewerText.amount(1 / 3D));
    }
}
