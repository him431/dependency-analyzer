package dev.local.dpa.graph;

import dev.local.dpa.config.AppProps;
import dev.local.dpa.event.Status;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class GraphStore {

    private final AppProps props;
    private final Map<String, Map<String, Edge>> forward = new ConcurrentHashMap<>();
    private final Map<String, Set<String>> reverse = new ConcurrentHashMap<>();
    private final Map<String, ServiceNode> nodes = new ConcurrentHashMap<>();
    private final Map<EdgeKey, Instant> tombstones = new ConcurrentHashMap<>();

    public GraphStore(AppProps props) { this.props = props; }

    public void observe(String source, String target, Instant ts, long latencyMs, Status status) {
        ensureNode(source);
        ensureNode(target);

        Instant tomb = tombstones.get(new EdgeKey(source, target));
        if (tomb != null && ts.isBefore(tomb)) return;

        // forward + reverse must be a single critical section per (src, tgt)
        // otherwise concurrent remove can leave the maps out of sync
        Map<String, Edge> outs = forward.computeIfAbsent(source, k -> new ConcurrentHashMap<>());
        outs.compute(target, (t, existing) -> {
            Edge e = existing;
            if (e == null) {
                e = new Edge(source, target, props.getRollingBuffer(), ts);
                reverse.computeIfAbsent(target, k -> ConcurrentHashMap.newKeySet()).add(source);
            }
            e.touch(ts);
            e.getStats().add(new Sample(ts, latencyMs, status));
            return e;
        });
        if (tomb != null) tombstones.remove(new EdgeKey(source, target), tomb);
    }

    public void remove(String source, String target, Instant ts) {
        EdgeKey k = new EdgeKey(source, target);
        tombstones.merge(k, ts, (a, b) -> a.isAfter(b) ? a : b);

        Map<String, Edge> outs = forward.get(source);
        if (outs == null) return;
        outs.compute(target, (t, existing) -> {
            if (existing == null) return null;
            if (existing.getLastEventTs().isAfter(ts)) return existing;
            Set<String> srcs = reverse.get(target);
            if (srcs != null) srcs.remove(source);
            return null;
        });
        Map<String, Edge> after = forward.get(source);
        if (after != null && after.isEmpty()) forward.remove(source, after);
    }

    public void updateMetadata(String service, Map<String, String> attrs, Instant ts) {
        ensureNode(service).updateAttributes(attrs, ts);
    }

    public void heartbeat(String service, Instant ts) {
        ensureNode(service).heartbeat(ts);
    }

    public int sweepTombstones(Instant now) {
        long ttl = props.getTombstoneTtlSeconds();
        Instant cutoff = now.minusSeconds(ttl);
        int removed = 0;
        for (Map.Entry<EdgeKey, Instant> e : tombstones.entrySet()) {
            if (e.getValue().isBefore(cutoff) && tombstones.remove(e.getKey(), e.getValue())) removed++;
        }
        return removed;
    }

    private ServiceNode ensureNode(String id) {
        ServiceNode n = nodes.get(id);
        if (n != null) return n;
        ServiceNode fresh = new ServiceNode(id);
        ServiceNode prev = nodes.putIfAbsent(id, fresh);
        return prev != null ? prev : fresh;
    }

    public boolean hasService(String id) { return nodes.containsKey(id); }
    public ServiceNode node(String id) { return nodes.get(id); }
    public Set<String> services() { return Collections.unmodifiableSet(nodes.keySet()); }
    public int nodeCount() { return nodes.size(); }
    public int edgeCount() {
        int total = 0;
        for (Map<String, Edge> m : forward.values()) total += m.size();
        return total;
    }

    public Map<String, Edge> outgoing(String source) {
        Map<String, Edge> m = forward.get(source);
        return m == null ? Collections.<String, Edge>emptyMap() : m;
    }

    public Set<String> incoming(String target) {
        Set<String> s = reverse.get(target);
        return s == null ? Collections.<String>emptySet() : s;
    }

    public Set<Edge> incidentEdges(String service) {
        Set<Edge> out = new HashSet<>();
        out.addAll(outgoing(service).values());
        for (String src : incoming(service)) {
            Edge e = outgoing(src).get(service);
            if (e != null) out.add(e);
        }
        return out;
    }
}
