package com.drunkencod.spice_road.datagen;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import com.google.gson.JsonElement;

import com.drunkencod.spice_road.Constants;
import com.drunkencod.spice_road.block.SpicePlantBlock;
import com.drunkencod.spice_road.block.SpicePlants;
import com.drunkencod.spice_road.spice.SourceType;

import net.fabricmc.fabric.api.datagen.v1.FabricDataOutput;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.data.CachedOutput;
import net.minecraft.data.DataProvider;
import net.minecraft.data.PackOutput;
import net.minecraft.data.models.blockstates.MultiVariantGenerator;
import net.minecraft.data.models.blockstates.PropertyDispatch;
import net.minecraft.data.models.blockstates.Variant;
import net.minecraft.data.models.blockstates.VariantProperties;
import net.minecraft.data.models.model.ModelTemplate;
import net.minecraft.data.models.model.ModelTemplates;
import net.minecraft.data.models.model.TextureMapping;
import net.minecraft.data.models.model.TextureSlot;
import net.minecraft.resources.ResourceLocation;

/**
 * Datagens the crop-shaped blockstate + one
 * {@code minecraft:block/crop}-parented model per growth stage (age
 * {@code 0..getMaxAge()}) for every registered Spice Plant block (see
 * {@link SpicePlants}) - {@code spice_road:block/rhizome}-parented instead for
 * {@code RHIZOME} Spices. Blockstate properties besides the age (e.g.
 * {@code waterlogged}) don't affect the model. Mirrors vanilla crop blocks
 * (e.g. wheat) exactly; see
 * {@code NeoForgeBlockStateProvider} for the NeoForge counterpart.
 * <p>
 * Implemented as a standalone {@link DataProvider} rather than via
 * {@code FabricModelProvider#generateBlockStateModels}, because vanilla's own
 * per-age-variant crop generator
 * ({@code net.minecraft.data.models.BlockModelGenerators#createCropBlock}) is
 * private, and that hook only exposes the {@code BlockModelGenerators}
 * instance itself - not the underlying blockstate/model output sinks needed
 * to reimplement it. Writing directly via {@link PackOutput.PathProvider} +
 * {@link DataProvider#saveStable} sidesteps that entirely.
 * <p>
 * The inherited {@code age} blockstate property always spans vanilla
 * {@code CropBlock}'s full 0-7 range - unlike {@code getMaxAge()}, it isn't
 * overridden per Spice Plant - so every state in that range needs a model even
 * though gameplay never grows a Spice Plant past its own (lower) {@code
 * getMaxAge()}. States above {@code getMaxAge()} reuse the final stage's model.
 */
public class FabricSpiceCropModelProvider implements DataProvider {

    /** Stage model template of {@code RHIZOME} Spices, in place of vanilla's crop template. */
    private static final ModelTemplate RHIZOME = new ModelTemplate(
            Optional.of(ResourceLocation.fromNamespaceAndPath(Constants.MOD_ID, "block/rhizome")),
            Optional.empty(), TextureSlot.CROP);

    private final PackOutput.PathProvider blockStatePathProvider;
    private final PackOutput.PathProvider modelPathProvider;

    public FabricSpiceCropModelProvider(FabricDataOutput output) {
        this.blockStatePathProvider = output.createPathProvider(PackOutput.Target.RESOURCE_PACK, "blockstates");
        this.modelPathProvider = output.createPathProvider(PackOutput.Target.RESOURCE_PACK, "models");
    }

    @Override
    public CompletableFuture<?> run(CachedOutput cachedOutput) {
        List<CompletableFuture<?>> futures = new ArrayList<>();

        SpicePlants.getRegistered().values().forEach(plant -> {
            SpicePlantBlock block = plant.block().get();
            int maxAge = block.getMaxAge();

            ModelTemplate template = plant.spice().getSourceType() == SourceType.RHIZOME
                    ? RHIZOME
                    : ModelTemplates.CROP;
            Map<Integer, ResourceLocation> stageModels = new HashMap<>();
            String blockId = BuiltInRegistries.BLOCK.getKey(block).getPath();
            for (int age = 0; age <= maxAge; age++) {
                ResourceLocation stage = SpiceAssetPaths.block(plant.spice(), blockId + "_stage" + age);
                ResourceLocation model = template.create(stage, TextureMapping.crop(stage),
                        (modelLocation, jsonSupplier) -> futures.add(DataProvider.saveStable(
                                cachedOutput, jsonSupplier.get(), modelPathProvider.json(modelLocation))));
                stageModels.put(age, model);
            }

            futures.add(writeCropBlockState(cachedOutput, block, maxAge, stageModels));
            if (plant.worldgenBlock() != plant.block())
                futures.add(writeCropBlockState(cachedOutput, plant.worldgenBlock().get(), maxAge, stageModels));
        });

        return CompletableFuture.allOf(futures.toArray(CompletableFuture[]::new));
    }

    /**
     * Writes {@code block}'s blockstate, mapping every {@code age} state to
     * its stage model in {@code stageModels}, clamped to {@code maxAge}.
     * Reused as-is for a worldgen-only ({@code wild_}) block sharing another
     * block's already-generated {@code stageModels}, so no duplicate
     * textures/models are needed for it.
     */
    private CompletableFuture<?> writeCropBlockState(CachedOutput cachedOutput, SpicePlantBlock block, int maxAge,
            Map<Integer, ResourceLocation> stageModels) {
        ResourceLocation id = BuiltInRegistries.BLOCK.getKey(block);
        PropertyDispatch dispatch = PropertyDispatch.property(block.getAgeProperty())
                .generate(age -> Variant.variant().with(VariantProperties.MODEL,
                        stageModels.get(Math.min(age, maxAge))));

        JsonElement blockState = MultiVariantGenerator.multiVariant(block).with(dispatch).get();
        return DataProvider.saveStable(cachedOutput, blockState, blockStatePathProvider.json(id));
    }

    @Override
    public String getName() {
        return "Spice Crop Models";
    }
}
