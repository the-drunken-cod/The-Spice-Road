package com.drunkencod.spice_road.datagen;

import com.drunkencod.spice_road.Constants;

import net.minecraft.data.PackOutput;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.client.model.generators.ItemModelProvider;
import net.neoforged.neoforge.common.data.ExistingFileHelper;

/**
 * Datagens flat {@code minecraft:item/generated} item models for every item
 * queued via {@link ItemModelHelper#addFlatItem(String)}. See
 * {@code FabricItemModelProvider} for the Fabric counterpart.
 */
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
