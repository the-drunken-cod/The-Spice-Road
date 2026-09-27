package com.drunkencod.spice_road.loot;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import com.google.common.collect.ImmutableSet;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.saveddata.maps.MapDecorationType;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.functions.LootItemConditionalFunction;
import net.minecraft.world.level.storage.loot.functions.LootItemFunctionType;
import net.minecraft.world.level.storage.loot.parameters.LootContextParam;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;
import net.minecraft.world.phys.Vec3;

import com.drunkencod.spice_road.map.SpiceMaps;
import com.drunkencod.spice_road.platform.Services;
import com.drunkencod.spice_road.registry.ModLootFunctions;
import com.drunkencod.spice_road.spice.Spice;
import com.drunkencod.spice_road.spice.Tier;
import com.drunkencod.spice_road.spice.region.RegionHeartSearch;

/**
 * Loot function turning an empty {@code minecraft:map} into a Spice Map,
 * searching from the loot's origin. The Spice's counterpart of vanilla's
 * {@code minecraft:exploration_map}.
 * <p>
 * Takes exactly one of {@code spice} (map that Spice) or {@code tier} (map a
 * random Spice of that tier which has a Region Heart in range), plus
 * optional {@code zoom}, {@code decoration} and {@code search_radius_cells}
 * (defaults to the configured search radius). Yields nothing if no matching
 * heart is in range, so no blank map is left behind.
 */
public class SpiceMapFunction extends LootItemConditionalFunction {

    /** Codec of this function's JSON fields. */
    public static final MapCodec<SpiceMapFunction> CODEC = RecordCodecBuilder.<SpiceMapFunction>mapCodec(
            instance -> commonFields(instance).and(instance.group(
                    Spice.CODEC.optionalFieldOf("spice").forGetter(function -> function.spice),
                    Tier.CODEC.optionalFieldOf("tier").forGetter(function -> function.tier),
                    MapDecorationType.CODEC.optionalFieldOf("decoration").forGetter(function -> function.decoration),
                    Codec.BYTE.optionalFieldOf("zoom", SpiceMaps.DEFAULT_ZOOM).forGetter(function -> function.zoom),
                    Codec.intRange(1, 1000).optionalFieldOf("search_radius_cells")
                            .forGetter(function -> function.searchRadiusCells)))
                    .apply(instance, SpiceMapFunction::new))
            .validate(function -> function.spice.isPresent() == function.tier.isPresent()
                    ? DataResult.error(() -> "Exactly one of 'spice' or 'tier' must be set")
                    : DataResult.success(function));

    private final Optional<Spice> spice;
    private final Optional<Tier> tier;
    private final Optional<Holder<MapDecorationType>> decoration;
    private final byte zoom;
    private final Optional<Integer> searchRadiusCells;

    private SpiceMapFunction(List<LootItemCondition> conditions, Optional<Spice> spice, Optional<Tier> tier,
            Optional<Holder<MapDecorationType>> decoration, byte zoom, Optional<Integer> searchRadiusCells) {
        super(conditions);
        this.spice = spice;
        this.tier = tier;
        this.decoration = decoration;
        this.zoom = zoom;
        this.searchRadiusCells = searchRadiusCells;
    }

    @Override
    public LootItemFunctionType<SpiceMapFunction> getType() {
        return ModLootFunctions.SPICE_MAP.get();
    }

    @Override
    public Set<LootContextParam<?>> getReferencedContextParams() {
        return ImmutableSet.of(LootContextParams.ORIGIN);
    }

    @Override
    protected ItemStack run(ItemStack stack, LootContext context) {
        if (!stack.is(Items.MAP))
            return stack;

        Vec3 origin = context.getParamOrNull(LootContextParams.ORIGIN);
        if (origin == null)
            return ItemStack.EMPTY;

        ServerLevel level = context.getLevel();
        BlockPos originPos = BlockPos.containing(origin);
        int radius = RegionHeartSearch
                .cellsToBlocks(searchRadiusCells.orElseGet(Services.CONFIG::getSpiceMapSearchRadiusCells));
        Optional<ItemStack> map = spice.isPresent()
                ? SpiceMaps.createForSpice(level, originPos, radius, spice.get(), zoom, decoration.orElse(null))
                : SpiceMaps.createForTier(level, originPos, radius, tier.orElseThrow(), context.getRandom(), zoom,
                        decoration.orElse(null));
        return map.orElse(ItemStack.EMPTY);
    }
}
