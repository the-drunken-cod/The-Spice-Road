package com.drunkencod.spice_road.block;

import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;
import java.util.function.Supplier;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.FireBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.levelgen.feature.ConfiguredFeature;

import com.drunkencod.spice_road.Constants;
import com.drunkencod.spice_road.datagen.ItemModelHelper;
import com.drunkencod.spice_road.item.SpiceItem;
import com.drunkencod.spice_road.platform.Services;
import com.drunkencod.spice_road.spice.SourceType;
import com.drunkencod.spice_road.spice.Spice;

/**
 * Registers a {@link SpiceVineBlock}, its block item and the raw Spice item
 * for every {@link Spice} enum member with {@link SourceType#VINE}, driven
 * entirely by the member's own fields. Vines have no seeds - the vine block
 * item, obtained by shearing a segment, is what gets replanted.
 * <p>
 * Worldgen places vines by generating their Host Tree, see
 * {@link RegisteredSpiceVine#hostTreeFeature()}.
 */
public final class SpiceVines {

    /**
     * One registered Spice Vine's block and items.
     *
     * @param spice           The Spice this vine grows.
     * @param hostTreeFeature The key of the datapack-defined
     *                        {@code minecraft:tree} configured feature for
     *                        this Spice's Host Tree - the tree worldgen
     *                        generates to carry the vine, i.e.
     *                        {@code data/<namespace>/worldgen/configured_feature/<id>_host_tree.json}.
     *                        Its {@code spice_road:attached_to_logs}
     *                        decorators are what actually seed the vine (see
     *                        {@code AttachedToLogsDecorator}).
     * @param block           The vine block, registered as {@code <id>_vine}.
     * @param vineItem        The vine block's item, used to replant it.
     * @param productItem     The raw Spice item, registered as {@code <id>}.
     */
    public record RegisteredSpiceVine(
            Spice spice,
            ResourceKey<ConfiguredFeature<?, ?>> hostTreeFeature,
            Supplier<SpiceVineBlock> block,
            Supplier<Item> vineItem,
            Supplier<Item> productItem) {
    }

    private static final Map<Spice, RegisteredSpiceVine> REGISTERED = new EnumMap<>(Spice.class);

    private SpiceVines() {
    }

    /**
     * Registers every vine Spice's block and items. Must be called during mod
     * initialization (see {@code SpiceRoad#init()}).
     */
    public static void bootstrap() {
        for (Spice spice : Spice.values()) {
            if (spice.getSourceType() == SourceType.VINE)
                REGISTERED.put(spice, register(spice));
        }
    }

    private static RegisteredSpiceVine register(Spice spice) {
        String id = spice.getId();
        String vineId = id + "_vine";
        ResourceKey<ConfiguredFeature<?, ?>> hostTreeFeature = ResourceKey.create(Registries.CONFIGURED_FEATURE,
                ResourceLocation.fromNamespaceAndPath(Constants.MOD_ID, id + "_host_tree"));

        Supplier<Item> productItem = Services.REGISTRY.registerItem(id,
                () -> new SpiceItem(SpiceItem.defaultProperties(spice)));
        Supplier<SpiceVineBlock> block = Services.REGISTRY.registerBlock(vineId,
                () -> new SpiceVineBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.VINE), spice, productItem));
        Supplier<Item> vineItem = Services.REGISTRY.registerItem(vineId,
                () -> new SpiceVineItem(block.get(), SpiceVineItem.defaultProperties(spice)));

        ItemModelHelper.addFlatItem(id, spice);
        ItemModelHelper.addFlatItem(vineId, spice);

        return new RegisteredSpiceVine(spice, hostTreeFeature, block, vineItem, productItem);
    }

    /**
     * Makes vines burn like vanilla ones. Must be called after block
     * registration has completed (see {@code SpiceRoad#commonSetup()}).
     */
    public static void registerFlammability() {
        FireBlock fire = (FireBlock) Blocks.FIRE;
        REGISTERED.values().forEach(vine -> fire.setFlammable(vine.block().get(), 15, 100));
    }

    /**
     * @return All Spice Vines registered by {@link #bootstrap()}, keyed by
     *         their {@link Spice}.
     */
    public static Map<Spice, RegisteredSpiceVine> getRegistered() {
        return Collections.unmodifiableMap(REGISTERED);
    }
}
