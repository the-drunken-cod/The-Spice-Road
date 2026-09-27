package com.drunkencod.spice_road.spice;

import java.util.Optional;

import org.jetbrains.annotations.Nullable;

import com.drunkencod.spice_road.platform.Services;
import com.drunkencod.spice_road.registry.ModItems;
import com.drunkencod.spice_road.spice.region.SpiceRegionResolver;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.Item;

/**
 * One member per Spice, holding its identity, growth/harvest metadata, and
 * tier.
 * <p>
 * A Spice is not itself an item or a block - it is the product-level
 * abstraction the enum system owns. Kept lean, as only
 * fields consumed downstream are stored (e.g. no free-text
 * {@code harvestedPart} description).
 * <p>
 * Flavor Axis data is <b>not</b> stored here - a Spice's raw/dried item(s)
 * get their {@link SpiceProfile} from the datapack-driven
 * {@code data/<namespace>/spice_profile/*.json} registry (see
 * {@link SpiceProfileRegistry}), keyed by item rather than by this enum, so
 * that any item - not just the ones this enum knows about - can be
 * registered as a spice.
 */
public enum Spice {

    LAVENDER("lavender", SourceType.FLOWER_PATCH, HarvestAction.PICK, Climate.ARID, 1, false, 2),
    CHILI_PEPPER("chili_pepper", SourceType.CROP, HarvestAction.PICK, Climate.TEMPERATE, 2, false, 2),
    CUMIN("cumin", SourceType.CROP, HarvestAction.BREAK, Climate.ARID, 5, false, 2),
    CINNAMON("cinnamon", SourceType.TREE, HarvestAction.STRIP, Climate.TROPICAL, 4, false, 2);

    private final String id;
    private final SourceType sourceType;
    private final HarvestAction harvestAction;
    private final Climate climate;
    private final int harvestDifficulty;
    private final boolean requiresCuttingTool;
    private final int dropAmount;

    Spice(String id, SourceType sourceType, HarvestAction harvestAction, Climate climate, int harvestDifficulty,
            boolean requiresCuttingTool, int dropAmount) {
        this.id = id;
        this.sourceType = sourceType;
        this.harvestAction = harvestAction;
        this.climate = climate;
        this.harvestDifficulty = harvestDifficulty;
        this.requiresCuttingTool = requiresCuttingTool;
        this.dropAmount = dropAmount;
    }

    /**
     * @return This Spice's ID, used as the registry path of its product item
     *         and as the base of its other blocks' and items' IDs.
     */
    public String getId() {
        return id;
    }

    /** @return How this Spice grows. */
    public SourceType getSourceType() {
        return sourceType;
    }

    /** @return How this Spice is harvested once mature. */
    public HarvestAction getHarvestAction() {
        return harvestAction;
    }

    /** @return The {@link Climate} this Spice naturally occurs in. */
    public Climate getClimate() {
        return climate;
    }

    /**
     * @return The 1-5 Harvest Difficulty value of this Spice.
     */
    public int getHarvestDifficulty() {
        return harvestDifficulty;
    }

    /**
     * @return This Spice's {@link Tier}, derived from its harvest difficulty
     *         via {@link Tier#fromHarvestDifficulty(int)} so the two can
     *         never drift out of sync.
     */
    public Tier getTier() {
        return Tier.fromHarvestDifficulty(harvestDifficulty);
    }

    /**
     * @return Whether harvesting this Spice requires an item from the
     *         {@code spice_road:cutting_tools} tag.
     */
    public boolean requiresCuttingTool() {
        return requiresCuttingTool;
    }

    /**
     * @return The base number of product items per harvest, before any
     *         configured yield multiplier.
     */
    public int getDropAmount() {
        return dropAmount;
    }

    /**
     * Checks whether this Spice may grow (or bear fruit) at the given
     * position, based on Spice Region support:
     * <ol>
     * <li>Hardy Spices (harvest difficulty at or below
     * {@code IConfigHelper#getSpiceHardyHarvestDifficulty()}) can be
     * cultivated anywhere.</li>
     * <li>Otherwise, if {@code IConfigHelper#isSpiceRegionPlantingRestricted()}
     * is enabled, the position's Spice Region must resolve to this Spice.</li>
     * <li>If the restriction is disabled, cultivation is never gated.</li>
     * </ol>
     *
     * @param level The server level.
     * @param pos   The position to check.
     * @return Whether this Spice is allowed to grow at {@code pos}.
     */
    public boolean canBeCultivatedAt(ServerLevel level, BlockPos pos) {
        if (harvestDifficulty <= Services.CONFIG.getSpiceHardyHarvestDifficulty())
            return true;

        if (!Services.CONFIG.isSpiceRegionPlantingRestricted())
            return true;

        double cellScale = Services.CONFIG.getSpiceRegionCellScale();
        double clusteringStrength = Services.CONFIG.getSpiceRegionClusteringStrength();
        Climate climate = Climate.fromBiome(level.getBiome(pos), pos);

        Optional<Spice> resolved = SpiceRegionResolver
                .resolve(level.getSeed(), Services.CONFIG.getSpiceRegionSalt(), cellScale, clusteringStrength, climate, pos.getX(), pos.getZ())
                .spice();
        return resolved.isPresent() && resolved.get() == this;
    }

    // #region static

    /** Returns the raw spice item with the given enum ID from the registry. */
    public static @Nullable Item getRawById(String id) {
        return ModItems.byPath(id);
    }

    /** Returns the dried spice item with the given enum ID from the registry. */
    public static @Nullable Item getDriedById(String id) {
        return ModItems.byPath("dried_" + id);
    }

    /** Returns the spice seeds item with the given enum ID from the registry. */
    public static @Nullable Item getSeedsById(String id) {
        return ModItems.byPath(id + "_seeds");
    }

    // #endregion
}
