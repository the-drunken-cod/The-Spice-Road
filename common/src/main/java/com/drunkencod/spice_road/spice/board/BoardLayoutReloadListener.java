package com.drunkencod.spice_road.spice.board;

import java.util.Map;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.mojang.serialization.JsonOps;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;

import com.drunkencod.spice_road.Constants;

/**
 * Loads the board layout from {@code data/spice_road/seasoning_board/default.json}.
 * A datapack replaces it by overriding that file. A malformed file is logged and
 * the built-in layout is used instead. Registered on both loaders via
 * {@code IRegistryHelper#registerReloadListener}.
 */
public class BoardLayoutReloadListener extends SimpleJsonResourceReloadListener {

    /** Datapack directory the layout is loaded from. */
    public static final String DIRECTORY = "seasoning_board";
    /** ID of this reload listener. */
    public static final ResourceLocation ID = ResourceLocation.fromNamespaceAndPath(Constants.MOD_ID, DIRECTORY);
    /** ID of the layout file. */
    public static final ResourceLocation LAYOUT_ID = ResourceLocation.fromNamespaceAndPath(Constants.MOD_ID,
            "default");

    /** Creates the listener for the {@link #DIRECTORY} directory. */
    public BoardLayoutReloadListener() {
        super(new Gson(), DIRECTORY);
    }

    @Override
    protected void apply(Map<ResourceLocation, JsonElement> object, ResourceManager resourceManager,
            ProfilerFiller profiler) {
        BoardLayout layout = BoardLayout.DEFAULT;
        JsonElement json = object.get(LAYOUT_ID);
        if (json != null) {
            layout = BoardLayout.CODEC.parse(JsonOps.INSTANCE, json)
                    .resultOrPartial(error -> Constants.LOG.error("Couldn't load Seasoning Board layout {}: {}",
                            LAYOUT_ID, error))
                    .orElse(BoardLayout.DEFAULT);
        }
        BoardLayoutRegistry.set(layout);
        AutomaticSeasoning.clearBoards();
    }
}
