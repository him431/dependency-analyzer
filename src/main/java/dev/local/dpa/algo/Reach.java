package dev.local.dpa.algo;

import dev.local.dpa.graph.GraphStore;
import org.springframework.stereotype.Component;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Component
public class Reach {

    private final GraphStore graph;

    public Reach(GraphStore graph) { this.graph = graph; }

    public Map<String, List<String>> downstream(String source) { return bfs(source, true); }
    public Map<String, List<String>> upstream(String target) { return bfs(target, false); }

    public int countDownstream(String source) { return bfs(source, true).size(); }
    public int countUpstream(String target) { return bfs(target, false).size(); }

    private Map<String, List<String>> bfs(String start, boolean forward) {
        Map<String, List<String>> result = new LinkedHashMap<>();
        if (!graph.hasService(start)) return result;

        Map<String, String> parent = new HashMap<>();
        Deque<String> q = new ArrayDeque<>();
        q.add(start);
        parent.put(start, null);

        while (!q.isEmpty()) {
            String cur = q.removeFirst();
            Iterable<String> next = forward ? graph.outgoing(cur).keySet() : graph.incoming(cur);
            for (String n : next) {
                if (parent.containsKey(n)) continue;
                parent.put(n, cur);
                q.addLast(n);
                List<String> p = new ArrayList<>();
                for (String c = n; c != null; c = parent.get(c)) p.add(c);
                if (forward) Collections.reverse(p);
                result.put(n, p);
            }
        }
        return result;
    }
}
