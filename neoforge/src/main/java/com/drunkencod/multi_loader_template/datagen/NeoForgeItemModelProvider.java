package com.drunkencod.multi_loader_template.datagen;

import com.drunkencod.multi_loader_template.Constants;

import net.minecraft.data.PackOutput;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.client.model.generators.ItemModelProvider;
import net.neoforged.neoforge.common.data.ExistingFileHelper;

public class NeoForgeItemModelProvider extends ItemModelProvider {

    public NeoForgeItemModelProvider(PackOutput output, ExistingFileHelper existingFileHelper) {
        super(output, Constants.MOD_ID, existingFileHelper);
    }

    @Override
    protected void registerModels() {
        for (String id : ItemModelHelper.getFlatItemIds()) {
            withExistingParent(id, ResourceLocation.withDefaultNamespace("item/generated"))
                    .texture("layer0", modLoc("item/" + id));
        }
    }
}
