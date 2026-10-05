package com.drunkencod.spice_road.block;

import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;

import org.jetbrains.annotations.Nullable;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.grower.TreeGrower;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.levelgen.feature.ConfiguredFeature;

import com.drunkencod.spice_road.Constants;
import com.drunkencod.spice_road.datagen.ItemModelHelper;
import com.drunkencod.spice_road.item.SpiceItem;
import com.drunkencod.spice_road.platform.Services;
import com.drunkencod.spice_road.spice.HarvestAction;
import com.drunkencod.spice_road.spice.SourceType;
import com.drunkencod.spice_road.spice.Spice;

/**
 * One {@link SourceType#TREE} Spice's full set of registered blocks and items:
 * log, stripped log, leaves, sapling, and the raw Spice product.
 * <p>
 * Trees are harvested in one of two ways, see {@link HarvestPart}. Either way,
 * the leaves drop saplings via their (datagenned) loot table.
 * <p>
 * The tree's shape is fully datapack-driven: saplings and Spice Region
 * worldgen both place the {@code minecraft:tree} configured feature at
 * {@link #getTreeFeature()}, i.e.
 * {@code data/<namespace>/worldgen/configured_feature/<id>_tree.json}.
 * <p>
 * Instances are created and registered by {@link SpiceTrees#bootstrap()}.
 */
public final class SpiceTree {

    /** Which part of a Spice Tree yields its Spice. */
    public enum HarvestPart {
        /**
         * Stripping a log (with an axe, a Create Deployer, or anything else
         * that turns the log into its stripped variant) drops the Spice.
         */
        BARK,
        /**
         * Air-exposed leaves grow through fruiting stages and are right-clicked
         * to harvest the Spice once ripe.
         */
        LEAVES
    }

    private final Spice spice;
    private final ResourceKey<ConfiguredFeature<?, ?>> treeFeature;
    private final Supplier<Item> productItem;
    private final Supplier<RotatedPillarBlock> log;
    private final Supplier<RotatedPillarBlock> strippedLog;
    private final Supplier<LeavesBlock> leaves;
    private final Supplier<SpiceSaplingBlock> sapling;
    private final Supplier<Item> logItem;
    private final Supplier<Item> strippedLogItem;
    private final Supplier<Item> leavesItem;
    private final Supplier<Item> saplingItem;

