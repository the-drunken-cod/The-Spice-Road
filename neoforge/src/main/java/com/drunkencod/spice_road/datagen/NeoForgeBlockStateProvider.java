package com.drunkencod.spice_road.datagen;

import com.drunkencod.spice_road.Constants;
import com.drunkencod.spice_road.block.SpicePlantBlock;
import com.drunkencod.spice_road.block.FruitingSpiceLeavesBlock;
import com.drunkencod.spice_road.block.SpicePlants;
import com.drunkencod.spice_road.block.SpiceTree;
import com.drunkencod.spice_road.block.SpiceTrees;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.data.PackOutput;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.LeavesBlock;
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
                        .texture("crop", modLoc("block/" + stageId))
                        .renderType("minecraft:cutout");
            }

            getVariantBuilder(block).forAllStates(state -> ConfiguredModel.builder()
                    .modelFile(stageModels[Math.min(state.getValue(block.getAgeProperty()), maxAge)])
                    .build());
        });

        SpiceTrees.getRegistered().values().forEach(this::registerSpiceTree);
    }

    /**
     * Log/stripped log: vanilla-style axis-rotated columns
     * ({@code block/<id>} sides, {@code block/<id>_top} ends). Leaves:
     * {@code minecraft:block/leaves} with {@code block/<id>} (no biome tint),
     * one model per fruiting stage for {@link FruitingSpiceLeavesBlock}
     * ({@code block/<id>_stage<n>} for stages above 0). Sapling: cross model.
     * Log and leaves items use their block model; the sapling's flat item model
     * is generated through {@code ItemModelHelper}.
     */
    private void registerSpiceTree(SpiceTree tree) {
        logBlock(tree.getLog().get());
        logBlock(tree.getStrippedLog().get());

        LeavesBlock leaves = tree.getLeaves().get();
        String leavesId = tree.getLeavesId();
        if (leaves instanceof FruitingSpiceLeavesBlock) {
            ModelFile[] stageModels = new ModelFile[Constants.SPICE_TREE_LEAF_GROWTH_STAGES + 1];
            for (int age = 0; age < stageModels.length; age++) {
                String modelId = age == 0 ? leavesId : leavesId + "_stage" + age;
                stageModels[age] = leavesModel(modelId);
            }
            getVariantBuilder(leaves).forAllStates(state -> ConfiguredModel.builder()
                    .modelFile(stageModels[state.getValue(FruitingSpiceLeavesBlock.AGE)])
                    .build());
        } else {
            simpleBlock(leaves, leavesModel(leavesId));
        }

        simpleBlock(tree.getSapling().get(), models().cross(tree.getSaplingId(), modLoc("block/" + tree.getSaplingId()))
                .renderType("minecraft:cutout"));

        for (String itemId : new String[] { tree.getLogId(), tree.getStrippedLogId(), leavesId })
            itemModels().withExistingParent(itemId, modLoc("block/" + itemId));
    }

    private ModelFile leavesModel(String modelId) {
        return models().withExistingParent(modelId, "block/leaves")
                .texture("all", modLoc("block/" + modelId))
                .renderType("minecraft:cutout_mipped");
    }
}
