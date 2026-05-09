package dev.local.dpa.graph;

import dev.local.dpa.config.AppProps;
import dev.local.dpa.event.Status;

import java.time.Instant;

public final class GraphFixtures {

    private GraphFixtures() {}

    public static GraphStore newStore() {
        return new GraphStore(new AppProps());
    }

    /** A->B->C, B->D. reachable(A)={B,C,D}; dependents(D)={A,B}. */
    public static GraphStore yShape() {
        GraphStore g = newStore();
        Instant t = Instant.parse("2026-05-09T10:00:00Z");
        g.observe("A", "B", t, 10, Status.OK);
        g.observe("B", "C", t, 20, Status.OK);
        g.observe("B", "D", t, 30, Status.OK);
        return g;
    }

    /** A->B->C->A (3-cycle), X->Y->X (2-cycle), Z->Z self-loop. */
    public static GraphStore withCycles() {
        GraphStore g = newStore();
        Instant t = Instant.parse("2026-05-09T10:00:00Z");
        g.observe("A", "B", t, 1, Status.OK);
        g.observe("B", "C", t, 1, Status.OK);
        g.observe("C", "A", t, 1, Status.OK);
        g.observe("X", "Y", t, 1, Status.OK);
        g.observe("Y", "X", t, 1, Status.OK);
        g.observe("Z", "Z", t, 1, Status.OK);
        return g;
    }
}
