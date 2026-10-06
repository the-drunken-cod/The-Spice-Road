package com.drunkencod.spice_road.grinder;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;

import com.drunkencod.spice_road.spice.FlavorAxis;
import com.drunkencod.spice_road.spice.board.BoardGeometry;
import com.drunkencod.spice_road.spice.board.CellView;
import com.drunkencod.spice_road.spice.board.Direction;
import com.drunkencod.spice_road.spice.effect.SeasoningEffect;

/**
 * Everything the Spice Grinder screen shows, as the server lets one player see
 * it. It is sent whole after every accepted action, so a lost or reordered
 * packet can't desync the screen, and it never holds hidden board information.
 *
 * @param phase       Whether a draft or a run is shown.
 * @param food        The food stack: the one in the food slot while drafting,
 *                    the one being seasoned during a run.
 * @param spices      The spices and Spice Mixes per food: the draft's while
 *                    drafting, the consumed ones (as loose spices) during a run.
 * @param points      The points per axis in {@link FlavorAxis} order: the
 *                    preview of the draft, or what is left in a run.
 * @param poles       The pole per axis in {@link FlavorAxis} order.
 * @param x           The pawn's column, during a run.
 * @param y           The pawn's row, during a run.
 * @param cells       What the player sees of each cell, row-major, during a
 *                    run; empty while drafting.
 * @param effects     The effects gathered so far.
 * @param stepsLeft   For each {@link Direction}, how many steps that way the
 *                    points still pay for, {@code 0} if one can't be taken.
 * @param canLockIn   Whether the cell the pawn stands on can be locked in.
 * @param canSeason   Whether the draft can be started.
 * @param event       What the last action did, for a sound and a message.
 * @param maxPerKind  The most spices of one kind that count.
 * @param maxTotal    The most spices of all kinds that count.
 * @param available   How many of each loose spice and kind of Spice Mix the
 *                    player's inventory and the nearby spice storage hold
 *                    together.
 * @param stepCost    Points one step costs, during a run.
 * @param lockInCost  Points locking in the cell the pawn stands on costs, or
 *                    {@code 0} if it isn't an effect cell.
 */
