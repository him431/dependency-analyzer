package dev.local.dpa.ingest;

import dev.local.dpa.event.Observed;
import dev.local.dpa.event.Status;
import dev.local.dpa.graph.GraphFixtures;
import dev.local.dpa.graph.GraphStore;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

class IdempotencyTest {

    @Test
    void same_event_id_processed_once() {
        Dedup dedup = new Dedup();
        GraphStore g = GraphFixtures.newStore();
        EventApplier applier = new EventApplier(g);
        Observed e = new Observed("e1", Instant.parse("2026-05-09T10:00:00Z"),
                "checkout", "db", 5, Status.OK);

        int processed = 0;
        for (int i = 0; i < 100; i++) {
            if (dedup.firstSight(e.getEventId())) { applier.apply(e); processed++; }
        }
        assertThat(processed).isEqualTo(1);
        assertThat(g.edgeCount()).isEqualTo(1);
    }
}
