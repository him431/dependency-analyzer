package dev.local.dpa.algo;

import dev.local.dpa.graph.Edge;
import dev.local.dpa.graph.GraphStore;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.PriorityQueue;

@Component
public class ShortestPath {

    private final GraphStore graph;

    public ShortestPath(GraphStore graph) { this.graph = graph; }

    public Optional<Result> find(String from, String to) {
        if (!graph.hasService(from) || !graph.hasService(to)) return Optional.empty();
        if (from.equals(to)) return Optional.of(new Result(Collections.singletonList(from), 0.0));

        Map<String, Double> dist = new HashMap<>();
        Map<String, String> prev = new HashMap<>();
        PriorityQueue<Node> pq = new PriorityQueue<>();
        dist.put(from, 0.0);
        pq.add(new Node(from, 0.0));

        while (!pq.isEmpty()) {
            Node cur = pq.poll();
            if (cur.dist > dist.getOrDefault(cur.id, Double.POSITIVE_INFINITY)) continue;
            if (cur.id.equals(to)) break;
            for (Map.Entry<String, Edge> e : graph.outgoing(cur.id).entrySet()) {
                double w = Math.max(1.0, e.getValue().getStats().meanLatency());
                double nd = cur.dist + w;
                if (nd < dist.getOrDefault(e.getKey(), Double.POSITIVE_INFINITY)) {
                    dist.put(e.getKey(), nd);
                    prev.put(e.getKey(), cur.id);
                    pq.add(new Node(e.getKey(), nd));
                }
            }
        }
        if (!dist.containsKey(to)) return Optional.empty();

        List<String> path = new ArrayList<>();
        for (String c = to; c != null; c = prev.get(c)) path.add(c);
        Collections.reverse(path);
        return Optional.of(new Result(path, dist.get(to)));
    }

    @Getter @RequiredArgsConstructor
    public static class Result {
        private final List<String> path;
        private final double totalLatencyMs;
    }

    private static final class Node implements Comparable<Node> {
        final String id; final double dist;
        Node(String id, double dist) { this.id = id; this.dist = dist; }
        public int compareTo(Node o) { return Double.compare(this.dist, o.dist); }
    }
}
