package dev.local.dpa.ingest;

import dev.local.dpa.event.Event;
import dev.local.dpa.persist.Wal;
import dev.local.dpa.queue.EventQueue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

public class Consumer implements Runnable {

    private static final Logger log = LoggerFactory.getLogger(Consumer.class);

    private final EventQueue queue;
    private final Dedup dedup;
    private final Wal wal;
    private final EventApplier applier;
    private final Metrics metrics;
    private final AtomicBoolean running;

    public Consumer(EventQueue queue, Dedup dedup, Wal wal, EventApplier applier,
                    Metrics metrics, AtomicBoolean running) {
        this.queue = queue; this.dedup = dedup; this.wal = wal;
        this.applier = applier; this.metrics = metrics; this.running = running;
    }

    public void run() {
        while (running.get() || queue.size() > 0) {
            try {
                Event e = queue.poll(200, TimeUnit.MILLISECONDS);
                if (e == null) continue;
                metrics.getConsumed().increment();
                if (!dedup.firstSight(e.getEventId())) {
                    metrics.getDedupped().increment();
                    continue;
                }
                wal.append(e);
                try {
                    applier.apply(e);
                    metrics.getApplied().increment();
                } catch (RuntimeException ex) {
                    metrics.getApplyErrors().increment();
                    log.warn("apply failed for {}: {}", e.getEventId(), ex.toString());
                }
            } catch (InterruptedException ie) {
                Thread.currentThread().interrupt();
                return;
            } catch (Exception ex) {
                log.error("consumer error", ex);
            }
        }
    }
}
