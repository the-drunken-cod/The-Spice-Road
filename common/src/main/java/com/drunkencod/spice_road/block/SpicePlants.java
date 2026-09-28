package com.drunkencod.spice_road.block;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

import net.minecraft.world.item.Item;
import net.minecraft.world.level.ItemLike;

import com.drunkencod.spice_road.datagen.ItemModelHelper;
import com.drunkencod.spice_road.platform.Services;
import com.drunkencod.spice_road.spice.Spice;
import com.drunkencod.spice_road.spice.SourceType;

/**
 * Instantiates the actual block/item registrations for every {@link Spice}
 * enum member whose {@link SourceType} is handled by this pass. Driven
 * entirely by each member's own fields; nothing here is hand-authored per
 * spice.
 * <p>
 * {@code TREE} and {@code VINE} members are registered by {@link SpiceTrees}
 * and {@link SpiceVines} instead. {@code BUSH}/{@code RHIZOME} members are
 * deliberately skipped - they need mechanics (farmland spreading) not
 * implemented yet.
 * <p>
 * <b>Contains prototype wiring - subject to change.</b>
 */
public final class SpicePlants {

    /**
     * One registered Spice Plant's block + item suppliers.
     *
     * @param block        The seed-planted, farmland-only block.
     * @param worldgenBlock The block {@code SpicePlantFeature} actually
     *                      places. Equal to {@code block} for
     *                      {@code FLOWER_PATCH} Spices; a separate
     *                      {@link WildSpiceCropBlock} for {@code CROP}
     *                      Spices (see {@link SpiceCropBlock}).
     */
    public record RegisteredSpicePlant(
            Spice spice,
            Supplier<? extends SpicePlantBlock> block,
            Supplier<? extends SpicePlantBlock> worldgenBlock,
            Supplier<? extends Item> seedItem,
            Supplier<? extends Item> productItem) {
    }

    private static final Map<Spice, RegisteredSpicePlant> REGISTERED = new EnumMap<>(Spice.class);

    private SpicePlants() {
    }

    /**
     * Registers every in-scope {@link Spice} member's block + items. Must be
     * called during mod initialization (see {@code SpiceRoad#init()}).
     */
    public static void bootstrap() {
        for (Spice spice : Spice.values()) {
            if (spice.getSourceType() != SourceType.FLOWER_PATCH && spice.getSourceType() != SourceType.CROP)
                continue;

            REGISTERED.put(spice, register(spice));
        }
    }

    private static RegisteredSpicePlant register(Spice spice) {
        String id = spice.getId();
        String seedId = id + "_seeds";
        String blockId = id + (spice.getSourceType() == SourceType.FLOWER_PATCH ? "_flower" : "_crop");

        Supplier<Item> productItem = Services.REGISTRY.registerItem(id, () -> new Item(new Item.Properties()));

        @SuppressWarnings("unchecked")
        Supplier<Item>[] seedItemHolder = new Supplier[1];
        Supplier<ItemLike> seedItemRef = () -> seedItemHolder[0].get();

        Supplier<? extends SpicePlantBlock> block;
        Supplier<? extends SpicePlantBlock> worldgenBlock;
        if (spice.getSourceType() == SourceType.FLOWER_PATCH) {
            block = Services.REGISTRY.registerBlock(blockId,
                    () -> new FlowerPatchBlock(SpicePlantBlock.defaultProperties(), seedItemRef, spice));
            worldgenBlock = block;
        } else {
            block = Services.REGISTRY.registerBlock(blockId,
                    () -> new SpiceCropBlock(SpicePlantBlock.defaultProperties(), seedItemRef, spice));
            worldgenBlock = Services.REGISTRY.registerBlock("wild_" + blockId,
                    () -> new WildSpiceCropBlock(SpicePlantBlock.defaultProperties(), seedItemRef, spice));
        }

        Supplier<Item> seedItem = Services.REGISTRY.registerItem(seedId,
                () -> new SpiceSeedItem(block.get(), new Item.Properties()));
        seedItemHolder[0] = seedItem;

        ItemModelHelper.addFlatItem(id);
        ItemModelHelper.addFlatItem(seedId);

        return new RegisteredSpicePlant(spice, block, worldgenBlock, seedItem, productItem);
    }

    /**
     * @return Every registered {@link SpicePlantBlock} instance, including
     *         both {@link RegisteredSpicePlant#block} and
     *         {@link RegisteredSpicePlant#worldgenBlock} where they differ.
     *         Consumed by blockstate/model datagen, which doesn't need the
     *         rest of a Spice Plant's registration data.
     */
    public static List<SpicePlantBlock> getAllBlocks() {
        List<SpicePlantBlock> blocks = new ArrayList<>();
        for (RegisteredSpicePlant plant : REGISTERED.values()) {
            blocks.add(plant.block().get());
            if (plant.worldgenBlock() != plant.block())
                blocks.add(plant.worldgenBlock().get());
        }
        return blocks;
    }

    /**
     * @return All Spice Plants registered by {@link #bootstrap()}, keyed by
     *         their {@link Spice}. Consumed by datagen (loot tables) and,
     *         once built, harvest/interaction logic.
     */
    public static Map<Spice, RegisteredSpicePlant> getRegistered() {
        return Collections.unmodifiableMap(REGISTERED);
    }
}
