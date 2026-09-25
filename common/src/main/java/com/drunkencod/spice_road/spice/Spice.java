package com.drunkencod.spice_road.spice;

import java.util.Optional;

/**
 * One member per Spice, holding its identity, tier, and Spice Profile(s).
 * <p>
 * A Spice is not itself an item or a block - it is the product-level
 * abstraction the enum system owns. Kept lean, as only
 * fields consumed downstream are stored (e.g. no free-text
 * {@code harvestedPart} description).
 */
public enum Spice {

    LAVENDER(
            SourceType.FLOWER_PATCH, HarvestAction.PICK, Climate.ARID, 1, false,
            new SpiceProfile(-0.2, 0.0, -0.2, -0.9, -0.4, 0.0, 0.4, -0.8),
            null),

    CHILI_PEPPER(
            SourceType.CROP, HarvestAction.PICK, Climate.TEMPERATE, 2, false,
            new SpiceProfile(1.0, 0.1, 0.2, 0.2, -0.3, 0.4, -0.2, 0.3),
            null),

    CUMIN(
            SourceType.CROP, HarvestAction.BREAK, Climate.ARID, 2, false,
            new SpiceProfile(0.2, -0.3, -0.2, 0.9, 0.2, 0.3, 0.1, 0.6),
            null);

    private final SourceType sourceType;
    private final HarvestAction harvestAction;
    private final Climate climate;
    private final int harvestDifficulty;
    private final boolean requiresCuttingTool;
    private final SpiceProfile rawProfile;
    private final SpiceProfile driedProfile;

    Spice(SourceType sourceType, HarvestAction harvestAction, Climate climate, int harvestDifficulty,
            boolean requiresCuttingTool, SpiceProfile rawProfile, SpiceProfile driedProfile) {

        this.sourceType = sourceType;
        this.harvestAction = harvestAction;
        this.climate = climate;
        this.harvestDifficulty = harvestDifficulty;
        this.requiresCuttingTool = requiresCuttingTool;
        this.rawProfile = rawProfile;
        this.driedProfile = driedProfile;
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

    /**
     * @return The raw item's {@link SpiceProfile}. Always present.
     */
    public SpiceProfile getRawProfile() {

        return rawProfile;
    }

    /**
     * @return The dried item's {@link SpiceProfile}, if this Spice has a
     *         beneficial dried variant. Empty if not (case-by-case, TBD per
     *         spice).
     */
    public Optional<SpiceProfile> getDriedProfile() {

        return Optional.ofNullable(driedProfile);
    }
}
