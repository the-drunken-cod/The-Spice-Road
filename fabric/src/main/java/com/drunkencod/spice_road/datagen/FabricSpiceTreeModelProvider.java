package com.drunkencod.spice_road.datagen;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.function.BiConsumer;
import java.util.function.Supplier;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import net.fabricmc.fabric.api.datagen.v1.FabricDataOutput;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.data.CachedOutput;
import net.minecraft.data.DataProvider;
import net.minecraft.data.PackOutput;
import net.minecraft.data.models.blockstates.BlockStateGenerator;
import net.minecraft.data.models.blockstates.MultiVariantGenerator;
import net.minecraft.data.models.blockstates.PropertyDispatch;
import net.minecraft.data.models.blockstates.Variant;
import net.minecraft.data.models.blockstates.VariantProperties;
import net.minecraft.data.models.model.ModelLocationUtils;
import net.minecraft.data.models.model.ModelTemplates;
import net.minecraft.data.models.model.TextureMapping;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;

import com.drunkencod.spice_road.Constants;
import com.drunkencod.spice_road.block.FruitingSpiceLeavesBlock;
import com.drunkencod.spice_road.block.SpiceTrees;
import com.drunkencod.spice_road.spice.Spice;

/**
 * Datagens blockstates, block models, and log/leaves item models for every
 * registered Spice Tree (see {@link SpiceTrees}), matching
 * {@code NeoForgeBlockStateProvider}'s output. Implemented as a standalone
 * {@link DataProvider} for the same reason as
 * {@link FabricSpiceCropModelProvider}. The sapling's flat item model is
 * generated through {@code ItemModelHelper}.
 */
public class FabricSpiceTreeModelProvider implements DataProvider {

    private final PackOutput.PathProvider blockStatePathProvider;
    private final PackOutput.PathProvider modelPathProvider;

    public FabricSpiceTreeModelProvider(FabricDataOutput output) {
        this.blockStatePathProvider = output.createPathProvider(PackOutput.Target.RESOURCE_PACK, "blockstates");
        this.modelPathProvider = output.createPathProvider(PackOutput.Target.RESOURCE_PACK, "models");
    }

    @Override
    public CompletableFuture<?> run(CachedOutput cachedOutput) {
        List<CompletableFuture<?>> futures = new ArrayList<>();
        BiConsumer<ResourceLocation, Supplier<JsonElement>> modelOutput = (modelLocation, json) -> futures
                .add(DataProvider.saveStable(cachedOutput, json.get(), modelPathProvider.json(modelLocation)));

        SpiceTrees.getRegistered().values().forEach(tree -> {
            Spice spice = tree.getSpice();
            for (Block log : List.of(tree.getLog().get(), tree.getStrippedLog().get())) {
                saveBlockState(cachedOutput, futures, log, createLog(spice, log, modelOutput));
                saveBlockItemModel(spice, log, modelOutput);
            }

            LeavesBlock leaves = tree.getLeaves().get();
            saveBlockState(cachedOutput, futures, leaves, createLeaves(spice, leaves, modelOutput));
            saveBlockItemModel(spice, leaves, modelOutput);

            Block sapling = tree.getSapling().get();
            ResourceLocation saplingPath = SpiceAssetPaths.block(spice, tree.getSaplingId());
            ResourceLocation saplingModel = ModelTemplates.CROSS.create(saplingPath, TextureMapping.cross(saplingPath),
                    modelOutput);
            saveBlockState(cachedOutput, futures, sapling, MultiVariantGenerator.multiVariant(sapling,
                    Variant.variant().with(VariantProperties.MODEL, saplingModel)));
        });

        return CompletableFuture.allOf(futures.toArray(CompletableFuture[]::new));
    }

