package com.drunkencod.spice_road.datagen;

import com.drunkencod.spice_road.Constants;
import com.drunkencod.spice_road.block.SpicePlantBlock;
import com.drunkencod.spice_road.block.FruitingSpiceLeavesBlock;
import com.drunkencod.spice_road.block.SpicePlants;
import com.drunkencod.spice_road.block.SpiceTree;
import com.drunkencod.spice_road.block.SpiceTrees;
import com.drunkencod.spice_road.block.SpiceVineBlock;
import com.drunkencod.spice_road.block.SpiceVines;
import com.drunkencod.spice_road.spice.SourceType;
import com.drunkencod.spice_road.spice.Spice;

import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.data.PackOutput;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.VineBlock;
import net.neoforged.neoforge.client.model.generators.BlockModelBuilder;
import net.neoforged.neoforge.client.model.generators.BlockStateProvider;
import net.neoforged.neoforge.client.model.generators.ConfiguredModel;
import net.neoforged.neoforge.client.model.generators.ModelBuilder;
import net.neoforged.neoforge.client.model.generators.ModelFile;
import net.neoforged.neoforge.client.model.generators.MultiPartBlockStateBuilder;
import net.neoforged.neoforge.client.model.generators.VariantBlockStateBuilder;
import net.neoforged.neoforge.common.data.ExistingFileHelper;

