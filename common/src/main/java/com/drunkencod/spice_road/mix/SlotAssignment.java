package com.drunkencod.spice_road.mix;

import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.List;

/**
 * The pooled matching of {@link MixPreset}: placing counted spices into slots
 * that each accept only some of them.
 */
final class SlotAssignment {

    private SlotAssignment() {
    }

    /**
     * Max-flow (Edmonds-Karp) from the spices through the slots they fit.
     *
     * @param supply How many of each spice there is.
     * @param demand How many each slot takes.
     * @param fits   Whether spice {@code i} may go into slot {@code s}.
     * @return Whether every spice can be placed with every slot filled exactly,
     *         which also needs the supply to equal the demand in total.
     */
    static boolean exact(int[] supply, int[] demand, boolean[][] fits) {
        int items = supply.length;
        int source = items + demand.length;
        int sink = source + 1;
        int[][] capacity = new int[sink + 1][sink + 1];
        int supplied = 0;
        for (int i = 0; i < items; i++) {
            capacity[source][i] = supply[i];
            supplied += supply[i];
            for (int s = 0; s < demand.length; s++) {
                if (fits[i][s])
                    capacity[i][items + s] = supply[i];
            }
        }
        int demanded = 0;
        for (int s = 0; s < demand.length; s++) {
            capacity[items + s][sink] = demand[s];
            demanded += demand[s];
        }
        if (supplied != demanded)
            return false;

        int flow = 0;
        while (true) {
            int[] previous = new int[sink + 1];
            Arrays.fill(previous, -1);
            previous[source] = source;
            ArrayDeque<Integer> queue = new ArrayDeque<>(List.of(source));
            while (!queue.isEmpty() && previous[sink] < 0) {
                int from = queue.poll();
                for (int to = 0; to <= sink; to++) {
                    if (previous[to] < 0 && capacity[from][to] > 0) {
                        previous[to] = from;
                        queue.add(to);
                    }
                }
            }
            if (previous[sink] < 0)
                return flow == supplied;
            int pushed = Integer.MAX_VALUE;
            for (int node = sink; node != source; node = previous[node])
                pushed = Math.min(pushed, capacity[previous[node]][node]);
            for (int node = sink; node != source; node = previous[node]) {
                capacity[previous[node]][node] -= pushed;
                capacity[node][previous[node]] += pushed;
            }
            flow += pushed;
        }
    }
}
