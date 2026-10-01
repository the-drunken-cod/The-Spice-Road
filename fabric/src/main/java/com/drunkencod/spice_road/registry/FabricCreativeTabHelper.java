package com.drunkencod.spice_road.registry;

import com.drunkencod.spice_road.Constants;
import com.drunkencod.spice_road.spice.Spice;

import net.fabricmc.fabric.api.itemgroup.v1.FabricItemGroup;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/**
 * Fabric implementation of {@link ICreativeTabHelper}, registering the tabs
 * eagerly.
 */
public class FabricCreativeTabHelper implements ICreativeTabHelper {

    @Override
    public void register() {
        Registry.register(BuiltInRegistries.CREATIVE_MODE_TAB,
                ResourceLocation.fromNamespaceAndPath(Constants.MOD_ID,
                        ICreativeTabHelper.TAB_GENERIC_KEY),
                FabricItemGroup.builder()
                        .title(Component.translatable(ICreativeTabHelper.TAB_GENERIC_TR_KEY))
                        .icon(() -> Items.ROTTEN_FLESH.getDefaultInstance())
                        .displayItems((params, output) -> ModItems.populateGenericTab(output))
                        .build());
        Registry.register(BuiltInRegistries.CREATIVE_MODE_TAB,
                ResourceLocation.fromNamespaceAndPath(Constants.MOD_ID, ICreativeTabHelper.TAB_SPICES_KEY),
                FabricItemGroup.builder()
                        .title(Component.translatable(ICreativeTabHelper.TAB_SPICES_TR_KEY))
                        .icon(() -> new ItemStack(Spice.getRawById(Spice.HABANERO.getId())))
                        .displayItems((params, output) -> ModItems.populateSpicesTab(output))
                        .build());
    }
}
