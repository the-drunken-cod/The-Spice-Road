package com.drunkencod.spice_road.spice;

import org.jetbrains.annotations.Nullable;

import com.drunkencod.spice_road.registry.ModItems;

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

    LAVENDER("lavender", SourceType.FLOWER_PATCH, HarvestAction.PICK, Climate.ARID, 1, false),
    CHILI_PEPPER("chili_pepper", SourceType.CROP, HarvestAction.PICK, Climate.TEMPERATE, 2, false),
    CUMIN("cumin", SourceType.CROP, HarvestAction.BREAK, Climate.ARID, 2, false);

    private final String id;
    private final SourceType sourceType;
    private final HarvestAction harvestAction;
    private final Climate climate;
    private final int harvestDifficulty;
    private final boolean requiresCuttingTool;

    Spice(String id, SourceType sourceType, HarvestAction harvestAction, Climate climate, int harvestDifficulty,
            boolean requiresCuttingTool) {

        this.id = id;
        this.sourceType = sourceType;
        this.harvestAction = harvestAction;
        this.climate = climate;
        this.harvestDifficulty = harvestDifficulty;
        this.requiresCuttingTool = requiresCuttingTool;
    }

    public String getId() {
        return id;
    }

    public SourceType getSourceType() {

        return sourceType;
    }

    public HarvestAction getHarvestAction() {

        return harvestAction;
    }

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

    public boolean requiresCuttingTool() {

        return requiresCuttingTool;
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