    /** Mirrors vanilla's axis-rotated log blockstate with a horizontal model variant. */
    private static BlockStateGenerator createLog(Spice spice, Block log,
            BiConsumer<ResourceLocation, Supplier<JsonElement>> modelOutput) {
        String id = idOf(log);
        ResourceLocation side = SpiceAssetPaths.block(spice, id);
        TextureMapping textures = TextureMapping.column(side, SpiceAssetPaths.block(spice, id + "_top"));
        ResourceLocation vertical = ModelTemplates.CUBE_COLUMN.create(side, textures, modelOutput);
        ResourceLocation horizontal = ModelTemplates.CUBE_COLUMN_HORIZONTAL
                .create(SpiceAssetPaths.block(spice, id + "_horizontal"), textures, modelOutput);

        return MultiVariantGenerator.multiVariant(log).with(PropertyDispatch.property(BlockStateProperties.AXIS)
                .select(Direction.Axis.Y, Variant.variant().with(VariantProperties.MODEL, vertical))
                .select(Direction.Axis.Z, Variant.variant().with(VariantProperties.MODEL, horizontal)
                        .with(VariantProperties.X_ROT, VariantProperties.Rotation.R90))
                .select(Direction.Axis.X, Variant.variant().with(VariantProperties.MODEL, horizontal)
                        .with(VariantProperties.X_ROT, VariantProperties.Rotation.R90)
                        .with(VariantProperties.Y_ROT, VariantProperties.Rotation.R90)));
    }

    /**
     * Plain leaves get a single {@code minecraft:block/leaves} model; fruiting
     * leaves get one per stage, using {@code block/<spice>/<id>_stage<n>} for
     * stages above 0.
     */
    private static BlockStateGenerator createLeaves(Spice spice, LeavesBlock leaves,
            BiConsumer<ResourceLocation, Supplier<JsonElement>> modelOutput) {
        String id = idOf(leaves);
        if (!(leaves instanceof FruitingSpiceLeavesBlock)) {
            ResourceLocation path = SpiceAssetPaths.block(spice, id);
            ResourceLocation model = ModelTemplates.LEAVES.create(path, TextureMapping.cube(path), modelOutput);
            return MultiVariantGenerator.multiVariant(leaves, Variant.variant().with(VariantProperties.MODEL, model));
        }

        ResourceLocation[] stageModels = new ResourceLocation[Constants.SPICE_TREE_LEAF_GROWTH_STAGES + 1];
        for (int age = 0; age < stageModels.length; age++) {
            ResourceLocation path = SpiceAssetPaths.block(spice, age == 0 ? id : id + "_stage" + age);
            stageModels[age] = ModelTemplates.LEAVES.create(path, TextureMapping.cube(path), modelOutput);
        }
        return MultiVariantGenerator.multiVariant(leaves).with(PropertyDispatch.property(FruitingSpiceLeavesBlock.AGE)
                .generate(age -> Variant.variant().with(VariantProperties.MODEL, stageModels[age])));
    }

    /** Writes {@code block}'s item model, which stays flat under {@code item/} and inherits its block model. */
    private static void saveBlockItemModel(Spice spice, Block block,
            BiConsumer<ResourceLocation, Supplier<JsonElement>> modelOutput) {
        JsonObject itemModel = new JsonObject();
        itemModel.addProperty("parent", SpiceAssetPaths.block(spice, idOf(block)).toString());
        modelOutput.accept(ModelLocationUtils.getModelLocation(block.asItem()), () -> itemModel);
    }

    /** @return {@code block}'s registry path, e.g. {@code cinnamon_log}. */
    private static String idOf(Block block) {
        return BuiltInRegistries.BLOCK.getKey(block).getPath();
    }

    private void saveBlockState(CachedOutput cachedOutput, List<CompletableFuture<?>> futures, Block block,
            BlockStateGenerator generator) {
        futures.add(DataProvider.saveStable(cachedOutput, generator.get(),
                blockStatePathProvider.json(BuiltInRegistries.BLOCK.getKey(block))));
    }

    @Override
    public String getName() {
        return "Spice Tree Models";
    }
}
