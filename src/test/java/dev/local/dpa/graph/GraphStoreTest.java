package dev.local.dpa.graph;

import dev.local.dpa.event.Status;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

class GraphStoreTest {

    private static final Instant T = Instant.parse("2026-05-09T10:00:00Z");

    @Test
    void observe_then_query() {
        GraphStore g = GraphFixtures.newStore();
        g.observe("a", "b", T, 5, Status.OK);
        assertThat(g.outgoing("a")).containsKey("b");
        assertThat(g.incoming("b")).contains("a");
        assertThat(g.edgeCount()).isEqualTo(1);
    }

    @Test
    void duplicate_observes_dont_multiply_edges() {
        GraphStore g = GraphFixtures.newStore();
        for (int i = 0; i < 50; i++) g.observe("a", "b", T, 5, Status.OK);
        assertThat(g.edgeCount()).isEqualTo(1);
    }

    @Test
    void remove_then_old_observe_keeps_edge_absent() {
        GraphStore g = GraphFixtures.newStore();
        g.remove("a", "b", T.plusSeconds(10));
        g.observe("a", "b", T, 5, Status.OK);
        assertThat(g.outgoing("a")).doesNotContainKey("b");
    }

    @Test
    void newer_observe_after_remove_resurrects() {
        GraphStore g = GraphFixtures.newStore();
        g.remove("a", "b", T);
        g.observe("a", "b", T.plusSeconds(60), 5, Status.OK);
        assertThat(g.outgoing("a")).containsKey("b");
    }

    @Test
    void forward_and_reverse_agree() {
        GraphStore g = GraphFixtures.newStore();
        g.observe("a", "b", T, 1, Status.OK);
        g.observe("a", "c", T, 1, Status.OK);
        g.observe("b", "c", T, 1, Status.OK);
        for (String src : g.services()) {
            for (String tgt : g.outgoing(src).keySet()) {
                assertThat(g.incoming(tgt)).contains(src);
            }
        }
    }
}
