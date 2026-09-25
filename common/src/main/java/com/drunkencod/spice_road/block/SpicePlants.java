package com.drunkencod.spice_road.block;

import java.util.Collections;
import java.util.EnumMap;
import java.util.Locale;
import java.util.Map;
import java.util.function.Supplier;

import net.minecraft.world.item.Item;

import com.drunkencod.spice_road.datagen.ItemModelHelper;
import com.drunkencod.spice_road.item.RawSpiceItem;
import com.drunkencod.spice_road.platform.Services;
import com.drunkencod.spice_road.spice.Spice;
import com.drunkencod.spice_road.spice.SourceType;

/**
 * Instantiates the actual block/item registrations for every {@link Spice}
 * enum member whose {@link SourceType} is handled by this pass. Driven
 * entirely by each member's own fields; nothing here is hand-authored per
 * spice.
 * <p>
 * {@code TREE}/{@code BUSH}/{@code VINE}/{@code RHIZOME} members are
 * deliberately skipped - they need mechanics (sapling stages, farmland
 * spreading) not implemented yet.
 * <p>
 * <b>Contains prototype wiring - subject to change.</b>
 */
public final class SpicePlants {

    /** One registered Spice Plant's block + item suppliers. */
    public record RegisteredSpicePlant(
            Spice spice,
            Supplier<? extends SpicePlantBlock> block,
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

        String id = spice.name().toLowerCase(Locale.ROOT);
        String seedId = id + "_seeds";
        String blockId = id + (spice.getSourceType() == SourceType.FLOWER_PATCH ? "_flower" : "_crop");

        Supplier<Item> productItem = Services.REGISTRY.registerItem(id,
                () -> new RawSpiceItem(new Item.Properties(), spice));
        Supplier<Item> seedItem = Services.REGISTRY.registerItem(seedId, () -> new Item(new Item.Properties()));
        ItemModelHelper.addFlatItem(id);
        ItemModelHelper.addFlatItem(seedId);

        Supplier<? extends SpicePlantBlock> block;
        if (spice.getSourceType() == SourceType.FLOWER_PATCH) {
            block = Services.REGISTRY.registerBlock(blockId,
                    () -> new FlowerPatchBlock(SpicePlantBlock.defaultProperties(), seedItem));
        } else {
            block = Services.REGISTRY.registerBlock(blockId,
                    () -> new SpiceCropBlock(SpicePlantBlock.defaultProperties(), seedItem));
        }

        return new RegisteredSpicePlant(spice, block, seedItem, productItem);
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
