package com.drunkencod.spice_road.villager.trader;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.jetbrains.annotations.Nullable;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import com.drunkencod.spice_road.Constants;
import com.drunkencod.spice_road.block.SpiceTree;
import com.drunkencod.spice_road.block.SpiceTrees;
import com.drunkencod.spice_road.block.SpiceVines;
import com.drunkencod.spice_road.block.SpiceVines.RegisteredSpiceVine;
import com.drunkencod.spice_road.mix.MixPreset;
import com.drunkencod.spice_road.mix.MixPresetRegistry;
import com.drunkencod.spice_road.mix.SpiceMix;
import com.drunkencod.spice_road.mix.SpiceMixes;
import com.drunkencod.spice_road.registry.ModItems;
import com.drunkencod.spice_road.spice.ProcessedSpice;
import com.drunkencod.spice_road.spice.Spice;
import com.drunkencod.spice_road.spice.Tier;

/**
 * A pool of goods a trade catalog entry may offer. Which goods it holds is
 * worked out when an offer is rolled, so it follows the datapacks and the
 * config as they are then.
 */
public sealed interface SpiceTradeSource {

    /** ID of the {@link Spices} source type. */
    ResourceLocation SPICES_TYPE = ResourceLocation.fromNamespaceAndPath(Constants.MOD_ID, "spices");
    /** ID of the {@link Presets} source type. */
    ResourceLocation PRESETS_TYPE = ResourceLocation.fromNamespaceAndPath(Constants.MOD_ID, "presets");

    /** ID of the {@link PlantingItems} source type. */
    ResourceLocation PLANTING_ITEMS_TYPE = ResourceLocation.fromNamespaceAndPath(Constants.MOD_ID, "planting_items");

    /** Persistent (JSON) codec, selecting the source by its {@code type}. */
    Codec<SpiceTradeSource> CODEC = ResourceLocation.CODEC
            .validate(type -> SPICES_TYPE.equals(type) || PRESETS_TYPE.equals(type)
                    || PLANTING_ITEMS_TYPE.equals(type)
                            ? DataResult.success(type)
                            : DataResult.error(() -> "Unknown trade source type " + type))
            .dispatch(SpiceTradeSource::type, type -> {
                if (SPICES_TYPE.equals(type))
                    return Spices.CODEC;
                return PRESETS_TYPE.equals(type) ? Presets.CODEC : PlantingItems.CODEC;
            });

    /** @return The ID of this source's type. */
    ResourceLocation type();

    /**
     * @param direction Which way the offer goes.
     * @return Every good this source holds right now, valued for that
     *         direction. Empty if nothing qualifies at the moment.
     */
    List<TradeGood> goods(SpiceTradeDirection direction);

    /**
     * Loose spices: the raw item of every Spice and, optionally, every
     * Processed Spice, of the listed Tiers.
     *
     * @param tiers     Which {@link Tier}s qualify.
     * @param processed Whether Processed Spices qualify too.
     */
    record Spices(List<Tier> tiers, boolean processed) implements SpiceTradeSource {

