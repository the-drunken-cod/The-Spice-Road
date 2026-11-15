package com.drunkencod.spice_road.soil;

import org.jetbrains.annotations.Nullable;

import net.minecraft.network.chat.Component;

import com.drunkencod.spice_road.platform.Services;

/** Whether a Soil Sample may be taken at a block, and if not, why. */
public enum SoilSampleCheck {

    /** The sample may be taken. */
    OK(null),
    /** The Soil Analyzer is switched off in the config. */
    DISABLED("disabled"),
    /** The block isn't dirt. */
    NOT_DIRT("not_dirt"),
    /** Removing the block would knock off what sits on top of it. */
    BLOCKED_ABOVE("blocked_above"),
    /** The earlier samples were taken in another dimension. */
    OTHER_DIMENSION("other_dimension"),
    /** The block is too near an earlier sample. */
    TOO_CLOSE("too_close"),
    /** The block is too far from the first sample. */
    TOO_FAR("too_far"),
    /** The player isn't allowed to break the block. */
    PROTECTED("protected");

    private static final String KEY_PREFIX = "spice_road.message.soil_analyzer.";

    @Nullable
    private final String key;

    SoilSampleCheck(@Nullable String key) {
        this.key = key;
    }

    /**
     * @param spacing The result of the spacing rules.
     * @return The matching check.
     */
    public static SoilSampleCheck of(SoilSamples.Spacing spacing) {
        return switch (spacing) {
            case OK -> OK;
            case OTHER_DIMENSION -> OTHER_DIMENSION;
            case TOO_CLOSE -> TOO_CLOSE;
            case TOO_FAR -> TOO_FAR;
        };
    }

    /**
     * @return The message telling the player why the sample was refused, or
     *         an empty component for {@link #OK}.
     */
    public Component message() {
        if (key == null)
            return Component.empty();
        return switch (this) {
            case TOO_CLOSE -> Component.translatable(KEY_PREFIX + key,
                    Services.CONFIG.getSoilAnalyzerMinSampleDistance());
            case TOO_FAR -> Component.translatable(KEY_PREFIX + key, Services.CONFIG.getSoilAnalyzerMaxSampleSpan());
            default -> Component.translatable(KEY_PREFIX + key);
        };
    }
}
