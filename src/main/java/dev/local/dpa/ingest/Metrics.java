package dev.local.dpa.ingest;

import dev.local.dpa.graph.GraphStore;
import dev.local.dpa.queue.EventQueue;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import lombok.Getter;
import org.springframework.stereotype.Component;

@Component
@Getter
public class Metrics {
    private final Counter published;
    private final Counter consumed;
    private final Counter dedupped;
    private final Counter applied;
    private final Counter applyErrors;
    private final Timer queryLatency;

    public Metrics(MeterRegistry reg, EventQueue queue, GraphStore graph) {
        this.published   = Counter.builder("dpa.events.published").register(reg);
        this.consumed    = Counter.builder("dpa.events.consumed").register(reg);
        this.dedupped    = Counter.builder("dpa.events.dedupped").register(reg);
        this.applied     = Counter.builder("dpa.events.applied").register(reg);
        this.applyErrors = Counter.builder("dpa.events.apply_errors").register(reg);
        this.queryLatency = Timer.builder("dpa.query.latency").register(reg);

        reg.gauge("dpa.queue.size", queue, EventQueue::size);
        reg.gauge("dpa.graph.nodes", graph, GraphStore::nodeCount);
        reg.gauge("dpa.graph.edges", graph, GraphStore::edgeCount);
    }
}
