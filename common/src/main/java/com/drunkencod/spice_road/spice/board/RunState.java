package com.drunkencod.spice_road.spice.board;

import java.util.List;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import com.drunkencod.spice_road.spice.FlavorAxis;
import com.drunkencod.spice_road.spice.effect.SeasoningEffect;

/**
 * Everything of a {@link SeasoningRun} that changes while it is played and has
 * to survive being saved: the pawn, the poles fixed at the start, the points
 * spent, the cells locked in or detonated and the effects gathered.
 *
 * @param x         The pawn's column.
 * @param y         The pawn's row.
 * @param poles     The pole of every axis in {@link FlavorAxis} order, fixed at the start.
 * @param spent     The points spent on every axis in {@link FlavorAxis} order.
 * @param lockedIn  Indices of the effect cells locked in.
 * @param detonated Indices of the mines that already went off.
 * @param effects   The effects gathered so far.
 */
public record RunState(int x, int y, List<Integer> poles, List<Double> spent, List<Integer> lockedIn,
        List<Integer> detonated, List<SeasoningEffect> effects) {

    /** Persistent (NBT) codec. */
    public static final Codec<RunState> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.intRange(0, BoardGeometry.SIZE - 1).fieldOf("x").forGetter(RunState::x),
            Codec.intRange(0, BoardGeometry.SIZE - 1).fieldOf("y").forGetter(RunState::y),
            Codec.INT.listOf().fieldOf("poles").forGetter(RunState::poles),
            Codec.DOUBLE.listOf().fieldOf("spent").forGetter(RunState::spent),
            Codec.INT.listOf().fieldOf("locked_in").forGetter(RunState::lockedIn),
            Codec.INT.listOf().fieldOf("detonated").forGetter(RunState::detonated),
            SeasoningEffect.CODEC.listOf().fieldOf("effects").forGetter(RunState::effects))
            .apply(instance, RunState::new));

    /**
     * @param x         The pawn's column.
     * @param y         The pawn's row.
     * @param poles     One pole per axis.
     * @param spent     One spent amount per axis.
     * @param lockedIn  Indices of the effect cells locked in.
     * @param detonated Indices of the mines that went off.
     * @param effects   The effects gathered.
     */
    public RunState {
        if (poles.size() != FlavorAxis.values().length || spent.size() != FlavorAxis.values().length)
            throw new IllegalArgumentException("A run state needs one pole and one spent amount per axis");
        poles = List.copyOf(poles);
        spent = List.copyOf(spent);
        lockedIn = List.copyOf(lockedIn);
        detonated = List.copyOf(detonated);
        effects = List.copyOf(effects);
    }
}
