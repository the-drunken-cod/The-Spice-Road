package com.drunkencod.spice_road.datagen;

import com.drunkencod.spice_road.Constants;
import com.drunkencod.spice_road.block.SpicePlantBlock;
import com.drunkencod.spice_road.block.SpicePlants;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.data.PackOutput;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.client.model.generators.BlockStateProvider;
import net.neoforged.neoforge.client.model.generators.ConfiguredModel;
import net.neoforged.neoforge.client.model.generators.ModelFile;
import net.neoforged.neoforge.common.data.ExistingFileHelper;

/**
 * Datagens the crop-shaped blockstate + one
 * {@code minecraft:block/crop}-parented model per growth stage (age
 * {@code 0..getMaxAge()}) for every registered Spice Plant block (see
 * {@link SpicePlants}). Mirrors vanilla crop blocks (e.g. wheat) exactly; see
 * {@code FabricSpiceCropModelProvider} for the Fabric counterpart
 * -
 * vanilla's own equivalent generator
 * ({@code BlockModelGenerators#createCropBlock})
 * is private, so both loaders hand-roll it via their own datagen idiom instead
 * of
 * sharing one implementation.
 * <p>
 * The inherited {@code age} blockstate property always spans vanilla
 * {@code CropBlock}'s full 0-7 range - unlike {@code getMaxAge()}, it isn't
 * overridden per Spice Plant - so every state in that range needs a model even
 * though gameplay never grows a Spice Plant past its own (lower) {@code
 * getMaxAge()}. States above {@code getMaxAge()} reuse the final stage's model.
 */
public class NeoForgeBlockStateProvider extends BlockStateProvider {

    public NeoForgeBlockStateProvider(PackOutput output, ExistingFileHelper existingFileHelper) {
        super(output, Constants.MOD_ID, existingFileHelper);
    }

    @Override
    protected void registerStatesAndModels() {
        SpicePlants.getRegistered().values().forEach(plant -> {
            SpicePlantBlock block = plant.block().get();
            ResourceLocation id = BuiltInRegistries.BLOCK.getKey(block);
            int maxAge = block.getMaxAge();

            ModelFile[] stageModels = new ModelFile[maxAge + 1];
            for (int age = 0; age <= maxAge; age++) {
                String stageId = id.getPath() + "_stage" + age;
                stageModels[age] = models().withExistingParent(stageId, "block/crop")
                        .texture("crop", modLoc("block/" + stageId));
            }

            getVariantBuilder(block).forAllStates(state -> ConfiguredModel.builder()
                    .modelFile(stageModels[Math.min(state.getValue(block.getAgeProperty()), maxAge)])
                    .build());
        });
    }
}
