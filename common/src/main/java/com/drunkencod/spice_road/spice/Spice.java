package com.drunkencod.spice_road.spice;

import java.util.Arrays;
import java.util.Collections;
import java.util.EnumSet;
import java.util.Optional;
import java.util.Set;

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
 * Flavor Axis data is <b>not</b> stored here - a Spice's raw item and any
 * {@link ProcessedSpice} items
 * get their {@link SpiceProfile} from the datapack-driven
 * {@code data/<namespace>/spice_profile/*.json} registry (see
 * {@link SpiceProfileRegistry}), keyed by item rather than by this enum, so
 * that any item - not just the ones this enum knows about - can be
 * registered as a spice.
 */
public enum Spice implements StringRepresentable {

    // trees
    ALLSPICE("allspice", SourceType.TREE, HarvestAction.PICK, Climate.TROPICAL, 2, false, false, 2, false,
            Season.SUMMER),
    CASSIA("cassia", SourceType.TREE, HarvestAction.STRIP, Climate.TROPICAL, 4, false, false, 2, false,
            Season.SPRING, Season.SUMMER),
    CINNAMON("cinnamon", SourceType.TREE, HarvestAction.STRIP, Climate.TROPICAL, 5, false, false, 1, false,
            Season.SPRING, Season.SUMMER),
    CLOVE("clove", SourceType.TREE, HarvestAction.SHEAR, Climate.TROPICAL, 5, true, false, 1, false,
            Season.SUMMER, Season.AUTUMN),
    CURRY("curry", SourceType.TREE, HarvestAction.SHEAR, Climate.TROPICAL, 2, true, false, 2, false,
            Season.SPRING, Season.SUMMER),
    MASTIC("mastic", SourceType.TREE, HarvestAction.STRIP, Climate.ARID, 4, false, false, 1, false,
            Season.SUMMER),
    NUTMEG("nutmeg", SourceType.TREE, HarvestAction.SHEAR, Climate.TROPICAL, 5, true, false, 1, false,
            Season.SUMMER, Season.AUTUMN),
    STAR_ANISE("star_anise", SourceType.TREE, HarvestAction.PICK, Climate.TEMPERATE, 4, false, false, 1, false,
            Season.SPRING, Season.AUTUMN),
    TAMARIND("tamarind", SourceType.TREE, HarvestAction.PICK, Climate.TROPICAL, 3, false, false, 2, false,
            Season.SUMMER, Season.AUTUMN),
    // bushes
    CAPER("caper", SourceType.BUSH, HarvestAction.PICK, Climate.ARID, 3, true, false, 2, false,
            Season.SUMMER),
    ROSEMARY("rosemary", SourceType.BUSH, HarvestAction.PICK, Climate.ARID, 1, true, false, 2, false,
            Season.WINTER, Season.SPRING, Season.SUMMER),
    SAGE("sage", SourceType.BUSH, HarvestAction.PICK, Climate.TEMPERATE, 1, true, false, 2, false,
            Season.SPRING, Season.SUMMER),
    SUMAC("sumac", SourceType.BUSH, HarvestAction.PICK, Climate.ARID, 2, true, false, 2, false,
            Season.SUMMER, Season.AUTUMN),
    THYME("thyme", SourceType.BUSH, HarvestAction.PICK, Climate.ARID, 1, true, false, 2, false,
            Season.SPRING, Season.SUMMER),
    // vines
    LONG_PEPPER("long_pepper", SourceType.VINE, HarvestAction.PICK, Climate.TROPICAL, 4, false, false, 1, false,
            Season.SUMMER, Season.AUTUMN),
    VANILLA("vanilla", SourceType.VINE, HarvestAction.PICK, Climate.TROPICAL, 5, true, true, 1, false,
            Season.SPRING, Season.SUMMER),
    // flower patches
    LAVENDER("lavender", SourceType.FLOWER_PATCH, HarvestAction.PICK, Climate.ARID, 1, false, false, 2, false,
            Season.SUMMER),
    NIGELLA("nigella", SourceType.FLOWER_PATCH, HarvestAction.BREAK, Climate.TEMPERATE, 2, false, false, 2, false,
            Season.SPRING, Season.SUMMER),
    SAFFLOWER("safflower", SourceType.FLOWER_PATCH, HarvestAction.BREAK, Climate.ARID, 2, false, false, 2, false,
            Season.SUMMER),
    SAFFRON("saffron", SourceType.FLOWER_PATCH, HarvestAction.PICK, Climate.ARID, 5, true, true, 1, false,
            Season.AUTUMN),
    // crops
    CARAWAY("caraway", SourceType.CROP, HarvestAction.BREAK, Climate.COLD, 1, false, false, 2, false,
            Season.SPRING, Season.AUTUMN),
    CORIANDER("coriander", SourceType.CROP, HarvestAction.BREAK, Climate.TEMPERATE, 1, false, false, 2, false,
            Season.SPRING, Season.AUTUMN),
    CUMIN("cumin", SourceType.CROP, HarvestAction.BREAK, Climate.ARID, 2, false, false, 1, false,
            Season.SPRING, Season.SUMMER),
    DILL("dill", SourceType.CROP, HarvestAction.BREAK, Climate.COLD, 1, false, false, 2, false,
            Season.SPRING, Season.SUMMER),
    FENNEL("fennel", SourceType.CROP, HarvestAction.BREAK, Climate.TEMPERATE, 1, false, false, 2, false,
            Season.SPRING, Season.SUMMER, Season.AUTUMN),
    FENUGREEK("fenugreek", SourceType.CROP, HarvestAction.BREAK, Climate.ARID, 2, false, false, 2, false,
            Season.SPRING, Season.AUTUMN),
    HABANERO("habanero", SourceType.CROP, HarvestAction.PICK, Climate.ARID, 3, false, false, 2, false,
            Season.SUMMER),
    MUSTARD("mustard", SourceType.CROP, HarvestAction.BREAK, Climate.COLD, 1, false, false, 2, false,
            Season.AUTUMN, Season.WINTER, Season.SPRING),
    // rhizomes
    CARDAMOM("cardamom", SourceType.RHIZOME, HarvestAction.BREAK, Climate.TROPICAL, 4, false, false, 1, false,
            Season.SPRING, Season.SUMMER),
    GINGER("ginger", SourceType.RHIZOME, HarvestAction.BREAK, Climate.TROPICAL, 3, false, false, 2, false,
            Season.SUMMER, Season.AUTUMN),
    HORSERADISH("horseradish", SourceType.RHIZOME, HarvestAction.BREAK, Climate.COLD, 2, false, false, 2, false,
            Season.AUTUMN, Season.WINTER),
    LICORICE("licorice", SourceType.RHIZOME, HarvestAction.BREAK, Climate.TEMPERATE, 3, false, false, 2, false,
            Season.SPRING, Season.SUMMER, Season.AUTUMN),
    SWEET_FLAG("sweet_flag", SourceType.RHIZOME, HarvestAction.BREAK, Climate.COLD, 2, false, false, 2, true,
            Season.SPRING, Season.AUTUMN, Season.WINTER),
    TURMERIC("turmeric", SourceType.RHIZOME, HarvestAction.BREAK, Climate.TROPICAL, 3, false, false, 2, false,
            Season.SUMMER, Season.AUTUMN),
    WASABI("wasabi", SourceType.RHIZOME, HarvestAction.BREAK, Climate.COLD, 5, true, true, 1, true,
            Season.AUTUMN, Season.WINTER, Season.SPRING),
    WATER_PEPPER("water_pepper", SourceType.RHIZOME, HarvestAction.BREAK, Climate.TEMPERATE, 3, false, false, 2, true,
            Season.SPRING, Season.SUMMER, Season.AUTUMN);

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
    private final boolean aquatic;
    private final Set<Season> seasons;

    Spice(String id, SourceType src, HarvestAction act, Climate climate, int rarity,
            boolean reqTool, boolean handPick, int drops, boolean aquatic, Season... seasons) {
        this.id = id;
        this.sourceType = src;
        this.harvestAction = act;
        this.climate = climate;
        this.harvestDifficulty = rarity;
        this.requiresHarvestTool = reqTool;
        this.requiresHandPick = handPick;
        this.dropAmount = drops;
        this.aquatic = aquatic;
        this.seasons = Collections.unmodifiableSet(EnumSet.copyOf(Arrays.asList(seasons)));
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

    /**
     * @return Whether this is an Aquatic Spice, whose plant lives waterlogged
     *         on aquatic-growable ground instead of on farmland. Only
     *         honored for {@link SourceType#RHIZOME} Spices.
     */
    public boolean isAquatic() {
        return aquatic && sourceType == SourceType.RHIZOME;
    }

    /**
     * @return The seasons this Spice grows in, for season mods. Never empty.
     */
    public Set<Season> getSeasons() {
        return seasons;
    }

    /**
     * @return Whether a harvest of this Spice's mature plant yields its
     *         planting item at the same rate as the Spice itself - true for
     *         {@link SourceType#RHIZOME} Spices, whose cuttings are sliced off
     *         together with the harvested part.
     */
    public boolean harvestYieldsPlantingItem() {
        return sourceType == SourceType.RHIZOME;
    }

    /**
     * @return The registry path suffix of this Spice's planting item:
     *         {@code "_cuttings"} for {@link SourceType#RHIZOME} Spices,
     *         {@code "_seeds"} otherwise.
     */
    public String getPlantingItemSuffix() {
        return sourceType == SourceType.RHIZOME ? "_cuttings" : "_seeds";
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

    /**
     * Returns the raw spice item with the given enum ID from the registry, if it
     * exists.
     */
    public static @Nullable Item getRawById(String id) {
        return ModItems.byPath(id);
    }

    /**
     * Returns the planting item (seeds, or cuttings for rhizomes - see
     * {@link #getPlantingItemSuffix()}) of the spice with the given enum ID
     * from the registry, if it exists.
     */
    public static @Nullable Item getSeedsById(String id) {
        Spice spice = byId(id);
        return ModItems.byPath(id + (spice != null ? spice.getPlantingItemSuffix() : "_seeds"));
    }

    // #endregion
}