    /**
     * Registers all blocks and items of the given tree Spice.
     *
     * @param spice A {@link Spice} with {@link SourceType#TREE}.
     */
    SpiceTree(Spice spice) {
        this.spice = spice;

        String id = spice.getId();
        this.treeFeature = ResourceKey.create(Registries.CONFIGURED_FEATURE,
                ResourceLocation.fromNamespaceAndPath(Constants.MOD_ID, id + "_tree"));

        TreeGrower grower = new TreeGrower(Constants.MOD_ID + ":" + id, Optional.empty(), Optional.of(treeFeature),
                Optional.empty());

        this.productItem = Services.REGISTRY.registerItem(id, () -> new SpiceItem(SpiceItem.defaultProperties(spice)));
        this.log = Services.REGISTRY.registerBlock(getLogId(),
                () -> new RotatedPillarBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.JUNGLE_LOG)));
        this.strippedLog = Services.REGISTRY.<RotatedPillarBlock>registerBlock(getStrippedLogId(),
                () -> new StrippedSpiceLogBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.STRIPPED_JUNGLE_LOG), this));
        this.leaves = Services.REGISTRY.<LeavesBlock>registerBlock(getLeavesId(), () -> getHarvestPart() == HarvestPart.LEAVES
                ? new FruitingSpiceLeavesBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.JUNGLE_LEAVES), this)
                : new LeavesBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.JUNGLE_LEAVES)));
        this.sapling = Services.REGISTRY.registerBlock(getSaplingId(),
                () -> new SpiceSaplingBlock(grower, BlockBehaviour.Properties.ofFullCopy(Blocks.JUNGLE_SAPLING), spice));

        this.logItem = registerBlockItem(getLogId(), log);
        this.strippedLogItem = registerBlockItem(getStrippedLogId(), strippedLog);
        this.leavesItem = registerBlockItem(getLeavesId(), leaves);
        this.saplingItem = Services.REGISTRY.registerItem(getSaplingId(),
                () -> new SpiceSaplingItem(sapling.get(), SpiceSaplingItem.defaultProperties(spice)));

        ItemModelHelper.addFlatItem(id, spice);
        ItemModelHelper.addFlatItem(getSaplingId(), spice);
    }

    private static Supplier<Item> registerBlockItem(String id, Supplier<? extends Block> block) {
        return Services.REGISTRY.registerItem(id, () -> new BlockItem(block.get(), new Item.Properties()));
    }

    // #region harvest

    /**
     * @return Which part of this tree yields its Spice - {@link HarvestPart#BARK}
     *         for {@link HarvestAction#STRIP} Spices, otherwise
     *         {@link HarvestPart#LEAVES}.
     */
    public HarvestPart getHarvestPart() {
        return spice.getHarvestAction() == HarvestAction.STRIP ? HarvestPart.BARK : HarvestPart.LEAVES;
    }

    /**
     * Rolls a single harvest of this tree's Spice: {@link Spice#getDropAmount()}
     * scaled by {@code IConfigHelper#getSpiceTreeHarvestYieldMultiplier()} and
     * the harvester's {@link HarvestLuck}, with a fractional result rounded up
     * at random (weighted by its fraction).
     *
     * @param random    The random source to roll with.
     * @param harvester The entity that harvested, if any; only a player's Luck counts.
     * @return The harvested stack; empty if the roll yields nothing.
     */
    public ItemStack rollHarvest(RandomSource random, @Nullable Entity harvester) {
        int count = HarvestLuck.rollYield(random, spice.getDropAmount(),
                Services.CONFIG.getSpiceTreeHarvestYieldMultiplier(), harvester);
        return count > 0 ? new ItemStack(productItem.get(), count) : ItemStack.EMPTY;
    }

    // #region getters

    /** @return The {@link Spice} this tree grows. */
    public Spice getSpice() {
        return spice;
    }

    /**
     * @return The key of the datapack-defined {@code minecraft:tree} configured
     *         feature that defines this tree's shape.
     */
    public ResourceKey<ConfiguredFeature<?, ?>> getTreeFeature() {
        return treeFeature;
    }

    /** @return The raw Spice item this tree yields. */
    public Supplier<Item> getProductItem() {
        return productItem;
    }

    /** @return The log block. */
    public Supplier<RotatedPillarBlock> getLog() {
        return log;
    }

    /** @return The stripped log block, see {@link StrippedSpiceLogBlock}. */
    public Supplier<RotatedPillarBlock> getStrippedLog() {
        return strippedLog;
    }

    /**
     * @return The leaves block - a {@link FruitingSpiceLeavesBlock} when
     *         {@link #getHarvestPart()} is {@link HarvestPart#LEAVES}, a plain
     *         {@link LeavesBlock} otherwise.
     */
    public Supplier<LeavesBlock> getLeaves() {
        return leaves;
    }

    /** @return The sapling block. */
    public Supplier<SpiceSaplingBlock> getSapling() {
        return sapling;
    }

    /** @return The sapling's block item. */
    public Supplier<Item> getSaplingItem() {
        return saplingItem;
    }

    /** @return Every block of this tree, e.g. for loot table datagen. */
    public List<Block> getBlocks() {
        return List.of(log.get(), strippedLog.get(), leaves.get(), sapling.get());
    }

    /** @return This tree's block items, in creative tab order. */
    public List<Item> getBlockItems() {
        return List.of(saplingItem.get(), logItem.get(), strippedLogItem.get(), leavesItem.get());
    }

    /** @return The log's registry path, e.g. {@code cinnamon_log}. */
    public String getLogId() {
        return spice.getId() + "_log";
    }

    /** @return The stripped log's registry path, e.g. {@code stripped_cinnamon_log}. */
    public String getStrippedLogId() {
        return "stripped_" + spice.getId() + "_log";
    }

    /** @return The leaves' registry path, e.g. {@code cinnamon_leaves}. */
    public String getLeavesId() {
        return spice.getId() + "_leaves";
    }

    /** @return The sapling's registry path, e.g. {@code cinnamon_sapling}. */
    public String getSaplingId() {
        return spice.getId() + "_sapling";
    }
}