public record GrinderView(Phase phase, ItemStack food, Map<GrinderSpice, Integer> spices, List<Double> points,
        List<Integer> poles, int x, int y, List<CellView> cells, List<SeasoningEffect> effects,
        List<Integer> stepsLeft, boolean canLockIn, boolean canSeason, Event event, int maxPerKind, int maxTotal,
        Map<GrinderSpice, Integer> available, double stepCost, double lockInCost) {

    /** The view of a Grinder that hasn't been told anything yet. */
    public static final GrinderView EMPTY = new GrinderView(Phase.DRAFT, ItemStack.EMPTY, Map.of(),
            java.util.Collections.nCopies(FlavorAxis.values().length, 0D),
            java.util.Collections.nCopies(FlavorAxis.values().length, 0), 0, 0, List.of(), List.of(),
            java.util.Collections.nCopies(Direction.values().length, 0), false, false, Event.NONE, 3, 16, Map.of(), 0D, 0D);

    /** Network codec. */
    public static final StreamCodec<RegistryFriendlyByteBuf, GrinderView> STREAM_CODEC = StreamCodec.of(
            GrinderView::write, GrinderView::read);

    /** What a Grinder screen is showing. */
    public enum Phase {
        /** Choosing the food and the spices; nothing is consumed yet. */
        DRAFT,
        /** Playing a run on the Seasoning Board. */
        RUNNING
    }

    /** What the last action did. */
    public enum Event {
        /** Nothing worth announcing, e.g. an ordinary step. */
        NONE,
        /** A cell was locked in. */
        LOCKED_IN,
        /** The pawn stepped on a mine and its bane was added. */
        MINE_HIT,
        /** The pawn stepped on a mine in a zone without a pole, which did nothing. */
        MINE_DUD,
        /** The action wasn't possible. */
        REFUSED,
        /** Accepting would waste the spices; a second click confirms. */
        CONFIRM_REQUIRED
    }

    private static void write(RegistryFriendlyByteBuf buf, GrinderView view) {
        buf.writeEnum(view.phase);
        ItemStack.OPTIONAL_STREAM_CODEC.encode(buf, view.food);
        buf.writeVarInt(view.spices.size());
        view.spices.forEach((spice, count) -> {
            GrinderSpice.STREAM_CODEC.encode(buf, spice);
            buf.writeVarInt(count);
        });
        view.points.forEach(buf::writeDouble);
        view.poles.forEach(pole -> buf.writeByte(pole));
        buf.writeByte(view.x);
        buf.writeByte(view.y);
        buf.writeVarInt(view.cells.size());
        view.cells.forEach(cell -> writeCell(buf, cell));
        buf.writeVarInt(view.effects.size());
        view.effects.forEach(effect -> SeasoningEffect.STREAM_CODEC.encode(buf, effect));
        view.stepsLeft.forEach(buf::writeVarInt);
        buf.writeBoolean(view.canLockIn);
        buf.writeBoolean(view.canSeason);
        buf.writeEnum(view.event);
        buf.writeVarInt(view.maxPerKind);
        buf.writeVarInt(view.maxTotal);
        buf.writeDouble(view.stepCost);
        buf.writeDouble(view.lockInCost);
        buf.writeVarInt(view.available.size());
        view.available.forEach((spice, count) -> {
            GrinderSpice.STREAM_CODEC.encode(buf, spice);
            buf.writeVarInt(count);
        });
    }

    private static GrinderView read(RegistryFriendlyByteBuf buf) {
        Phase phase = buf.readEnum(Phase.class);
        ItemStack food = ItemStack.OPTIONAL_STREAM_CODEC.decode(buf);
        Map<GrinderSpice, Integer> spices = new LinkedHashMap<>();
        for (int i = buf.readVarInt(); i > 0; i--)
            spices.put(GrinderSpice.STREAM_CODEC.decode(buf), buf.readVarInt());
        List<Double> points = new ArrayList<>();
        for (int i = 0; i < FlavorAxis.values().length; i++)
            points.add(buf.readDouble());
        List<Integer> poles = new ArrayList<>();
        for (int i = 0; i < FlavorAxis.values().length; i++)
            poles.add((int) buf.readByte());
        int x = buf.readByte();
        int y = buf.readByte();
        List<CellView> cells = new ArrayList<>();
        for (int i = buf.readVarInt(); i > 0; i--)
            cells.add(readCell(buf));
        List<SeasoningEffect> effects = new ArrayList<>();
        for (int i = buf.readVarInt(); i > 0; i--)
            effects.add(SeasoningEffect.STREAM_CODEC.decode(buf));
        List<Integer> stepsLeft = new ArrayList<>();
        for (int i = 0; i < Direction.values().length; i++)
            stepsLeft.add(buf.readVarInt());
        boolean canLockIn = buf.readBoolean();
        boolean canSeason = buf.readBoolean();
        Event event = buf.readEnum(Event.class);
        int maxPerKind = buf.readVarInt();
        int maxTotal = buf.readVarInt();
        double stepCost = buf.readDouble();
        double lockInCost = buf.readDouble();
        Map<GrinderSpice, Integer> available = new LinkedHashMap<>();
        for (int i = buf.readVarInt(); i > 0; i--)
            available.put(GrinderSpice.STREAM_CODEC.decode(buf), buf.readVarInt());
        return new GrinderView(phase, food, spices, points, poles, x, y, cells, effects, stepsLeft, canLockIn,
                canSeason, event, maxPerKind, maxTotal, available, stepCost, lockInCost);
    }

    private static void writeCell(RegistryFriendlyByteBuf buf, CellView cell) {
        buf.writeEnum(cell.kind());
        buf.writeVarInt(cell.effectCount());
        buf.writeVarInt(cell.boons() + 1);
        buf.writeVarInt(cell.banes() + 1);
        buf.writeVarInt(cell.randoms() + 1);
        buf.writeVarInt(cell.effects().size());
        cell.effects().forEach(effect -> SeasoningEffect.STREAM_CODEC.encode(buf, effect));
        buf.writeBoolean(cell.lockedIn());
    }

    private static CellView readCell(RegistryFriendlyByteBuf buf) {
        CellView.Kind kind = buf.readEnum(CellView.Kind.class);
        int effectCount = buf.readVarInt();
        int boons = buf.readVarInt() - 1;
        int banes = buf.readVarInt() - 1;
        int randoms = buf.readVarInt() - 1;
        List<SeasoningEffect> effects = new ArrayList<>();
        for (int i = buf.readVarInt(); i > 0; i--)
            effects.add(SeasoningEffect.STREAM_CODEC.decode(buf));
        return new CellView(kind, effectCount, boons, banes, randoms, effects, buf.readBoolean());
    }

    /**
     * @param index A cell index.
     * @return The cell at {@code index}, or {@link CellView#BLANK} without a run.
     */
    public CellView cell(int index) {
        return index >= 0 && index < cells.size() ? cells.get(index) : CellView.BLANK;
    }

    /**
     * @param cellX A column.
     * @param cellY A row.
     * @return The cell at {@code (cellX, cellY)}.
     */
    public CellView cell(int cellX, int cellY) {
        return cell(BoardGeometry.index(cellX, cellY));
    }
}
