package com.drunkencod.spice_road.datagen;

import java.util.concurrent.CompletableFuture;

import com.google.gson.JsonElement;
import com.mojang.serialization.JsonOps;

import net.minecraft.data.CachedOutput;
import net.minecraft.data.DataProvider;
import net.minecraft.data.PackOutput;

import com.drunkencod.spice_road.spice.board.BoardLayout;
import com.drunkencod.spice_road.spice.board.BoardLayoutReloadListener;

/**
 * Writes {@link BoardLayout#DEFAULT} as
 * {@code data/spice_road/seasoning_board/default.json}, as raw JSON so it runs
 * unchanged on both loaders. The layout is encoded and parsed back through its
 * codec, so datagen fails if the default doesn't validate.
 */
public class BoardLayoutProvider implements DataProvider {

    private final PackOutput.PathProvider pathProvider;

    /** @param output The pack output to write into. */
    public BoardLayoutProvider(PackOutput output) {
        this.pathProvider = output.createPathProvider(PackOutput.Target.DATA_PACK,
                BoardLayoutReloadListener.DIRECTORY);
    }

    @Override
    public CompletableFuture<?> run(CachedOutput cachedOutput) {
        JsonElement json = BoardLayout.CODEC.encodeStart(JsonOps.INSTANCE, BoardLayout.DEFAULT).getOrThrow();
        BoardLayout.CODEC.parse(JsonOps.INSTANCE, json).getOrThrow();
        return DataProvider.saveStable(cachedOutput, json, pathProvider.json(BoardLayoutReloadListener.LAYOUT_ID));
    }

    @Override
    public String getName() {
        return "Seasoning Board Layout";
    }
}
