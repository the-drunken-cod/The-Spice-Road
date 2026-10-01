package com.drunkencod.spice_road.debug;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;

import com.drunkencod.spice_road.platform.Services;
import com.drunkencod.spice_road.spice.Climate;
import com.drunkencod.spice_road.spice.region.SpiceRegionResolver;
import com.drunkencod.spice_road.spice.region.SpiceRegionResult;
import com.drunkencod.spice_road.spice.region.SublevelPositions;

/**
 * Real {@link SpiceRegionDebugSource}, backed directly by
 * {@link SpiceRegionResolver} - no separate/duplicated logic, so what you
 * see on F3 is exactly what {@code SpicePlantFeature} would resolve at that
 * spot.
 * <p>
 * Only ever meaningful in singleplayer. The true world seed is server-only
 * state a remote multiplayer client never receives (by design - the same
 * reason vanilla's own F3 seed display only ever resolves via the
 * integrated server).
 * <p>
 * References {@code net.minecraft.client.Minecraft} directly from a
 * common-module class. Safe for the same reason
 * {@code com.drunkencod.spice_road.tooltip.SpiceProfileTooltips}'s
 * registered content ultimately reaches a {@code Screen}-referencing
 * per-loader tooltip handler: this class is only ever constructed and
 * queried by each loader's client-only F3 hook, never on a dedicated
 * server.
 */
public class SpiceRegionDebugSourceImpl implements SpiceRegionDebugSource {

    @Override
    public SpiceRegionDebugInfo getDebugInfo(Level level, BlockPos pos) {
        Long worldSeed = resolveWorldSeed(level);
        if (worldSeed == null) {
            return new SpiceRegionDebugInfo(List.of(
                    new SpiceRegionDebugInfo.Line("Spice Region", "unavailable (not singleplayer)")));
        }

        BlockPos effectivePos = SublevelPositions.projectOutOfSubLevel(level, pos);
        Holder<Biome> biome = level.getBiome(effectivePos);
        Climate climate = Climate.fromBiome(biome, effectivePos);

        long salt = Services.CONFIG.getSpiceRegionSalt();
        double cellScale = Services.CONFIG.getSpiceRegionCellScale();
        double clusteringStrength = Services.CONFIG.getSpiceRegionClusteringStrength();

        SpiceRegionResult result = SpiceRegionResolver.resolve(worldSeed, salt, cellScale, clusteringStrength, climate,
                effectivePos.getX(), effectivePos.getZ());

        List<SpiceRegionDebugInfo.Line> lines = new ArrayList<>();
        if (!effectivePos.equals(pos)) {
            lines.add(new SpiceRegionDebugInfo.Line("Sub-level",
                    "projected to " + effectivePos.getX() + ", " + effectivePos.getY() + ", " + effectivePos.getZ()));
        }
        lines.add(new SpiceRegionDebugInfo.Line("Climate", climate.name()));
        lines.add(new SpiceRegionDebugInfo.Line("Cell",
                result.cell().gridX() + ", " + result.cell().gridZ() + " (seed " + result.cell().seed() + ")"));
        double dx = result.cell().centerX() - effectivePos.getX();
        double dz = result.cell().centerZ() - effectivePos.getZ();
        lines.add(new SpiceRegionDebugInfo.Line("Cell center",
                Math.round(result.cell().centerX()) + ", " + Math.round(result.cell().centerZ())
                        + " (" + describeOffset(dx, dz) + ")"));
        lines.add(new SpiceRegionDebugInfo.Line("Resolved Spice",
                result.spice().map(Enum::name)
                        .orElse(result.spiceless() ? "NONE (spiceless region)" : "NONE (empty Climate Bucket)")));
        return new SpiceRegionDebugInfo(lines);
    }

    /**
     * Formats the offset to a target as compass directions plus direct distance,
     * e.g. {@code north 14.5, west 13.1, dist 19.54}.
     *
     * @param dx Block offset on the X axis (positive is east).
     * @param dz Block offset on the Z axis (positive is south).
     * @return The formatted offset description.
     */
    private static String describeOffset(double dx, double dz) {
        return String.format(Locale.ROOT, "%s %.1f, %s %.1f, dist %.2f",
                dz <= 0 ? "north" : "south", Math.abs(dz),
                dx >= 0 ? "east" : "west", Math.abs(dx),
                Math.sqrt(dx * dx + dz * dz));
    }

    private static Long resolveWorldSeed(Level level) {
        MinecraftServer server = Minecraft.getInstance().getSingleplayerServer();
        if (server == null) {
            return null;
        }
        ServerLevel serverLevel = server.getLevel(level.dimension());
        return serverLevel != null ? serverLevel.getSeed() : null;
    }
}
