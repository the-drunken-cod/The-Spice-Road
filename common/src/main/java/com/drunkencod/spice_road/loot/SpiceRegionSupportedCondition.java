package com.drunkencod.spice_road.loot;

import com.google.common.collect.ImmutableSet;
import com.mojang.serialization.MapCodec;

import java.util.Set;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.parameters.LootContextParam;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;
import net.minecraft.world.level.storage.loot.predicates.LootItemConditionType;
import net.minecraft.world.phys.Vec3;

import com.drunkencod.spice_road.block.SpicePlantBlock;
import com.drunkencod.spice_road.registry.ModLootConditions;

/**
 * Loot condition mirroring {@link SpicePlantBlock#canGrow} at break-time:
 * true if the harvested block isn't a {@link SpicePlantBlock} at all, or if
 * its {@code canGrow} check passes for the position it's broken at.
 */
public enum SpiceRegionSupportedCondition implements LootItemCondition {

    INSTANCE;

    public static final MapCodec<SpiceRegionSupportedCondition> CODEC = MapCodec.unit(INSTANCE);

    @Override
    public LootItemConditionType getType() {

        return ModLootConditions.SPICE_REGION_SUPPORTED.get();
    }

    @Override
    public Set<LootContextParam<?>> getReferencedContextParams() {

        return ImmutableSet.of(LootContextParams.ORIGIN, LootContextParams.BLOCK_STATE);
    }

    @Override
    public boolean test(LootContext lootContext) {

        BlockState state = lootContext.getParamOrNull(LootContextParams.BLOCK_STATE);
        if (!(state.getBlock() instanceof SpicePlantBlock block))
            return true;

        Vec3 origin = lootContext.getParamOrNull(LootContextParams.ORIGIN);
        ServerLevel level = lootContext.getLevel();
        return block.canGrow(level, BlockPos.containing(origin));
    }

    /**
     * @return A builder for this condition, ready to pass to
     *         {@code LootPool.Builder#when}.
     */
    public static LootItemCondition.Builder spiceRegionSupported() {

        return () -> INSTANCE;
    }
}