        /** Persistent (JSON) codec. */
        public static final MapCodec<Spices> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
                Tier.CODEC.listOf().fieldOf("tiers").forGetter(Spices::tiers),
                Codec.BOOL.optionalFieldOf("processed", true).forGetter(Spices::processed))
                .apply(instance, Spices::new));

        @Override
        public ResourceLocation type() {
            return SPICES_TYPE;
        }

        @Override
        public List<TradeGood> goods(SpiceTradeDirection direction) {
            List<TradeGood> goods = new ArrayList<>();
            for (Spice spice : Spice.values())
                add(goods, ModItems.byId(spice.getId()), spice.getTier(), direction);
            if (processed) {
                for (ProcessedSpice spice : ProcessedSpice.values())
                    add(goods, spice.getItem(), spice.getTier(), direction);
            }
            return goods;
        }

        private void add(List<TradeGood> goods, Item item, Tier tier, SpiceTradeDirection direction) {
            if (item == null || !tiers.contains(tier))
                return;
            goods.add(new TradeGood(new ItemStack(item), SpiceTradePricing.valueOf(tier, direction), true));
        }
    }

    /**
     * Mix Presets, each as its standard batch (the first member of every tag
     * in its proportions). Priced by the Tiers of the spices in it.
     *
     * @param ids     Which presets qualify; empty means any.
     * @param maxTier The highest {@link Tier} a preset may have, where a
     *                preset's Tier is that of its rarest spice.
     */
    record Presets(List<ResourceLocation> ids, Tier maxTier) implements SpiceTradeSource {

        /** Persistent (JSON) codec. */
        public static final MapCodec<Presets> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
                ResourceLocation.CODEC.listOf().optionalFieldOf("ids", List.of()).forGetter(Presets::ids),
                Tier.CODEC.optionalFieldOf("max_tier", Tier.EPIC).forGetter(Presets::maxTier))
                .apply(instance, Presets::new));

        @Override
        public ResourceLocation type() {
            return PRESETS_TYPE;
        }

        @Override
        public List<TradeGood> goods(SpiceTradeDirection direction) {
            List<TradeGood> goods = new ArrayList<>();
            for (Map.Entry<ResourceLocation, MixPreset> entry : MixPresetRegistry.getAll().entrySet()) {
                if (!ids.isEmpty() && !ids.contains(entry.getKey()))
                    continue;
                Optional<ItemStack> stack = SpiceMixes.ofPreset(entry.getKey(), entry.getValue());
                Optional<SpiceMix> mix = stack.flatMap(SpiceMixes::contentsOf);
                if (mix.isEmpty())
                    continue;
                Map<Item, Integer> spices = mix.get().spices();
                if (SpiceTradePricing.rarestTierOf(spices).ordinal() > maxTier.ordinal())
                    continue;
                double value = 0.0;
                for (Map.Entry<Item, Integer> spice : spices.entrySet()) {
                    Tier tier = SpiceTradePricing.tierOf(spice.getKey());
                    value += spice.getValue() * SpiceTradePricing.valueOf(tier != null ? tier : Tier.COMMON, direction);
                }
                goods.add(new TradeGood(stack.get(), value, false));
            }
            return goods;
        }
    }

    /**
     * Planting Items: the seeds or cuttings, the sapling and the vine segment
     * of every Spice of the listed Tiers that has them. Only ever sold: it
     * holds nothing in the {@link SpiceTradeDirection#BUY} direction.
     *
     * @param tiers Which {@link Tier}s qualify.
     */
    record PlantingItems(List<Tier> tiers) implements SpiceTradeSource {

        /** Persistent (JSON) codec. */
        public static final MapCodec<PlantingItems> CODEC = RecordCodecBuilder.mapCodec(instance -> instance
                .group(Tier.CODEC.listOf().fieldOf("tiers").forGetter(PlantingItems::tiers))
                .apply(instance, PlantingItems::new));

        @Override
        public ResourceLocation type() {
            return PLANTING_ITEMS_TYPE;
        }

        @Override
        public List<TradeGood> goods(SpiceTradeDirection direction) {
            List<TradeGood> goods = new ArrayList<>();
            if (direction != SpiceTradeDirection.SELL)
                return goods;
            for (Spice spice : Spice.values()) {
                Tier tier = spice.getTier();
                if (!tiers.contains(tier))
                    continue;
                SpiceTree tree = SpiceTrees.getRegistered().get(spice);
                RegisteredSpiceVine vine = SpiceVines.getRegistered().get(spice);
                add(goods, Spice.getSeedsById(spice.getId()), tier);
                add(goods, tree != null ? tree.getSaplingItem().get() : null, tier);
                add(goods, vine != null ? vine.vineItem().get() : null, tier);
            }
            return goods;
        }

        private static void add(List<TradeGood> goods, @Nullable Item item, Tier tier) {
            if (item != null)
                goods.add(new TradeGood(new ItemStack(item), SpiceTradePricing.plantingItemValue(tier), false));
        }
    }
}
