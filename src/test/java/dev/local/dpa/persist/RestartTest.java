package dev.local.dpa.persist;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import dev.local.dpa.algo.Reach;
import dev.local.dpa.config.AppProps;
import dev.local.dpa.event.Observed;
import dev.local.dpa.event.Status;
import dev.local.dpa.graph.GraphStore;
import dev.local.dpa.ingest.EventApplier;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Path;
import java.time.Instant;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class RestartTest {

    @Test
    void restart_preserves_query_results(@TempDir Path tmp) throws IOException {
        AppProps props = new AppProps();
        props.setDataDir(tmp.toString());

        // -- run 1 --
        GraphStore g1 = new GraphStore(props);
        EventApplier a1 = new EventApplier(g1);
        Wal w1 = new Wal(props, mapper()); w1.init();
        Snapshot s1 = new Snapshot(props, mapper(), g1, w1);

        Instant t = Instant.parse("2026-05-09T10:00:00Z");
        for (Observed e : new Observed[]{
                new Observed("e1", t, "checkout", "payments", 10, Status.OK),
                new Observed("e2", t, "payments", "ledger", 8, Status.OK),
                new Observed("e3", t, "checkout", "cart", 3, Status.OK),
        }) { w1.append(e); a1.apply(e); }
        s1.take();
        for (Observed e : new Observed[]{
                new Observed("e4", t.plusSeconds(60), "cart", "inventory", 5, Status.OK),
                new Observed("e5", t.plusSeconds(60), "ledger", "audit", 2, Status.OK),
        }) { w1.append(e); a1.apply(e); }
        Map<String, ?> before = new Reach(g1).downstream("checkout");
        w1.close();

        // -- run 2: fresh store, recover --
        GraphStore g2 = new GraphStore(props);
        EventApplier a2 = new EventApplier(g2);
        Wal w2 = new Wal(props, mapper()); w2.init();
        Snapshot s2 = new Snapshot(props, mapper(), g2, w2);

        s2.loadIfPresent(a2);
        w2.disable();
        try {
            w2.replay(e -> { try { a2.apply(e); } catch (Exception ignored) {} });
        } finally { w2.enable(); }
        Map<String, ?> after = new Reach(g2).downstream("checkout");
        w2.close();

        assertThat(after.keySet()).isEqualTo(before.keySet());
    }

    private static ObjectMapper mapper() {
        return new ObjectMapper().registerModule(new JavaTimeModule());
    }
}
