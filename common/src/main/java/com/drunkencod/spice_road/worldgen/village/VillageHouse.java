package com.drunkencod.spice_road.worldgen.village;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.levelgen.structure.pools.StructurePoolElement;

/**
 * One datapack-defined village house: a pool element to add to an existing
 * template pool. Read from {@code data/<namespace>/village_house/*.json}, see
 * {@link VillageHouseInjection}.
 *
 * @param pool    The template pool to add the element to, e.g.
 *                {@code minecraft:village/plains/houses}.
 * @param weight  How many times the element is entered into the pool, like the
 *                weight of a vanilla pool entry.
 * @param element The pool element, in the format of a vanilla template pool's
 *                {@code element}.
 */
public record VillageHouse(ResourceLocation pool, int weight, StructurePoolElement element) {

    /** Persistent (JSON) codec. Needs registry ops, for the element's processors. */
    public static final Codec<VillageHouse> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            ResourceLocation.CODEC.fieldOf("pool").forGetter(VillageHouse::pool),
            Codec.intRange(1, 150).optionalFieldOf("weight", 1).forGetter(VillageHouse::weight),
            StructurePoolElement.CODEC.fieldOf("element").forGetter(VillageHouse::element))
            .apply(instance, VillageHouse::new));
}
