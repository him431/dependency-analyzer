package dev.local.dpa.graph;

import dev.local.dpa.config.AppProps;
import dev.local.dpa.event.Status;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class GraphStore {

    private final AppProps props;
    private final Map<String, Map<String, Edge>> forward = new ConcurrentHashMap<>();
    private final Map<String, Set<String>> reverse = new ConcurrentHashMap<>();
    private final Map<String, ServiceNode> nodes = new ConcurrentHashMap<>();

    public GraphStore(AppProps props) { this.props = props; }

    public void observe(String source, String target, Instant ts, long latencyMs, Status status) {
        ensureNode(source);
        ensureNode(target);
        Map<String, Edge> outs = forward.computeIfAbsent(source, k -> new ConcurrentHashMap<>());
        Edge e = outs.get(target);
        if (e == null) {
            e = new Edge(source, target, props.getRollingBuffer(), ts);
            outs.put(target, e);
            reverse.computeIfAbsent(target, k -> ConcurrentHashMap.newKeySet()).add(source);
        }
        e.touch(ts);
        e.getStats().add(new Sample(ts, latencyMs, status));
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
}
