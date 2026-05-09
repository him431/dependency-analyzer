package dev.local.dpa.graph;

import dev.local.dpa.event.Status;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

class ConcurrencyTest {

    @Test
    void writers_and_readers_dont_corrupt_state() throws Exception {
        GraphStore g = GraphFixtures.newStore();
        Instant base = Instant.parse("2026-05-09T10:00:00Z");

        int writers = 12;
        int ops = 10_000;
        ExecutorService pool = Executors.newFixedThreadPool(writers + 4);
        List<Future<?>> futures = new ArrayList<>();

        for (int w = 0; w < writers; w++) {
            final int seed = w;
            futures.add(pool.submit(() -> {
                Random r = new Random(seed);
                for (int i = 0; i < ops; i++) {
                    String src = "s" + r.nextInt(40);
                    String tgt = "s" + r.nextInt(40);
                    if (src.equals(tgt)) continue;
                    Instant t = base.plusMillis(i);
                    if (r.nextInt(10) < 8) g.observe(src, tgt, t, 1 + r.nextInt(50), Status.OK);
                    else g.remove(src, tgt, t);
                }
            }));
        }
        for (int i = 0; i < 4; i++) {
            futures.add(pool.submit(() -> {
                for (int j = 0; j < 200; j++) {
                    g.edgeCount();
                    g.outgoing("s1").size();
                }
            }));
        }
        for (Future<?> f : futures) f.get(60, TimeUnit.SECONDS);
        pool.shutdown();

        for (String src : g.services()) {
            for (String tgt : g.outgoing(src).keySet()) {
                assertThat(g.incoming(tgt)).contains(src);
            }
        }
    }
}
