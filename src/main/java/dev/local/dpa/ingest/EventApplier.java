package dev.local.dpa.ingest;

import dev.local.dpa.event.Event;
import dev.local.dpa.event.Heartbeat;
import dev.local.dpa.event.Meta;
import dev.local.dpa.event.Observed;
import dev.local.dpa.event.Removed;
import dev.local.dpa.graph.GraphStore;
import org.springframework.stereotype.Component;

@Component
public class EventApplier {

    private final GraphStore graph;

    public EventApplier(GraphStore graph) { this.graph = graph; }

    public void apply(Event e) {
        if (e instanceof Observed) {
            Observed o = (Observed) e;
            graph.observe(o.getSource(), o.getTarget(), o.getTimestamp(), o.getLatencyMs(), o.getStatus());
        } else if (e instanceof Removed) {
            Removed r = (Removed) e;
            graph.remove(r.getSource(), r.getTarget(), r.getTimestamp());
        } else if (e instanceof Meta) {
            Meta m = (Meta) e;
            graph.updateMetadata(m.getService(), m.getAttributes(), m.getTimestamp());
        } else if (e instanceof Heartbeat) {
            Heartbeat h = (Heartbeat) e;
            graph.heartbeat(h.getService(), h.getTimestamp());
        } else {
            throw new IllegalStateException("unknown event: " + e.getClass());
        }
    }
}