/**
 * Datagens the crop-shaped blockstate + one
 * {@code minecraft:block/crop}-parented model per growth stage (age
 * {@code 0..getMaxAge()}) for every registered Spice Plant block (see
 * {@link SpicePlants}) - {@code spice_road:block/rhizome}-parented instead for
 * {@code RHIZOME} Spices ({@code spice_road:block/rhizome_aquatic}-parented,
 * with an extra {@code _top} texture per stage, for Aquatic Spices) and
 * {@code minecraft:block/cross}-parented for {@code FLOWER_PATCH} Spices.
 * Blockstate properties besides the age (e.g.
 * {@code waterlogged}) don't affect the model. Mirrors vanilla crop blocks
 * (e.g. wheat) exactly; see
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

            SourceType sourceType = plant.spice().getSourceType();
            boolean aquatic = plant.spice().isAquatic();
            ResourceLocation parent = switch (sourceType) {
                case RHIZOME -> modLoc(aquatic ? "block/rhizome_aquatic" : "block/rhizome");
                case FLOWER_PATCH, BUSH -> mcLoc("block/cross");
                default -> mcLoc("block/crop");
            };
            String textureSlot = isCross(sourceType) ? "cross" : "crop";
            ModelFile[] stageModels = new ModelFile[maxAge + 1];
            for (int age = 0; age <= maxAge; age++) {
                ResourceLocation stage = SpiceAssetPaths.block(plant.spice(), id.getPath() + "_stage" + age);
                ModelBuilder<BlockModelBuilder> model = models().withExistingParent(stage.toString(), parent)
                        .texture(textureSlot, stage)
                        .renderType("minecraft:cutout");
                if (aquatic)
                    model.texture("top", SpiceAssetPaths.block(plant.spice(), id.getPath() + "_stage" + age + "_top"));
                stageModels[age] = model;
            }

            applyCropVariants(block, stageModels, maxAge);
            if (plant.worldgenBlock() != plant.block())
                applyCropVariants(plant.worldgenBlock().get(), stageModels, maxAge);
        });

        SpiceTrees.getRegistered().values().forEach(this::registerSpiceTree);
        SpiceVines.getRegistered().values().forEach(vine -> registerSpiceVine(vine.spice(), vine.block().get()));
    }

    /** @return Whether {@code sourceType} uses the {@code minecraft:block/cross} model. */
    private static boolean isCross(SourceType sourceType) {
        return sourceType == SourceType.FLOWER_PATCH || sourceType == SourceType.BUSH;
    }

    /**
     * Mirrors vanilla's multipart vine blockstate, with one
     * {@code minecraft:block/vine}-parented model per ripening stage
     * ({@code block/<spice>/<id>_stage<n>}). The vine item's flat model is
     * generated through {@code ItemModelHelper}.
     */
    private void registerSpiceVine(Spice spice, SpiceVineBlock vine) {
        String id = BuiltInRegistries.BLOCK.getKey(vine).getPath();
        MultiPartBlockStateBuilder builder = getMultipartBuilder(vine);
        for (int age = 0; age <= Constants.SPICE_VINE_GROWTH_STAGES; age++) {
            ResourceLocation stage = SpiceAssetPaths.block(spice, id + "_stage" + age);
            ModelFile model = models().withExistingParent(stage.toString(), "block/vine")
                    .texture("vine", stage)
                    .texture("particle", stage)
                    .renderType("minecraft:cutout");

            for (Direction direction : SpiceVineBlock.FACES) {
                builder.part().modelFile(model)
                        .rotationX(direction == Direction.UP ? 270 : 0)
                        .rotationY(direction.getAxis().isHorizontal() ? ((int) direction.toYRot() + 180) % 360 : 0)
                        .uvLock(direction != Direction.NORTH)
                        .addModel()
                        .condition(VineBlock.getPropertyForFace(direction), true)
                        .condition(SpiceVineBlock.AGE, age)
                        .end();
            }
        }
    }

    /**
     * Maps every {@code age} state of {@code block} to its stage model,
     * clamped to {@code maxAge}. Reused as-is for a worldgen-only
     * ({@code wild_}) block sharing another block's already-generated
     * {@code stageModels}, so no duplicate textures/models are needed for it.
     */
    private void applyCropVariants(SpicePlantBlock block, ModelFile[] stageModels, int maxAge) {
        VariantBlockStateBuilder builder = getVariantBuilder(block);
        for (int age : block.getAgeProperty().getPossibleValues())
            builder.partialState().with(block.getAgeProperty(), age)
                    .modelForState().modelFile(stageModels[Math.min(age, maxAge)]).addModel();
    }

    /**
     * Log/stripped log: vanilla-style axis-rotated columns
     * ({@code block/<spice>/<id>} sides, {@code block/<spice>/<id>_top} ends).
     * Leaves: {@code minecraft:block/leaves} with {@code block/<spice>/<id>}
     * (no biome tint), one model per fruiting stage for
     * {@link FruitingSpiceLeavesBlock} ({@code block/<spice>/<id>_stage<n>}
     * for stages above 0). Sapling: cross model. Log and leaves items use
     * their block model; the sapling's flat item model is generated through
     * {@code ItemModelHelper}.
     */
    private void registerSpiceTree(SpiceTree tree) {
        Spice spice = tree.getSpice();
        spiceLogBlock(spice, tree.getLog().get(), tree.getLogId());
        spiceLogBlock(spice, tree.getStrippedLog().get(), tree.getStrippedLogId());

        LeavesBlock leaves = tree.getLeaves().get();
        String leavesId = tree.getLeavesId();
        if (leaves instanceof FruitingSpiceLeavesBlock) {
            ModelFile[] stageModels = new ModelFile[Constants.SPICE_TREE_LEAF_GROWTH_STAGES + 1];
            for (int age = 0; age < stageModels.length; age++) {
                String modelId = age == 0 ? leavesId : leavesId + "_stage" + age;
                stageModels[age] = leavesModel(SpiceAssetPaths.block(spice, modelId));
            }
            getVariantBuilder(leaves).forAllStates(state -> ConfiguredModel.builder()
                    .modelFile(stageModels[state.getValue(FruitingSpiceLeavesBlock.AGE)])
                    .build());
        } else {
            simpleBlock(leaves, leavesModel(SpiceAssetPaths.block(spice, leavesId)));
        }

        ResourceLocation sapling = SpiceAssetPaths.block(spice, tree.getSaplingId());
        simpleBlock(tree.getSapling().get(), models().cross(sapling.toString(), sapling)
                .renderType("minecraft:cutout"));

        for (String itemId : new String[] { tree.getLogId(), tree.getStrippedLogId(), leavesId })
            itemModels().withExistingParent(itemId, SpiceAssetPaths.block(spice, itemId));
    }

    /**
     * Equivalent of {@code logBlock}, with models and textures in {@code spice}'s
     * folder.
     */
    private void spiceLogBlock(Spice spice, RotatedPillarBlock log, String id) {
        ResourceLocation side = SpiceAssetPaths.block(spice, id);
        ResourceLocation end = SpiceAssetPaths.block(spice, id + "_top");
        axisBlock(log, models().cubeColumn(side.toString(), side, end),
                models().cubeColumnHorizontal(side + "_horizontal", side, end));
    }

    private ModelFile leavesModel(ResourceLocation path) {
        return models().withExistingParent(path.toString(), "block/leaves")
                .texture("all", path)
                .renderType("minecraft:cutout_mipped");
    }
}
