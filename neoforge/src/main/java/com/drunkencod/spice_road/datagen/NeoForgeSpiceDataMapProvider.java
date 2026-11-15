package com.drunkencod.spice_road.datagen;

import java.util.concurrent.CompletableFuture;

import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.data.PackOutput;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.neoforged.neoforge.common.data.DataMapProvider;
import net.neoforged.neoforge.registries.datamaps.builtin.Compostable;
import net.neoforged.neoforge.registries.datamaps.builtin.NeoForgeDataMaps;
import net.neoforged.neoforge.registries.datamaps.builtin.Strippable;

import com.drunkencod.spice_road.block.SpiceTrees;
import com.drunkencod.spice_road.item.SpiceCompostables;

/**
 * Datagens NeoForge data maps:
 * <ul>
 * <li>{@code neoforge:strippables}, mapping every Spice Tree log to its
 * stripped variant, which is what axes (and anything else using NeoForge's
 * {@code AXE_STRIP} item ability, e.g. Create's Deployer) use to strip it.
 * Fabric registers the same mapping in code via
 * {@code StrippableBlockRegistry}.</li>
 * <li>{@code neoforge:compostables}, see {@link SpiceCompostables}. Fabric
 * registers these in code via {@code CompostingChanceRegistry}.</li>
 * </ul>
 */
public class NeoForgeSpiceDataMapProvider extends DataMapProvider {

    public NeoForgeSpiceDataMapProvider(PackOutput output, CompletableFuture<HolderLookup.Provider> lookupProvider) {
        super(output, lookupProvider);
    }

    @Override
    protected void gather(HolderLookup.Provider provider) {
        Builder<Strippable, Block> strippables = builder(NeoForgeDataMaps.STRIPPABLES);
        SpiceTrees.getRegistered().values().forEach(tree -> strippables.add(
                BuiltInRegistries.BLOCK.getKey(tree.getLog().get()),
                new Strippable(tree.getStrippedLog().get()),
                false));

        Builder<Compostable, Item> compostables = builder(NeoForgeDataMaps.COMPOSTABLES);
        SpiceCompostables.getAll().forEach((item, chance) -> compostables.add(
                BuiltInRegistries.ITEM.getKey(item), new Compostable(chance), false));
    }
}
