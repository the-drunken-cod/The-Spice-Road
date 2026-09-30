package com.drunkencod.spice_road.datagen;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import com.google.gson.JsonObject;

import net.fabricmc.fabric.api.datagen.v1.FabricDataOutput;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.data.CachedOutput;
import net.minecraft.data.DataProvider;
import net.minecraft.data.PackOutput;
import net.minecraft.data.models.blockstates.Condition;
import net.minecraft.data.models.blockstates.MultiPartGenerator;
import net.minecraft.data.models.blockstates.Variant;
import net.minecraft.data.models.blockstates.VariantProperties;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.VineBlock;

import com.drunkencod.spice_road.Constants;
import com.drunkencod.spice_road.block.SpiceVineBlock;
import com.drunkencod.spice_road.block.SpiceVines;

/**
 * Datagens the blockstate and per-ripening-stage block models of every
 * registered Spice Vine (see {@link SpiceVines}), matching
 * {@code NeoForgeBlockStateProvider}'s output. Implemented as a standalone
 * {@link DataProvider} for the same reason as
 * {@link FabricSpiceCropModelProvider}. The vine item's flat model is
 * generated through {@code ItemModelHelper}.
 */
public class FabricSpiceVineModelProvider implements DataProvider {

    private static final Map<Direction, VariantProperties.Rotation> Y_ROTATION = Map.of(
            Direction.NORTH, VariantProperties.Rotation.R0,
            Direction.EAST, VariantProperties.Rotation.R90,
            Direction.SOUTH, VariantProperties.Rotation.R180,
            Direction.WEST, VariantProperties.Rotation.R270);

    private final PackOutput.PathProvider blockStatePathProvider;
    private final PackOutput.PathProvider modelPathProvider;

    public FabricSpiceVineModelProvider(FabricDataOutput output) {
        this.blockStatePathProvider = output.createPathProvider(PackOutput.Target.RESOURCE_PACK, "blockstates");
        this.modelPathProvider = output.createPathProvider(PackOutput.Target.RESOURCE_PACK, "models");
    }

    @Override
    public CompletableFuture<?> run(CachedOutput cachedOutput) {
        List<CompletableFuture<?>> futures = new ArrayList<>();

        SpiceVines.getRegistered().values().forEach(registered -> {
            SpiceVineBlock vine = registered.block().get();
            MultiPartGenerator blockState = MultiPartGenerator.multiPart(vine);

            String vineId = BuiltInRegistries.BLOCK.getKey(vine).getPath();
            for (int age = 0; age <= Constants.SPICE_VINE_GROWTH_STAGES; age++) {
                ResourceLocation model = SpiceAssetPaths.block(registered.spice(), vineId + "_stage" + age);
                futures.add(DataProvider.saveStable(cachedOutput, createStageModel(model),
                        modelPathProvider.json(model)));

                for (Direction direction : SpiceVineBlock.FACES) {
                    Variant variant = Variant.variant().with(VariantProperties.MODEL, model);
                    if (direction == Direction.UP)
                        variant.with(VariantProperties.X_ROT, VariantProperties.Rotation.R270);
                    else
                        variant.with(VariantProperties.Y_ROT, Y_ROTATION.get(direction));
                    if (direction != Direction.NORTH)
                        variant.with(VariantProperties.UV_LOCK, true);

                    blockState.with(Condition.condition()
                            .term(VineBlock.getPropertyForFace(direction), true)
                            .term(SpiceVineBlock.AGE, age), variant);
                }
            }

            futures.add(DataProvider.saveStable(cachedOutput, blockState.get(),
                    blockStatePathProvider.json(BuiltInRegistries.BLOCK.getKey(vine))));
        });

        return CompletableFuture.allOf(futures.toArray(CompletableFuture[]::new));
    }

    /** @return A {@code minecraft:block/vine}-parented model using {@code texture}. */
    private static JsonObject createStageModel(ResourceLocation texture) {
        JsonObject textures = new JsonObject();
        textures.addProperty("vine", texture.toString());
        textures.addProperty("particle", texture.toString());

        JsonObject model = new JsonObject();
        model.addProperty("parent", "minecraft:block/vine");
        model.add("textures", textures);
        return model;
    }

    @Override
    public String getName() {
        return "Spice Vine Models";
    }
}
