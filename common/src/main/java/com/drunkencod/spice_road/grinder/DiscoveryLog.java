package com.drunkencod.spice_road.grinder;

import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

/**
 * The effect cells one player has locked in, remembered so that they show
 * their content on later runs on the same board. A board is identified by its
 * seed (which already depends on the world, the salt and the food item) and a
 * fingerprint of its layout, so a salt or layout change simply stops matching
 * old entries. The log is bounded: past the limit the oldest entry is
 * forgotten, and locking in a cell again makes its entry the newest.
 */
public final class DiscoveryLog {

    private final LinkedHashSet<Entry> entries = new LinkedHashSet<>();

    /**
     * One locked-in cell.
     *
     * @param boardSeed  The seed of the board.
     * @param layoutHash A fingerprint of the layout the board was built to.
     * @param cell       The index of the cell on the board.
     */
    public record Entry(long boardSeed, int layoutHash, int cell) {

        /** Persistent (NBT) codec. */
        public static final Codec<Entry> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                Codec.LONG.fieldOf("board_seed").forGetter(Entry::boardSeed),
                Codec.INT.fieldOf("layout").forGetter(Entry::layoutHash),
                Codec.INT.fieldOf("cell").forGetter(Entry::cell))
                .apply(instance, Entry::new));
    }

    /**
     * @param saved Entries in the order they were recorded, oldest first.
     * @return A log holding them.
     */
    public static DiscoveryLog of(List<Entry> saved) {
        DiscoveryLog log = new DiscoveryLog();
        log.entries.addAll(saved);
        return log;
    }

    /**
     * Remembers a locked-in cell as the newest entry.
     *
     * @param entry The cell.
     * @param limit The most entries to keep; {@code 0} or less keeps none.
     * @return Whether the log didn't know the cell yet.
     */
    public boolean record(Entry entry, int limit) {
        boolean fresh = !entries.remove(entry);
        if (limit > 0)
            entries.add(entry);
        trim(limit);
        return fresh;
    }

    /**
     * Forgets the oldest entries beyond a limit, e.g. after it was lowered.
     *
     * @param limit The most entries to keep.
     */
    public void trim(int limit) {
        Iterator<Entry> oldest = entries.iterator();
        while (entries.size() > Math.max(0, limit) && oldest.hasNext()) {
            oldest.next();
            oldest.remove();
        }
    }

    /**
     * @param boardSeed  The seed of a board.
     * @param layoutHash A fingerprint of its layout.
     * @return The indices of the cells of that board the player knows.
     */
    public Set<Integer> revealed(long boardSeed, int layoutHash) {
        Set<Integer> cells = new HashSet<>();
        for (Entry entry : entries) {
            if (entry.boardSeed() == boardSeed && entry.layoutHash() == layoutHash)
                cells.add(entry.cell());
        }
        return cells;
    }

    /** @return The entries, oldest first. */
    public List<Entry> entries() {
        return List.copyOf(entries);
    }

    /** @return Whether nothing is remembered. */
    public boolean isEmpty() {
        return entries.isEmpty();
    }
}
