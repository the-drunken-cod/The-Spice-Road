package com.drunkencod.spice_road.villager.trader;

import java.util.function.DoubleSupplier;
import java.util.function.IntSupplier;

import com.mojang.serialization.Codec;

import net.minecraft.util.StringRepresentable;

import com.drunkencod.spice_road.platform.Services;

/**
 * One of the wandering trader's optional Spice offers. Each pool is one
 * listing in the wandering trader's offers, with its own chance and its own
 * number of entries in the vanilla offer pool.
 */
public enum WanderingTradePool implements StringRepresentable {
    /** Seeds, cuttings, saplings and vine segments. */
    PLANTING_ITEMS("planting_items", () -> Services.CONFIG.getWanderingTraderPlantingItemChance(),
            () -> Services.CONFIG.getWanderingTraderPlantingItemEntries()),
    /** Spice Mix presets. */
    MIXES("mixes", () -> Services.CONFIG.getWanderingTraderMixChance(),
            () -> Services.CONFIG.getWanderingTraderMixEntries());

    /** Persistent (JSON) codec. */
    public static final Codec<WanderingTradePool> CODEC = StringRepresentable.fromEnum(WanderingTradePool::values);

    private final String name;
    private final DoubleSupplier chance;
    private final IntSupplier entries;

    WanderingTradePool(String name, DoubleSupplier chance, IntSupplier entries) {
        this.name = name;
        this.chance = chance;
        this.entries = entries;
    }

    /**
     * @return The configured chance, from {@code 0.0} to {@code 1.0}, that this
     *         pool's offer is made when one of its entries is drawn.
     */
    public double getChance() {
        return chance.getAsDouble();
    }

    /**
     * @return The configured number of entries this pool's listing has in the
     *         vanilla offer pool, {@code 0} or more.
     */
    public int getEntries() {
        return entries.getAsInt();
    }

    @Override
    public String getSerializedName() {
        return name;
    }
}
