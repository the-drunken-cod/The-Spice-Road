package com.drunkencod.spice_road.spice;

import java.util.Optional;

import org.jetbrains.annotations.Nullable;

import com.drunkencod.spice_road.item.SpiceItemTags;
import com.drunkencod.spice_road.platform.Services;
import com.drunkencod.spice_road.registry.ModItems;
import com.drunkencod.spice_road.spice.region.SpiceRegionResolver;
import com.drunkencod.spice_road.spice.region.SublevelPositions;

import com.mojang.serialization.Codec;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.TagKey;
import net.minecraft.util.StringRepresentable;
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
public enum Spice implements StringRepresentable {

    // crops
    LAVENDER("lavender", SourceType.FLOWER_PATCH, HarvestAction.PICK, Climate.ARID, 1, false, false, 2),
    SAFFRON("saffron", SourceType.FLOWER_PATCH, HarvestAction.PICK, Climate.ARID, 5, true, true, 1),
    CHILI_PEPPER("chili_pepper", SourceType.CROP, HarvestAction.PICK, Climate.TEMPERATE, 2, false, false, 2),
    CUMIN("cumin", SourceType.CROP, HarvestAction.BREAK, Climate.ARID, 2, false, false, 1),
    // trees
    CINNAMON("cinnamon", SourceType.TREE, HarvestAction.STRIP, Climate.TROPICAL, 4, false, false, 1),
    NUTMEG("nutmeg", SourceType.TREE, HarvestAction.SHEAR, Climate.TROPICAL, 5, true, false, 1),
    // vines
    VANILLA("vanilla", SourceType.VINE, HarvestAction.PICK, Climate.TROPICAL, 5, true, true, 1);

    /**
     * Codec reading and writing a {@link Spice} by its {@link #getId() ID}, e.g.
     * {@code "cinnamon"}.
     */
    public static final Codec<Spice> CODEC = StringRepresentable.fromEnum(Spice::values);

    private final String id;
    private final SourceType sourceType;
    private final HarvestAction harvestAction;
    private final Climate climate;
    private final int harvestDifficulty;
    private final boolean requiresHarvestTool;
    private final boolean requiresHandPick;
    private final int dropAmount;

    Spice(String id, SourceType sourceType, HarvestAction harvestAction, Climate climate, int harvestDifficulty,
            boolean requiresHarvestTool, boolean requiresHandPick, int dropAmount) {
        this.id = id;
        this.sourceType = sourceType;
        this.harvestAction = harvestAction;
        this.climate = climate;
        this.harvestDifficulty = harvestDifficulty;
        this.requiresHarvestTool = requiresHarvestTool;
        this.requiresHandPick = requiresHandPick;
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
     * @return Whether harvesting this Spice requires an item from its
     *         {@link #getHarvestToolTag() harvest tool tag} (its Harvest Tool
     *         Requirement). Always true for {@link HarvestAction#SHEAR}, which
     *         can't be performed without a tool by definition, so the two can
     *         never drift out of sync.
     */
    public boolean requiresHarvestTool() {
        return requiresHarvestTool || harvestAction == HarvestAction.SHEAR;
    }

    /**
     * @return The tag of items satisfying this Spice's Harvest Tool
     *         Requirement: {@link SpiceItemTags#PICKING_TOOLS} for
     *         {@link HarvestAction#PICK}, otherwise
     *         {@link SpiceItemTags#CUTTING_TOOLS}.
     */
    public TagKey<Item> getHarvestToolTag() {
        return harvestAction == HarvestAction.PICK ? SpiceItemTags.PICKING_TOOLS : SpiceItemTags.CUTTING_TOOLS;
    }

    /**
     * @return Whether this Spice may only be harvested by a connected player
     *         performing its Harvest Action directly (its Hand-Pick
     *         Requirement). Breaking such a Spice's plant never drops it.
     */
    public boolean requiresHandPick() {
        return requiresHandPick;
    }

    /**
     * @return The base number of product items per harvest, before any
     *         configured yield multiplier.
     */
    public int getDropAmount() {
        return dropAmount;
    }

    @Override
    public String getSerializedName() {
        return id;
    }

    /**
     * @return The translation key of this Spice's plain name (e.g.
     *         {@code "Cinnamon"}), independent of any item state.
     */
    public String getTranslationKey() {
        return "spice.spice_road." + id;
    }

    /**
     * @return This Spice's translatable plain name. See
     *         {@link #getTranslationKey()}.
     */
    public Component getDisplayName() {
        return Component.translatable(getTranslationKey());
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
     * @param pos   The position to check. Projected out of a Sable
     *              sub-level first (see {@link SublevelPositions}), so a
     *              Spice Plant built on a moving contraption is judged
     *              against whatever region it's currently over.
     * @return Whether this Spice is allowed to grow at {@code pos}.
     */
    public boolean canBeCultivatedAt(ServerLevel level, BlockPos pos) {
        if (harvestDifficulty <= Services.CONFIG.getSpiceHardyHarvestDifficulty())
            return true;

        if (!Services.CONFIG.isSpiceRegionPlantingRestricted())
            return true;

        BlockPos effectivePos = SublevelPositions.projectOutOfSubLevel(level, pos);

        double cellScale = Services.CONFIG.getSpiceRegionCellScale();
        double clusteringStrength = Services.CONFIG.getSpiceRegionClusteringStrength();
        Climate climate = Climate.fromBiome(level.getBiome(effectivePos), effectivePos);

        Optional<Spice> resolved = SpiceRegionResolver
                .resolve(level.getSeed(), Services.CONFIG.getSpiceRegionSalt(), cellScale, clusteringStrength, climate,
                        effectivePos.getX(), effectivePos.getZ())
                .spice();
        return resolved.isPresent() && resolved.get() == this;
    }

    // #region static

    /**
     * Looks up a Spice by its {@link #getId() ID}.
     *
     * @param id The Spice's ID, e.g. {@code "cinnamon"}.
     * @return The matching Spice, or {@code null} if there is none.
     */
    public static @Nullable Spice byId(String id) {
        for (Spice spice : values()) {
            if (spice.id.equals(id))
                return spice;
        }
        return null;
    }

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
