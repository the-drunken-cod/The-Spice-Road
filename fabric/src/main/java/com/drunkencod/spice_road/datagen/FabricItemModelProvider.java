package com.drunkencod.spice_road.datagen;

import net.fabricmc.fabric.api.datagen.v1.provider.FabricModelProvider;

import com.drunkencod.spice_road.Constants;
import com.drunkencod.spice_road.datagen.ItemModelHelper;

import net.fabricmc.fabric.api.datagen.v1.FabricDataOutput;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.data.models.BlockModelGenerators;
import net.minecraft.data.models.ItemModelGenerators;
import net.minecraft.data.models.model.ModelTemplates;
import net.minecraft.resources.ResourceLocation;

public class FabricItemModelProvider extends FabricModelProvider {

    public FabricItemModelProvider(FabricDataOutput output) {
        super(output);
    }

    @Override
    public void generateBlockStateModels(BlockModelGenerators generators) {
        // No block models in this provider; add here when needed
    }

    @Override
    public void generateItemModels(ItemModelGenerators generators) {
        for (String id : ItemModelHelper.getFlatItemIds()) {
            generators.generateFlatItem(
                    BuiltInRegistries.ITEM.get(
                            ResourceLocation.fromNamespaceAndPath(Constants.MOD_ID, id)),
                    ModelTemplates.FLAT_ITEM);
        }
    }
}
