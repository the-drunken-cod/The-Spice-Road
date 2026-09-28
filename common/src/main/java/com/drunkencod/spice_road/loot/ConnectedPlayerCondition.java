package com.drunkencod.spice_road.loot;

import com.google.common.collect.ImmutableSet;
import com.mojang.serialization.MapCodec;

import java.util.Set;

import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.parameters.LootContextParam;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;
import net.minecraft.world.level.storage.loot.predicates.LootItemConditionType;

import com.drunkencod.spice_road.block.SpiceHarvesting;
import com.drunkencod.spice_road.registry.ModLootConditions;

/**
 * Loot condition enforcing a Spice's Hand-Pick Requirement at break-time: true
 * only if a genuinely connected player caused the drop (see
 * {@link SpiceHarvesting#isConnectedPlayer}).
 * <p>
 * Needed because a Harvest Tool Requirement alone is expressible as a plain
 * {@code minecraft:match_tool} condition, which any simulated player holding
 * that tool would satisfy just as well as a real one. Anything that breaks a
 * block without an entity at all - pistons above all - fails this too, since
 * it leaves {@code this_entity} unset.
 */
public enum ConnectedPlayerCondition implements LootItemCondition {

    INSTANCE;

    /** Codec of this parameterless condition. */
    public static final MapCodec<ConnectedPlayerCondition> CODEC = MapCodec.unit(INSTANCE);

    @Override
    public LootItemConditionType getType() {
        return ModLootConditions.CONNECTED_PLAYER.get();
    }

    @Override
    public Set<LootContextParam<?>> getReferencedContextParams() {
        return ImmutableSet.of(LootContextParams.THIS_ENTITY);
    }

    @Override
    public boolean test(LootContext lootContext) {
        return SpiceHarvesting.isConnectedPlayer(lootContext.getParamOrNull(LootContextParams.THIS_ENTITY));
    }

    /**
     * @return A builder for this condition, ready to pass to
     *         {@code LootPool.Builder#when}.
     */
    public static LootItemCondition.Builder connectedPlayer() {
        return () -> INSTANCE;
    }
}
